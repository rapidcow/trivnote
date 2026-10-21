#!/usr/bin/env perl

use 5.010;
use strict;
use warnings;

use Data::Dumper;
use Fcntl qw(:seek);
use IPC::Open2;

use constant DEBUG => 0;

my %REPLACE;
my %futures;
my %parents;

$/ = "\012";
$Data::Dumper::Useqq = 1;

sub hash
{
	local ($., $!);
	my ($type, @content) = @_;
	my ($rdr, $wtr);
	if ($] < 5.006) {
		require Symbol;
		$rdr = Symbol::gensym();
		$wtr = Symbol::gensym();
	}
	my $pid = open2($rdr, $wtr, qw(git hash-object -t), $type, qw(-w --stdin));
	print { $wtr } @content or warn "hash-object: write pipe: $!\n";
	close $wtr or warn "hash-object: close pipe: $!\n";
	my $hash = <$rdr>;
	waitpid $pid, 0;
	$? == 0 or die "hash-object: died: wstat $? (code @{[$? >> 8]})\n";
	defined $hash or die "BUG: hash-object returned nothing";
	chomp $hash;
	return $hash,
}

foreach my $this (`git rev-list --all`) {
	chomp $this;
	my @parents;
	foreach (`git cat-file commit $this`) {
		next if /^ /;
		if (s/^parent //) {
			chomp;
			push @parents, $_;
		}
		last if /^$/;
	}
	foreach my $past (@parents) {
		push @{$futures{$past}}, $this;
		push @{$parents{$this}}, $past;
	}
}

my %refs = map { chomp; split } split /^/, <<MAP;
fd09c0733b2786619c26144a74322d4539178cd8 refs/heads/libexpr/0.0.0
ef51449a273f03d8e4bd943898975287226afc95 refs/heads/libexpr/0.1.0
119400cbe58cad384fd6e97c6f4c9848c525d040 refs/heads/master
161e32068a70b019e2546d692f9c8c6cbb28ebd2 refs/tags/android/v0.0.0
dc8aa3caad7d0cf315d83484884c99dd67f510b2 refs/tags/libexpr/v0.0.0
MAP
# Reverse topological sort
my @walk = reachable(\%futures, keys %refs);

#
# Non-recursive version of:
#
# L ← Empty list that will contain the sorted nodes
# while exists nodes without a permanent mark do
#     select an unmarked node n
#     visit(n)
#
# function visit(node n)
#     if n has a permanent mark then
#         return
#     if n has a temporary mark then
#         stop   (graph has at least one cycle)
#
#     mark n with a temporary mark
#
#     for each node m with an edge from n to m do
#         visit(m)
#
#     mark n with a permanent mark
#     add n to head of L
#
# /* from https://en.wikipedia.org/wiki/Topological_sorting#Depth-first_search */
sub reachable
{
	my ($graph, @node) = @_;
	my (%count, @list);
	my (@paths, @stop);
	# Stack trace
	@paths = ([]) x @node;
	# Bunch of sentinels
	@stop = (!!0) x @node;
	while (@node) {
		my $node = pop @node;
		my $path = pop @paths;
		my $stop = pop @stop;
		my $count = \$count{$node};

		# @@@ corresponds to bottom half of visit()
		if ($stop) {
			# We've visited all children; STOP and return.
			push @list, $node;
			$$count = 2;
			next;
		}
		# @@@ corresponds to top half of visit()
		$$count ||= 0;
		if ($$count == 2) {
			# Ok?  So $node has been emitted.  No biggie,
			# though it would be unwise to walk this again.
			next;
		}
		if ($$count == 1) {
			# Once of the children from $node led us all
			# the way back to it; there is a cycle in the
			# graph and it cannot be topologically sorted.
			my @path;
			foreach (reverse @$path) {
				push @path, $_;
				last if $_ eq $node;
			}
			die "Cyclic graph eats its own tail at ",
			join ' => ', reverse $node, @path;
		}
		# $count is nil; this is a fresh node
		{
			# Make sure whoever gets here GETS it.  It'd
			# take a while anyways; the children would have
			# all been emitted in the next part of the stack.
			push @node, $node;
			push @paths, $path;
			push @stop, !!1;

			my @path = (@$path, $node);
			foreach (@{$graph->{$node} // []}) {
				push @node, $_;
				push @paths, \@path;
				push @stop, !!0;
			}
		}
	}
	reverse @list;
}

# Maintain nodes in the current graph and the rebased graph
my ($this, $that);
my ($past, $last);

my $i = 1; foreach (@walk) {
	$this = $_;
	$that = $REPLACE{$this} // $this;
	print STDERR "WALK $_ ($i of @{[0+@walk]})\r" if -t STDERR;

	open my $tempfh, '+>', \(my $mem = "") or die "<:memory: error: $!";
	print { $tempfh } `git cat-file commit $that`;

	foreach my $past (@{$parents{$this} // []}) {
		if (($last = $REPLACE{$past} // $past) ne $past) {
			replace_header($tempfh, \$mem, "parent $past", "parent $last");
		}
	}

	if ($this eq 'e1b2492cba80c4456af043c140f2d5bf477c32fb') {
		# s/ the exc/ not$&/ => 19adb23b2beec815165877fa0b3ecabb20cc3f49
		replace_content($tempfh, \$mem, <<'EOM');
pointerInteropFilter is actually not the exception

(... as the only lower-case function referenced
that is explicitly by name)

i did not tell eep!  sorry >n<
EOM
	}

	seek $tempfh, 0, SEEK_SET;
	$that = hash commit => <$tempfh>;
	$REPLACE{$this} = $that;
} continue {
	$i += 1;
}
print STDERR "\n" if -t STDERR;

foreach (sort keys %REPLACE) {
	my ($this, $that) = ($_, $REPLACE{$_});
	print "REPLACE $this WITH $that\n";
}
while (my ($sha, $ref) = each %refs) {
	print $REPLACE{$sha}, " ", $ref, "\n";
}

sub replace_header
{
	local $.;
	my ($tempfh, $memptr, $spec, $line) = @_;
	chomp $line;
	seek $tempfh, 0, SEEK_SET;
	my @buffer;
	# Reset the input line counter.  Note that
	# simply unsetting $. is useless.
	$. = 0;
	while (<$tempfh>) {
		chomp (my $canon_line = $_);
		if (1 .. /^$/) {
			# If there is a space, consider it a full-line match.
			if ($spec =~ / /) {
				next unless $canon_line eq $spec;
			}
			else {
				# Must be a field name (and not any prefix!)
				next unless /\A\Q$spec\E ?/;
			}
			$_ = "$line$/";
		}
	}
	continue {
		push @buffer, $_;
	}
	$$memptr = '';
	seek $tempfh, 0, SEEK_SET;
	print { $tempfh } $_ for @buffer;
}

sub replace_content
{
	local $.;
	my ($tempfh, $memptr, @content) = @_;
	seek $tempfh, 0, SEEK_SET;
	my @buffer;
	$. = 0;
	while (<$tempfh>) {
		last unless 1 .. /^$/;
	}
	continue {
		push @buffer, $_;
	}
	push @buffer, @content;
	seek $tempfh, 0, SEEK_SET;
	$$memptr = '';
	print { $tempfh } $_ for @buffer;
}

sub get_all_content
{
	local $.;
	local $/ = $/;
	my ($tempfh) = @_;
	seek $tempfh, 0, SEEK_SET;
	$. = 0;
	my $content = '';
	while (<$tempfh>) {
		unless (1 .. /^$/) {
			$content .= $_;
			last;
		}
	}
	undef $/;
	my $morecontent = <$tempfh>;
	$content .= $morecontent if defined $morecontent;
	return $content;
}

sub get_committer_date
{
	local $.;
	local $/ = $/;
	my ($tempfh) = @_;
	seek $tempfh, 0, SEEK_SET;
	$. = 0;
	while (<$tempfh>) {
		last unless 1 .. /^$/;
		if (/^committer .+ <.*> ([0-9]+)/) {
			return $1;
		}
	}
	die "no committer date???";
}

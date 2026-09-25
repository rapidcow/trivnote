#!perl
#
# xt/changes.t - change log format check
#
use 5.006;
use strict;
use warnings;
use File::Spec;
use Test::More;

BEGIN {
	eval "use Test::CPAN::Changes";
	plan skip_all => "Test::CPAN::Changes required" if $@;
}

my (@subsys);
BEGIN {
	@subsys = qw( java/libexpr android );
	plan tests => 4 * @subsys;
}

foreach my $sys (@subsys) {
	my $chlog = File::Spec->catfile(split( '/', $sys ), 'Changes');
	changes_file_ok($chlog);
}

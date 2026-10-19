/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

/* This file is GENERATED.  See src/skel/java for original */
[%-# If you are reading this, then it is the original :) #-%]

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class NumberParser
{
	private final int radix;
	private final Exactness exact;
	/*
	 * Radix = 2**0
	 *  Exact = 2**1
	 *   Order = 2**2
	 *    RADIX = 2**3
	 *     EXACT = 2**4
	 * 100.. =   radix only
	 * 010.. =   exact only
	 * 110.. =   exact then radix
	 * 111.. =   radix then exact
	 *
	 * Higher bits preserve capitalization.
	 */
	private static final int HAS_RADIX = 1;
	private static final int HAS_EXACT = 2;
	private static final int RADIX_FIRST = 4;
	private static final int RADIX_UPPER = 8;
	private static final int EXACT_UPPER = 16;
	private final int flags;
	private final String sign;
	private final String mantissa;
	private final String exponent;
[%
# For later...
	SET def_vars = [
		{ type = "int", name = "radix" }
		{ type = "Exactness", name = "exact",
			fancy = "exactness", getter = "getExactness" }
		{ type = "String", name = "sign" }
		{ type = "String", name = "mantissa"  }
		{ type = "String", name = "exponent"  }
	]
-%]

	/*
	 * R7RS stopped allowing # in digits:
	 * https://docs.scheme.org/surveys/five-to-six-to-seven/
	 *
	 * But it is still present in Racket!  Quote:
	 * "A # in an <inexact_{n}> number is the same as 0, but
	 * # can be used to suggest that the digit's actual value
	 * is unknown."
	 * https://docs.racket-lang.org/reference/reader.html?version=9.3#%28part._parse-number%29
	 *
	 * I think it is still useful to keep... :)
	 *
	 * Note that unlike Racket, our exponent is always in
	 * base 10.  This is stupid, but it's what C printf %a
	 * does so...
	 *
	 * It's likely also silly to allow 10^e, given that
	 * 1/5 does not terminate in base 2^n, but we've been
	 * doing that for floating-point numbers; didn't seem
	 * to bother anyone that it was lossy. =_= (Though in
	 * hexadecimal it is physically impossible to use e+N
	 * given how e is indistinguishable from a hex digit.)
	 *
	 * These regular expressions do not handle inf/NaN,
	 * since those are the oddballs without an exponent.
	 */
[%
	SET radix_code = ["b", "o", "d", "x"];
	SET exact_code = ["e", "i"];

	SET map_name = {
		b = "BINARY"
		o = "OCTAL"
		d = "BASE10"
		x = "BASE16"
	};
	SET map_digit = {
		b = "[0-1]"
		o = "[0-7]"
		d = "[0-9]"
		x = "[0-9a-f]"
	};
	SET map_expo = {
		b = "[ep]"
		o = "[ep]"
		d = "[ep]"
		x = "p"
	};
	SET map_base = {
		b = 2
		o = 8
		d = 10
		x = 16
	};
-%]
[% FOREACH code IN radix_code -%]
[%
	SET name = map_name.item(code);
	SET base = map_base.item(code);
	SET digit = map_digit.item(code);
	SET expo = map_expo.item(code);
-%]
	private static final Pattern NUMBER_[% name %]_PATTERN
		= Pattern.compile("""
# (Grammar defs from Racket.)
(?<sign>
# [<sign>]
	[+-]?)
(?<mantissa>
# Names from 
# (<digits [% base %]>)+ "#"* ["."] "#"*
	[% digit %]+ \\#* [.]? \\#*
# [(<digits [% base %]>)+] "." <digits [% base %]>)+ "#"*
|	[% digit %]* [.] [% digit %]+ \\#*
# (<digits [% base %]>)+ "#"* / (<digits [% base %]>)+ "#"*
|	[% digit %]+ \\#* / [% digit %]+ \\#*
)
(?<exponent>
# Exponent power is coded by suffix, and radix is always 10.
	(?:[% expo %][+-]?[0-9]+)?)
"""
		, Pattern.CASE_INSENSITIVE | Pattern.COMMENTS
	);

[% END -%]
	public NumberParser(String value)
	{
		Exactness exact = Exactness.UNSPECIFIED;
		int order = 1;
		int flags = 0;
		int radix = 0;

		int i;
		final int n = value.length();

		for (i = 0; i + 1 < n; i += 2) {
			if (value.charAt(i) != '#') {
				break;
			}
			char c = value.charAt(i + 1);
			switch (c) {
[%
	SET map_exact = {
		e = "EXACT"
		i = "LOSSY"
	};
-%]
[% FOREACH code IN exact_code -%]
[%	SET exact = map_exact.item(code) -%]
			case [% "'" _ code.upper _ "'" %]:
				flags |= EXACT_UPPER;
				/* fall-through */
			case [% "'" _ code _ "'" %]:
				if (exact != Exactness.UNSPECIFIED) {
					throw new IllegalArgumentException(
"Misplaced exactness specification `#[% code %]'"
					);
				}
				exact = Exactness.[% exact %];
				order = order + 1;
				break;
[% END -%]
[% FOREACH code IN ["b", "o", "d", "x"] -%]
[%	SET base = map_base.item(code) -%]
			case [% "'" _ code.upper _ "'" %]:
				flags |= RADIX_UPPER;
				/* fall-through */
			case [% "'" _ code _ "'" %]:
				if (radix != 0) {
					throw new IllegalArgumentException(
"Misplaced radix specification `#[% code %]'"
					);
				}
				radix = [% base %];
				order = order * 2;
				break;
[% END -%]
			default:
				throw new IllegalArgumentException(
"Unknown specification `#" + c + "'"
				);
			}
		}

		this.flags = (
			((radix != 0) ? HAS_RADIX : 0)
			| ((exact != Exactness.UNSPECIFIED) ? HAS_EXACT : 0)
			| ((order == 3) ? RADIX_FIRST : 0)
			| flags
		);
		radix = (radix == 0) ? 10 : radix;
		this.exact = exact;
		this.radix = radix;

		value = value.substring(i);

		if (
			value.equalsIgnoreCase("+inf.0") ||
			value.equalsIgnoreCase("-inf.0") ||
			value.equalsIgnoreCase("+nan.0") ||
			value.equalsIgnoreCase("-nan.0"))
		{
			this.sign = value.substring(0, 1);
			this.mantissa = value.substring(1);
			this.exponent = "";
			return;
		}

		Matcher match;
		switch (radix) {
[% FOREACH code IN ["b", "o", "d", "x"] -%]
[%
	SET base = map_base.item(code);
	SET name = map_name.item(code);
-%]
			case [% base %]:
				match = NUMBER_[% name %]_PATTERN.matcher(value);
				break;
[% END -%]
			default:
				throw new IllegalStateException("unreachable");
		}
		if (!match.matches()) {
			throw new IllegalArgumentException(
				"Illegal base-" + radix + " number"
			);
		}
		sign = match.group("sign");
		mantissa = match.group("mantissa");
		exponent = match.group("exponent");
	}
[% FOREACH vstat IN def_vars -%]

	/**
	 * {@return the [% vstat.fancy || vstat.name %] of the parsed result}
	 */
	final [% vstat.type %]
[%	IF vstat.getter -%]
	[% vstat.getter %]
[%-	ELSE -%]
	get[% vstat.name.ucfirst %]
[%-	END %]()
	{
		return [% vstat.name %];
	}
[% END -%]

	private String
	formatExact()
	{
		switch (exact) {
		case EXACT:
			return (flags & EXACT_UPPER) != 0 ? "#E" : "#e";
		case LOSSY:
			return (flags & EXACT_UPPER) != 0 ? "#I" : "#i";
		default:
			return "";
		}
	}

	private String
	formatRadix()
	{
		switch (radix) {
[% FOREACH code IN radix_code -%]
		case [% map_base.item(code) %]:
			return (flags & RADIX_UPPER) != 0 ? "#[% code.upper %]" : "#[% code %]";
[% END -%]
		default:
			throw new IllegalStateException("unreachable");
		}
	}

	String
	getPrefix()
	{
		int need = 0;
		boolean hasRadix = (( flags & HAS_RADIX ) != 0);
		boolean hasExact = (( flags & HAS_EXACT ) != 0);
		if (hasRadix) {
			need += 2;
		}
		if (hasExact) {
			need += 2;
		}
		StringBuilder sb = new StringBuilder(need);
		if (hasRadix && hasExact) {
			if (( flags & RADIX_FIRST ) != 0) {
				sb.append(formatRadix());
				sb.append(formatExact());
			} else {
				sb.append(formatExact());
				sb.append(formatRadix());
			}
		}
		else if (hasRadix) {
			sb.append(formatRadix());
		}
		else if (hasExact) {
			sb.append(formatExact());
		}
		return sb.toString();
	}
}

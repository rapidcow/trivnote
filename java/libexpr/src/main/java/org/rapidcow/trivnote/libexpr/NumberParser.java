/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

/* This file is GENERATED.  See src/skel/java for original */
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
	private static final Pattern NUMBER_BINARY_PATTERN
		= Pattern.compile("""
(?<sign>
	[+-]?)
(?<mantissa>
# uinteger
	[0-1]+ \\#* [.]? \\#*
|	[0-1]* [.] [0-1]+ \\#*
|	[0-1]+ / [0-1]+
)
(?<exponent>
	(?:[ep][+-]?[0-9]+)?)
"""
		, Pattern.CASE_INSENSITIVE | Pattern.COMMENTS
	);

	private static final Pattern NUMBER_OCTAL_PATTERN
		= Pattern.compile("""
(?<sign>
	[+-]?)
(?<mantissa>
# uinteger
	[0-7]+ \\#* [.]? \\#*
|	[0-7]* [.] [0-7]+ \\#*
|	[0-7]+ / [0-7]+
)
(?<exponent>
	(?:[ep][+-]?[0-9]+)?)
"""
		, Pattern.CASE_INSENSITIVE | Pattern.COMMENTS
	);

	private static final Pattern NUMBER_BASE10_PATTERN
		= Pattern.compile("""
(?<sign>
	[+-]?)
(?<mantissa>
# uinteger
	[0-9]+ \\#* [.]? \\#*
|	[0-9]* [.] [0-9]+ \\#*
|	[0-9]+ / [0-9]+
)
(?<exponent>
	(?:[ep][+-]?[0-9]+)?)
"""
		, Pattern.CASE_INSENSITIVE | Pattern.COMMENTS
	);

	private static final Pattern NUMBER_BASE16_PATTERN
		= Pattern.compile("""
(?<sign>
	[+-]?)
(?<mantissa>
# uinteger
	[0-9a-f]+ \\#* [.]? \\#*
|	[0-9a-f]* [.] [0-9a-f]+ \\#*
|	[0-9a-f]+ / [0-9a-f]+
)
(?<exponent>
	(?:p[+-]?[0-9]+)?)
"""
		, Pattern.CASE_INSENSITIVE | Pattern.COMMENTS
	);

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
			case 'E':
				flags |= EXACT_UPPER;
				/* fall-through */
			case 'e':
				if (exact != Exactness.UNSPECIFIED) {
					throw new IllegalArgumentException(
"Misplaced exactness specification `#e'"
					);
				}
				exact = Exactness.EXACT;
				order = order + 1;
				break;
			case 'I':
				flags |= EXACT_UPPER;
				/* fall-through */
			case 'i':
				if (exact != Exactness.UNSPECIFIED) {
					throw new IllegalArgumentException(
"Misplaced exactness specification `#i'"
					);
				}
				exact = Exactness.LOSSY;
				order = order + 1;
				break;
			case 'B':
				flags |= RADIX_UPPER;
				/* fall-through */
			case 'b':
				if (radix != 0) {
					throw new IllegalArgumentException(
"Misplaced radix specification `#b'"
					);
				}
				radix = 2;
				order = order * 2;
				break;
			case 'O':
				flags |= RADIX_UPPER;
				/* fall-through */
			case 'o':
				if (radix != 0) {
					throw new IllegalArgumentException(
"Misplaced radix specification `#o'"
					);
				}
				radix = 8;
				order = order * 2;
				break;
			case 'D':
				flags |= RADIX_UPPER;
				/* fall-through */
			case 'd':
				if (radix != 0) {
					throw new IllegalArgumentException(
"Misplaced radix specification `#d'"
					);
				}
				radix = 10;
				order = order * 2;
				break;
			case 'X':
				flags |= RADIX_UPPER;
				/* fall-through */
			case 'x':
				if (radix != 0) {
					throw new IllegalArgumentException(
"Misplaced radix specification `#x'"
					);
				}
				radix = 16;
				order = order * 2;
				break;
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
			case 2:
				match = NUMBER_BINARY_PATTERN.matcher(value);
				break;
			case 8:
				match = NUMBER_OCTAL_PATTERN.matcher(value);
				break;
			case 10:
				match = NUMBER_BASE10_PATTERN.matcher(value);
				break;
			case 16:
				match = NUMBER_BASE16_PATTERN.matcher(value);
				break;
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

	/**
	 * {@return the radix of the parsed result}
	 */
	final int
	getRadix()
	{
		return radix;
	}

	/**
	 * {@return the exactness of the parsed result}
	 */
	final Exactness
	getExactness()
	{
		return exact;
	}

	/**
	 * {@return the sign of the parsed result}
	 */
	final String
	getSign()
	{
		return sign;
	}

	/**
	 * {@return the mantissa of the parsed result}
	 */
	final String
	getMantissa()
	{
		return mantissa;
	}

	/**
	 * {@return the exponent of the parsed result}
	 */
	final String
	getExponent()
	{
		return exponent;
	}

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
		case 2:
			return (flags & RADIX_UPPER) != 0 ? "#B" : "#b";
		case 8:
			return (flags & RADIX_UPPER) != 0 ? "#O" : "#o";
		case 10:
			return (flags & RADIX_UPPER) != 0 ? "#D" : "#d";
		case 16:
			return (flags & RADIX_UPPER) != 0 ? "#X" : "#x";
		default:
			throw new IllegalStateException("unreachable");
		}
	}

	String getPrefix()
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

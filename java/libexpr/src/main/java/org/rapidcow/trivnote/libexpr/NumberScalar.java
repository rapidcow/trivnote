package org.rapidcow.trivnote.libexpr;

/**
 * Numeric scalar.  The scalar is stored and compared
 * symbolically, without regards to the numeric value.
 *
 * <p>
 * Use {@link Scalar#ofNumber(String)} to parse and
 * construct a numeric scalar.
 *
 * <p>The syntax is mostly based on Racket’s grammar.
 * There are four orthogonally represented components:
 *
 * <ul>
 *   <li><em>Prefix</em>: This is an optional component
 *     that specifies the radix ({@code #b} = 2, {@code #o}
 *     = 8, {@code #d} = 10, {@code #x} = 16), or the
 *     exactness ({@code #i} = inexact, {@code #e} = exact),
 *     or both.  An unspecified radix is equivalent to 10.
 *     Note that while the radix applies to both the
 *     significand and the exponent in Racket, it only
 *     applies to the significand for us.</li>
 *
 *   <li><em>Sign</em>: Optional; positive ({@code +})
 *     or negative ({@code -}).</li>
 *
 *   <li><em>Significand</em>: Required.  This is also
 *     the only field subject to radix specification in
 *     the prefix under our model.  The significand may
 *     be in fixed point notation (like {@code 5},
 *     {@code 3.14}).  A notable deviation from R7RS is
 *     that digits may terminate in {@code #} to indicate
 *     uncertainty, though it must occur at the end (e.g.,
 *     no {@code 12#.4#}) and at least some numbers must
 *     come before it (e.g., no {@code .##1}).  It may
 *     also be in fraction notation, in which case the
 *     numerator and denominator are unsigned integers
 *     separated by {@code /}.  Because of how the way
 *     this is spelled is awfully similar to “sign”, it
 *     is given as “mantissa” in the method name here.
 *
 *   <li><em>Exponent</em>: Optional powers of 10
 *     exponent of the form {@code e+N} or powers of
 *     2 of the form {@code p+N}; with the exponent
 *     letter included.  The exponent itself is in
 *     radix 10 (decimal), irrespective of the radix
 *     specified by the prefix.
 * </ul>
 *
 * <p>
 * The following identities should hold true:
 *
 * <ul>
 *   <li><code>{@linkplain #value() value()}.equals(
 *   {@linkplain #prefix() prefix()} +
 *   {@linkplain #number() number()}
 *   )</code>;</li>
 *   <li><code>{@linkplain #number() number()}.equals(
 *   {@linkplain #sign() sign()} +
 *   {@linkplain #mantissa() mantissa()} +
 *   {@linkplain #exponent() exponent()}
 *   )</code>;</li>
 * </ul>
 *
 * @since %v
 */
public sealed
abstract class
	NumberScalar
extends
	Scalar
permits
	SimpleScalar
{
	/**
	 * Default constructor.
	 */
	protected NumberScalar()
	{
		/* nothing to do. */
	}

	/**
	 * {@return the value of this atom}  This value is
	 * suitable for serialization.
	 */
	public abstract String
	value();

	/**
	 * {@return the prefixes of this scalar}
	 * This should be returned in the original casing and order.
	 */
	public abstract String
	prefix();

	/**
	 * {@return the exactness of this scalar}
	 */
	public abstract Exactness
	exactness();

	/**
	 * {@return the radix of this scalar}
	 * The radix may be implied to be 10, when one
	 * is not explicitly specified by the prefix.
	 */
	public abstract int
	radix();

	/**
	 * {@return the numeric portion of this scalar}
	 */
	public abstract String
	number();

	/**
	 * {@return the sign of this scalar}
	 */
	public abstract String
	sign();

	/**
	 * {@return the significand of this scalar}
	 */
	public abstract String
	mantissa();

	/**
	 * {@return the exponent of this scalar}  The
	 * exponent letter, such as {@code e} for powers of
	 * 10 and {@code p} for powers of 2, is included in
	 * this string.
	 */
	public abstract String
	exponent();
}

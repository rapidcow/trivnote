package org.rapidcow.trivnote.libexpr;

import java.util.OptionalInt;

/**
 * Numeric scalar.  The scalar is stored and compared
 * symbolically, without regards to the numeric value.
 *
 * <p>
 * Use {@link #of(String)} to parse and
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
 *     this is spelled is awfully similar to <q>sign</q>, it
 *     is given as <q>mantissa</q> in the method name here.
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
 * @since 0.1.0
 */
public non-sealed
abstract class
	NumberScalar
implements
	Datum
{
	/**
	 * Default constructor.
	 */
	protected NumberScalar()
	{
		/* nothing to do. */
	}

	/**
	 * The number zero; exactly {@code NumberScalar.of("0")}.
	 *
	 * @since %v
	 */
	public static final NumberScalar ZERO = NumberScalar.of("0");

	/**
	 * {@return an immutable numeric scalar atom}
	 *
	 * <p>
	 * Usage:
	 * <pre>{@code Datum v = NumberScalar.of("17/29");}</pre>
	 *
	 * @param value  the given value
	 * @throws NullPointerException on null {@code value}
	 * @throws IllegalArgumentException on illegal number format
	 * @since 0.1.0
	 */
	public static NumberScalar
	of(String value)
	{
		return new SimpleNumberScalar(value);
	}

	/**
	 * {@return an immutable, int-backed numeric scalar}
	 * This is equal to {@code NumberScalar.of(Integer.toString(n))}.
	 *
	 * @param n  the given value
	 * @since %v
	 */
	public static NumberScalar
	of(int n)
	{
		return new IntNumberScalar(n);
	}

	/**
	 * {@return the value of this atom}  This value is
	 * suitable for serialization.
	 */
	public String
	value()
	{
		/*
		 * A subclass may choose to compute this more
		 * optimally (or even cache it), but otherwise
		 * it should be identical to:
		 */
		return prefix() + number();
	}

	/**
	 * {@return a string representation of this scalar}
	 */
	@Override
	public String
	toString()
	{
		return "Number[value=" + value() + "]";
	}

	/**
	 * Compares this scalar against {@code o} for equality.
	 * Two scalars are equal if and only if they agree on
	 * {@link #value()}; the concrete class is otherwise
	 * irrelevant.
	 *
	 * @param o  the object to compare against
	 * @return whether {@code o} is an equal scalar
	 */
	@Override
	public final boolean
	equals(Object o)
	{
		if (this == o) {
			return true;
		}
		if (o instanceof NumberScalar atom) {
			return value().equals(atom.value());
		}
		return false;
	}

	/**
	 * {@return hash code for this scalar}
	 * This implementation is consistent with
	 * {@link #equals(Object)}.
	 */
	@Override
	public final int
	hashCode()
	{
		return value().hashCode();
	}

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
	 * {@return the prefixes of this scalar}
	 * This should be returned in the original
	 * casing and order, if they are preserved.
	 */
	public String
	prefix()
	{
		/*
		 * Subclasses which do not care about the
		 * order in which exactness and radix are
		 * specified may leave the default imple-
		 * mentation untouched.
		 */
		StringBuffer sb = new StringBuffer();
		Exactness exact = exactness();
		int base = radix();
		switch (exact) {
		case EXACT:
			sb.append("#e");
			break;
		case LOSSY:
			sb.append("#i");
			break;
		}
		switch (base) {
		case 2:
			sb.append("#b");
			break;
		case 8:
			sb.append("#o");
			break;
		case 10:
			/* nothing; decimal is the default */
			break;
		case 16:
			sb.append("#x");
			break;
		default:
			throw new IllegalStateException("Bad radix: " + base);
		}
		return sb.toString();
	}

	/**
	 * {@return the numeric portion of this scalar}
	 */
	public String
	number()
	{
		/*
		 * Same case here.
		 */
		return sign() + mantissa() + exponent();
	}

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

	/**
	 * {@return whether this scalar is not-a-number}
	 * Both {@code +nan.0} and {@code -nan.0} qualify.
	 *
	 * @since %v
	 */
	public boolean
	isNaN()
	{
		return mantissa().equalsIgnoreCase("nan.0");
	}

	/**
	 * {@return whether this scalar is positive or negative infinity}
	 *
	 * @since %v
	 */
	public boolean
	isInfinite()
	{
		return mantissa().equalsIgnoreCase("inf.0");
	}

	/**
	 * Attempts to compare this scalar numerically against {@code o}.
	 *
	 * <p>
	 * This is deliberately not {@link Comparable}: {@link #equals(Object)}
	 * is symbolic ({@code 1.0} and {@code 1} differ), and an ordering
	 * that disagrees with it would invite misuse.
	 *
	 * <p>
	 * A subclass returns empty for any type it does not know how to
	 * read.  The concrete types of this package, however, must be able
	 * to compare with one another, so for those the result is always
	 * present.  The order matches {@link Double#compare}: negative
	 * infinity, finite numbers, positive infinity, NaN; and NaN
	 * compares equal to NaN.  Use {@link #equalsNumeric} for IEEE-style
	 * equality.
	 *
	 * <p>
	 * Implementations may throw {@link UnsupportedOperationException}
	 * if the scalar has no real ordering (e.g. complex numbers).
	 *
	 * @param o  the other scalar
	 * @return the sign of the comparison, or empty if unknown
	 * @since %v
	 */
	public OptionalInt
	tryCompareNumericTo(NumberScalar o)
	{
		return OptionalInt.empty();
	}

	/**
	 * Compares this scalar numerically against {@code that}.
	 *
	 * @param that  the other scalar
	 * @return negative, zero or positive as this is less than,
	 *    numerically equal to, or greater than {@code that}
	 * @see #tryCompareNumericTo(NumberScalar)
	 * @since %v
	 */
	public int
	compareNumericTo(NumberScalar that)
	{
		return compareNumeric(this, that);
	}

	/**
	 * Compares two scalars numerically.  Each side is asked first;
	 * if neither knows the other's type, both are re-read through
	 * their {@link #value()} as the built-in implementation.
	 *
	 * @param a  the first scalar
	 * @param b  the second scalar
	 * @return negative, zero or positive as {@code a} is less than,
	 *    numerically equal to, or greater than {@code b}
	 * @since %v
	 */
	public static int
	compareNumeric(NumberScalar a, NumberScalar b)
	{
		OptionalInt p;
		p = a.tryCompareNumericTo(b);
		if (p.isPresent()) {
			return p.getAsInt();
		}
		p = b.tryCompareNumericTo(a);
		if (p.isPresent()) {
			return -Integer.signum(p.getAsInt());
		}
		NumberScalar aNum = (a instanceof SimpleNumberScalar)
			? a : NumberScalar.of(a.value());
		NumberScalar bNum = (b instanceof SimpleNumberScalar)
			? b : NumberScalar.of(b.value());
		return aNum.tryCompareNumericTo(bNum).getAsInt();
	}

	/**
	 * {@return whether this and {@code that} are numerically equal}
	 * Like IEEE-754 and unlike {@link #compareNumericTo}, NaN is
	 * not equal to anything, itself included.
	 *
	 * @param that  the other scalar
	 * @since %v
	 */
	public boolean
	equalsNumeric(NumberScalar that)
	{
		return !this.isNaN() && !that.isNaN() && compareNumericTo(that) == 0;
	}

	/**
	 * {@return whether this scalar is numerically zero}
	 * This includes {@code -0.0} and {@code 0/5}.
	 *
	 * @since %v
	 */
	public boolean
	isZero()
	{
		return equalsNumeric(ZERO);
	}

	/**
	 * Reads this scalar as an {@code int}, if it is exactly one.
	 * The number qualifies if its value is a finite integer in the
	 * range of {@code int}, however it is spelled: {@code 3},
	 * {@code #x10}, {@code 6/2}, {@code 1e3} and {@code 4.0} all
	 * qualify.  Exactness prefixes are ignored; {@code #i4} is 4.
	 *
	 * <p>
	 * Unlike {@link #compareNumericTo}, this never throws for a
	 * malformed value such as {@code 1/0} or {@code 1e99999999999}:
	 * those are simply not ints.
	 *
	 * <p>
	 * The default implementation re-reads {@link #value()} as the
	 * built-in implementation.  Subclasses may override.
	 *
	 * @return the value, or empty if not an exact {@code int}
	 * @since 0.2.0
	 */
	public OptionalInt
	tryAsInt()
	{
		return NumberScalar.of(value()).tryAsInt();
	}

	/**
	 * {@return this scalar as an {@code int}}
	 *
	 * @throws ArithmeticException if {@link #tryAsInt()} is empty
	 * @since 0.2.0
	 */
	public int
	asIntExact()
	{
		OptionalInt n = tryAsInt();
		if (n.isEmpty()) {
			throw new ArithmeticException("not an exact int: " + value());
		}
		return n.getAsInt();
	}
}

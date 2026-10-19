package org.rapidcow.trivnote.libexpr;

/**
 * Numeric scalar.
 *
 * <p>
 * 
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
	 * {@return the prefixes of this scalar}
	 * This should be returned in the original casing and order.
	 */
	public abstract String
	prefix();

	public abstract Exactness
	exactness();

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
	 * {@return the exponent of this scalar, with included}
	 */
	public abstract String
	exponent();
}

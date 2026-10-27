/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import java.math.BigInteger;
import java.util.Objects;
import java.util.OptionalInt;

final
class
	SimpleNumberScalar
extends
	NumberScalar
{
	private final NumberParser result;

	public
	SimpleNumberScalar(String value)
	{
		Objects.requireNonNull(value);
		result = new NumberParser(value);
	}

	@Override
	public final String
	value()
	{
		return prefix() + number();
	}

	@Override
	public String
	prefix()
	{
		return result.getPrefix();
	}

	@Override
	public Exactness
	exactness()
	{
		return result.getExactness();
	}

	@Override
	public int
	radix()
	{
		return result.getRadix();
	}

	@Override
	public String
	number()
	{
		return sign() + mantissa() + exponent();
	}

	@Override
	public String
	sign()
	{
		return result.getSign();
	}

	@Override
	public String
	mantissa()
	{
		return result.getMantissa();
	}

	@Override
	public String
	exponent()
	{
		return result.getExponent();
	}

	/*
	 * Largest exponent magnitude we are willing to expand.
	 */
	private static final int MAX_EXPONENT = 100_000;

	/**
	 * Exact value of a number.  {@code kind} orders the
	 * non-finite values around the finite ones the same way
	 * {@link Double#compare(double, double)} does: -inf < finite
	 * < +inf < NaN.  {@code num}/{@code den} are only meaningful
	 * when finite, and {@code den} is always positive.
	 */
	private record Value(int kind, BigInteger num, BigInteger den)
	{
		static final int NEG_INF = -1, FINITE = 0, POS_INF = 1, NAN = 2;
	}

	@Override
	public OptionalInt
	tryCompareNumericTo(NumberScalar o)
	{
		Value a = valueOf(this);
		Value b = valueOf(o);
		if (b == null) {
			return OptionalInt.empty();
		}
		if (a.kind() != b.kind()) {
			return OptionalInt.of(Integer.compare(a.kind(), b.kind()));
		}
		if (a.kind() != Value.FINITE) {
			return OptionalInt.of(0);
		}
		return OptionalInt.of(
			a.num().multiply(b.den()).compareTo(b.num().multiply(a.den()))
		);
	}

	/**
	 * {@return the exact value, or {@code null} if we do not
	 * know how to read {@code n}}
	 */
	private static Value
	valueOf(NumberScalar n)
	{
		if (n instanceof IntNumberScalar i) {
			return new Value(Value.FINITE,
				BigInteger.valueOf(i.intValue()), BigInteger.ONE);
		}
		if (n instanceof SimpleNumberScalar s) {
			return s.parseValue();
		}
		return null;
	}

	/**
	 * Evaluates the literal.  Inexactness is ignored, so
	 * {@code #i0.1} is the rational 1/10, not the nearest double.
	 *
	 * @throws ArithmeticException on a zero denominator
	 *    or an exponent whose magnitude exceeds {@link #MAX_EXPONENT}
	 */
	private Value
	parseValue()
	{
		String m = mantissa();
		if (m.equalsIgnoreCase("nan.0")) {
			return new Value(Value.NAN, null, null);
		}
		if (m.equalsIgnoreCase("inf.0")) {
			return new Value(sign().equals("-") ? Value.NEG_INF : Value.POS_INF,
				null, null);
		}

		final int radix = radix();
		/* A trailing # stands for an unknown digit, i.e. 0. */
		m = m.replace('#', '0');
		BigInteger num, den;
		int slash = m.indexOf('/');
		int dot = m.indexOf('.');
		if (slash >= 0) {
			num = new BigInteger(m.substring(0, slash), radix);
			den = new BigInteger(m.substring(slash + 1), radix);
			if (den.signum() == 0) {
				throw new ArithmeticException("zero denominator: " + value());
			}
		} else if (dot >= 0) {
			String digits = m.substring(0, dot) + m.substring(dot + 1);
			num = digits.isEmpty() ? BigInteger.ZERO : new BigInteger(digits, radix);
			den = BigInteger.valueOf(radix).pow(m.length() - dot - 1);
		} else {
			num = new BigInteger(m, radix);
			den = BigInteger.ONE;
		}

		String e = exponent();
		if (!e.isEmpty() && num.signum() != 0) {
			int power;
			try {
				power = Integer.parseInt(e.substring(1));
			} catch (NumberFormatException x) {
				throw new ArithmeticException("exponent too large: " + value());
			}
			if (Math.abs(power) > MAX_EXPONENT) {
				throw new ArithmeticException("exponent too large: " + value());
			}
			BigInteger base = BigInteger.valueOf(
				(e.charAt(0) == 'e' || e.charAt(0) == 'E') ? 10 : 2);
			BigInteger scale = base.pow(Math.abs(power));
			if (power >= 0) {
				num = num.multiply(scale);
			} else {
				den = den.multiply(scale);
			}
		}
		if (sign().equals("-")) {
			num = num.negate();
		}
		return new Value(Value.FINITE, num, den);
	}
}

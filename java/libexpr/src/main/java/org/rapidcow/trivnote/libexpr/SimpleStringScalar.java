
/**
 * A simple string scalar is only backed by a native Java string.
 */
final
class
	SimpleStringScalar
extends
	StringScalar
{
	private final String value;

	/**
	 * Constructs a new string scalar.
	 *
	 * @param value  the given value
	 */
	public
	SimpleStringScalar(String value)
	{
		this.value = Objects.requireNonNull(value);
	}

	/**
	 * Returns the value of this atom.
	 * The value is always non-null.
	 *
	 * @return {@inheritDoc}
	 */
	@Override
	public final String
	value()
	{
		return value;
	}

	@Override
	public final boolean
	equals()
	{
	}

	@Override
	public final
}

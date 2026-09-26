package org.rapidcow.trivnote.libexpr;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class WalkTest
{
	@Test void testNullOrigin() {
		assertThrows(NullPointerException.class, () -> new Walk(null));
	}
}

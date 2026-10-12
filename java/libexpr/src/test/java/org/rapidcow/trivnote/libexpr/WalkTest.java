/*
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package org.rapidcow.trivnote.libexpr;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WalkTest
{
	@Test void testNullOrigin() {
		assertThrows(NullPointerException.class, () -> new Walk(null));
	}
}

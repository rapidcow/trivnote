package org.rapidcow.ai.trivnote.android.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExprParserTest {
    @Test
    fun countsTopLevelDatumsAndDumpsThem() {
        val result = parseExpression("(hello world)\n42")
        assertFalse(result.failed)
        assertEquals(2, result.datumCount)
        assertEquals("(hello world)\n42\n", result.output)
    }

    @Test
    fun emptyInputSucceedsWithZeroDatums() {
        val result = parseExpression("")
        assertFalse(result.failed)
        assertEquals(0, result.datumCount)
        assertEquals("", result.output)
    }

    @Test
    fun trailingParseErrorReplacesPartialOutputWithTrace() {
        val result = parseExpression("42\n(")
        assertTrue(result.failed)
        assertEquals(0, result.datumCount)
        assertTrue(result.output.contains("Exception"))
        assertFalse(result.output.startsWith("42\n"))
    }
}

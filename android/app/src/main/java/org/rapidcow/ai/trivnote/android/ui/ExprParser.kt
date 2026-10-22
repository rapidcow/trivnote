package org.rapidcow.ai.trivnote.android.ui

import java.io.IOException
import java.io.StringReader
import java.io.StringWriter
import org.rapidcow.trivnote.libexpr.DataDumper
import org.rapidcow.trivnote.libexpr.DataLoader

internal data class ExprResult(val output: String, val datumCount: Int, val failed: Boolean)

internal fun parseExpression(source: String): ExprResult {
    return try {
        val output = StringWriter()
        val dumper = DataDumper(output)
        val loader = DataLoader(StringReader(source))
        var count = 0
        for (datum in loader) {
            dumper.writeDatum(datum)
            output.write("\n")
            count++
        }
        // The iterator records IO/parse errors instead of throwing them.
        loader.lastIOException()?.let { throw it }
        ExprResult(output.toString(), count, false)
    } catch (error: IOException) {
        ExprResult(error.stackTraceToString(), 0, true)
    } catch (error: RuntimeException) {
        ExprResult(error.stackTraceToString(), 0, true)
    }
}

/* vim: set et sts=4 sw=4: ==========================
 *
 * Copyright 2026 rapidcow!
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 * ==================================================
 *
 * ui.PencilProbe - S Pen sensor test
 *
 *   - point shows the (x,y) coordinates;
 *   - azimuth shows the rotation about the z-axis;
 *   - altitude shows the rotation above the x-y plane;
 *   - force shows the pressure exerted on the pen tip.
 *
 * Angles measured in radians, force measured in [0,1].
 ================================================= */
package org.rapidcow.android.trivnote.ui

import kotlin.math.PI

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp   /* Device-independent pixel */
import androidx.compose.ui.unit.sp   /* Scalable pixel */

/*
 * operator-functions (these are implicit "traits"
 * (most certainly not the right word) required to
 * how to read to/write from a "mutable state".)
 *
 * Because of how it may not be obvious how they are
 * referenced (and contrarily, when they cease to be
 * referenced), they are imported/aliased separately.
 */
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import org.rapidcow.android.trivnote.ui.theme.Blue

/*
 * This is the sensor information struct designed by
 * Claude for the other eponymous Swift test program.
 * I don't know what they are supposed to stand for.
 *
 * data class is just Java record btw.  Also -- `val'
 * (one letter of difference from `var'!) is immutable.
 */
private data class PenReading(
    val location      : Offset,
    val isPenDown     : Boolean,
    val azimuthAngle  : Float,   // radians
    val altitudeAngle : Float,   // radians
    val forceFraction : Float    // 0..1
)

/*
 * PencilProbe is a function (not class!  Despite the casing)
 * because Jetpack Compose's convention is to name components
 * in PascalCase.  Canvas is in fact also a @Composable guy,
 * meaning it too is a function.
 *
 * The return type of PencilProbe is Unit, the default.
 * Unit is Kotlin for void.  (like how Rust calls it, eh.)
 *
 * In the parameter list: modifier is a guy of type Modifier,
 * that defaults to a singleton object *also* named by the
 * class name.  (The syntax is doing both at once, yes.)
 * This singleton is known as a *companion object*.
 */
@Composable
fun PencilProbe(modifier: Modifier = Modifier)
{
    var reading by remember { mutableStateOf<PenReading?>(null) }
    val textMeasurer = rememberTextMeasurer()
    val textColor = MaterialTheme.colorScheme.onBackground

    Canvas(
        modifier = modifier
        .fillMaxSize()
        /* https://gist.github.com/Debdutta-Panda/2db64ebac52c8c8863144e0489b0fc4c */
        .pointerInteropFilter { motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_UP -> reading = liftPen(reading, motionEvent)
                MotionEvent.ACTION_DOWN -> reading = readPen(reading, motionEvent)
                MotionEvent.ACTION_MOVE -> reading = readPen(reading, motionEvent)
            }
            true
        }
    ) {
        /* ?. similarly here.  This is the Kotlin way of doing
         * `if (x != null) { do stuff with x }'...? */
        reading?.let { currentReading ->
            if (currentReading.isPenDown) {
                drawCircle(
                    center = currentReading.location,
                    color = Blue.copy(alpha = 0.6f),
                    radius = 24f,
                )
            }
            drawCircle(
                center = currentReading.location,
                color = Blue,
                radius = 24f,
                style = Stroke(width = 4f),
            )
        }

        /* Partially provided by Claude :) */
        drawText(
            textMeasurer = textMeasurer,
            text = diagnosticsText(reading),
            topLeft = Offset(8.dp.toPx(), 8.dp.toPx()),
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                color = textColor,
            ),
        )
    }
}

private fun liftPen(reading: PenReading?, event: MotionEvent): PenReading?
{
    /* The ?. operator just evaluates to null when `reading' itself is null. */
    return reading?.copy(isPenDown = false)
}

private fun readPen(reading: PenReading?, event: MotionEvent): PenReading
{
    /*
       There seems to be the option to pass pointerIndex to some of these?
       I hope it is fine that I leave some of those out...

       It also seems possible to turn .getX() -> .x but i am not doing that
     */
    return PenReading(
        location = Offset(event.getX(), event.getY()),
        isPenDown = true,
        azimuthAngle = event.getOrientation(),
        altitudeAngle = event.getAxisValue(MotionEvent.AXIS_TILT),
        forceFraction = event.getPressure()
    )
}

private fun diagnosticsText(reading: PenReading?): String
{
    if (reading == null) { return """
point     -
azimuth   -
altitude  -
force     -
"""
    }

    val x = reading.location.x
    val y = reading.location.y
    val azimuthAngle = reading.azimuthAngle
    val altitudeAngle = reading.altitudeAngle
    val forceFraction = reading.forceFraction

    return """
point     (${formatFloat(x)}, ${formatFloat(y)})
azimuth   ${formatFloat(azimuthAngle)} rad
altitude  ${formatFloat(altitudeAngle)} rad
force     ${formatFloat(forceFraction)}%
"""
}

private fun formatFloat(x: Float): String
{
    return "%+.4f".format(x)
}

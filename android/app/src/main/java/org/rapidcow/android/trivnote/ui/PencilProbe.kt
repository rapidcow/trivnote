/* vim: set et sts=4 sw=4: ==========================
 *
 * PencilProbe.kt - S Pen sensor test for my Samsung
 * tablet while I beg Apple to not cancel my Mac Mini
 * because payment did not through and Apple hasn't
 * tried charging me since for some reason eughhhhh
 *
 ================================================= */

package org.rapidcow.android.trivnote.ui

import kotlin.math.PI

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

/*
 * operator-functions (these are implicit "traits"
 * (most certainly not the right word) required to
 * how to read to/write from a "mutable state".)
 *
 * Because of how it may not be obvious how they are
 * referenced (and contrarily, when they cease to be
 * referenced), they are imported/aliased separately.
 *
 * Thankfully these wannabes all start with lowercase,
 * so it's easier to tell.
 */
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInteropFilter

import org.rapidcow.android.trivnote.ui.theme.BlueLight

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

    /* Technically this is a function call, but all of
     * Jetpack Compose is essentially DSL to me, so I am
     * just writing all of these like one. */
    Canvas ( modifier = modifier
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
                    color = BlueLight.copy(alpha = 0.6f),
                    radius = 12f,
                )
            }
            drawCircle(
                center = currentReading.location,
                color = BlueLight,
                radius = 12f,
                style = Stroke(width = 2f),
            )
        }
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

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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset

/*
 * operator-functions
 */
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

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

    Canvas(modifier = modifier.fillMaxSize()) {
        /*
         * We're supposed to do something in this lambda,
         * but this has hurt my brain enough that
         * I am going to stop here.
         */
    }
}

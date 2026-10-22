package org.rapidcow.android.trivnote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.rapidcow.ai.trivnote.android.ui.TrivnoteApp
import org.rapidcow.android.trivnote.ui.theme.TrivnoteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TrivnoteTheme {
                TrivnoteApp()
            }
        }
    }
}

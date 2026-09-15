package org.rapidcow.android.trivnote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.rapidcow.android.trivnote.ui.PencilProbe
import org.rapidcow.android.trivnote.ui.theme.TrivnoteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TrivnoteTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PencilProbe(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

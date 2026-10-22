package org.rapidcow.ai.trivnote.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.rapidcow.android.trivnote.R

@Composable
fun ExprPane(modifier: Modifier = Modifier) {
    var source by rememberSaveable { mutableStateOf("") }
    var output by rememberSaveable { mutableStateOf("") }
    var parsed by rememberSaveable { mutableStateOf(false) }
    var failed by rememberSaveable { mutableStateOf(false) }
    var datumCount by rememberSaveable { mutableStateOf(0) }
    var split by rememberSaveable { mutableStateOf(0.5f) }
    var parsing by remember { mutableStateOf(false) }
    var panelHeight by remember { mutableStateOf(1) }
    val scope = rememberCoroutineScope()
    val dividerLabel = stringResource(R.string.expr_resize)
    val editorStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)

    Column(modifier.fillMaxSize().imePadding().padding(12.dp)) {
        Column(Modifier.weight(1f).onSizeChanged { panelHeight = it.height }) {
            OutlinedTextField(
                value = source,
                onValueChange = {
                    source = it
                    parsed = false
                },
                readOnly = parsing,
                label = { Text(stringResource(R.string.expr_source)) },
                textStyle = editorStyle,
                modifier = Modifier.fillMaxWidth().weight(split),
            )
            Box(
                Modifier.fillMaxWidth().height(32.dp)
                    .semantics {
                        contentDescription = dividerLabel
                        progressBarRangeInfo = ProgressBarRangeInfo(split, 0.2f..0.8f)
                        setProgress { value ->
                            split = value.coerceIn(0.2f, 0.8f)
                            true
                        }
                    }
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            split = (split + delta / panelHeight.coerceAtLeast(1)).coerceIn(0.2f, 0.8f)
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.width(48.dp).height(4.dp).background(MaterialTheme.colorScheme.outlineVariant))
            }
            OutlinedTextField(
                value = output,
                onValueChange = {},
                readOnly = true,
                isError = failed,
                label = { Text(stringResource(R.string.expr_output)) },
                textStyle = editorStyle,
                modifier = Modifier.fillMaxWidth().weight(1f - split),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = when {
                    parsing -> stringResource(R.string.expr_parsing)
                    !parsed -> ""
                    failed -> ":("
                    else -> stringResource(R.string.expr_ok, datumCount)
                },
                color = if (parsed && failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
            Button(
                enabled = !parsed && !parsing,
                onClick = {
                    val input = source
                    parsing = true
                    scope.launch {
                        try {
                            val result = withContext(Dispatchers.Default) { parseExpression(input) }
                            output = result.output
                            failed = result.failed
                            datumCount = result.datumCount
                            parsed = true
                        } finally {
                            parsing = false
                        }
                    }
                },
            ) {
                Text(stringResource(R.string.expr_parse))
            }
        }
    }
}

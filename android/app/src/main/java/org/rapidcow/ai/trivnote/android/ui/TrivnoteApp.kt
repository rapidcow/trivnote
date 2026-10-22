package org.rapidcow.ai.trivnote.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.rapidcow.android.trivnote.R
import org.rapidcow.android.trivnote.ui.PencilProbe

private enum class Scene { Main, PencilProbe }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrivnoteApp() {
    var selectedScene by rememberSaveable { mutableStateOf(Scene.Main) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val openMenuLabel = stringResource(R.string.open_menu)

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        // Keep drawing gestures from opening the drawer while using the probe.
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet {
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.app_name)) },
                    selected = selectedScene == Scene.Main,
                    onClick = {
                        selectedScene = Scene.Main
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
                Text(
                    stringResource(R.string.debug_tools),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                        .semantics { heading() },
                    style = MaterialTheme.typography.titleSmall,
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.pencil_probe)) },
                    selected = selectedScene == Scene.PencilProbe,
                    onClick = {
                        selectedScene = Scene.PencilProbe
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
        },
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(stringResource(when (selectedScene) {
                            Scene.Main -> R.string.app_name
                            Scene.PencilProbe -> R.string.pencil_probe
                        }))
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.semantics { contentDescription = openMenuLabel },
                        ) {
                            Text("☰", style = MaterialTheme.typography.headlineSmall)
                        }
                    },
                )
            },
        ) { innerPadding ->
            when (selectedScene) {
                Scene.PencilProbe -> PencilProbe(modifier = Modifier.padding(innerPadding))
                Scene.Main -> Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(R.string.main_scene_hint))
                }
            }
        }
    }
}

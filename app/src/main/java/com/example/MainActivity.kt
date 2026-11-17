package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.GestureViewModel
import com.example.ui.components.ActionPickerSheet
import com.example.ui.screens.GestureLabScreen
import com.example.ui.screens.GesturesHubScreen
import com.example.ui.screens.LauncherSettingsScreen
import com.example.ui.theme.GesturesTheme

const val ROUTE_HUB = "gestures_hub"
const val ROUTE_LAB = "gesture_lab"
const val ROUTE_SETTINGS = "launcher_settings"

private data class NavDestinationItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                GesturesAppRoot()
            }
        }
    }
}

@Composable
fun GesturesAppRoot(
    viewModel: GestureViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshPermissions()
    }

    LaunchedEffect(state.feedbackMessage) {
        val msg = state.feedbackMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    val destinations = remember {
        listOf(
            NavDestinationItem(
                route = ROUTE_HUB,
                label = "الإيماءات",
                selectedIcon = Icons.Rounded.TouchApp,
                unselectedIcon = Icons.Outlined.TouchApp,
                testTag = "nav_hub"
            ),
            NavDestinationItem(
                route = ROUTE_LAB,
                label = "ساحة الاختبار",
                selectedIcon = Icons.Rounded.Gesture,
                unselectedIcon = Icons.Outlined.Gesture,
                testTag = "nav_lab"
            ),
            NavDestinationItem(
                route = ROUTE_SETTINGS,
                label = "اللانشر والحواف",
                selectedIcon = Icons.Rounded.Settings,
                unselectedIcon = Icons.Outlined.Settings,
                testTag = "nav_settings"
            )
        )
    }

    GesturesTheme(darkTheme = state.settings.darkTheme) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        fun navigateTo(route: String) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 700.dp

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (!useRail) {
                        NavigationBar(
                            windowInsets = WindowInsets.navigationBars,
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            destinations.forEach { item ->
                                val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTo(item.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label
                                        )
                                    },
                                    label = { Text(item.label) },
                                    modifier = Modifier.testTag(item.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            destinations.forEach { item ->
                                val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTo(item.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label
                                        )
                                    },
                                    label = { Text(item.label) },
                                    modifier = Modifier.testTag(item.testTag)
                                )
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_HUB,
                        modifier = Modifier.weight(1f)
                    ) {
                        composable(ROUTE_HUB) {
                            GesturesHubScreen(
                                state = state,
                                onSelectCategory = viewModel::selectCategory,
                                onSearchChange = viewModel::updateSearchQuery,
                                onToggleGesture = viewModel::toggleGesture,
                                onEditGesture = viewModel::openEditor,
                                onTestGesture = viewModel::testGestureNow,
                                onEnableAll = { viewModel.setAllGesturesEnabled(true) },
                                onOpenOverlayPad = viewModel::openOverlayPadFromApp,
                                onOpenLauncherSetup = { navigateTo(ROUTE_SETTINGS) }
                            )
                        }
                        composable(ROUTE_LAB) {
                            GestureLabScreen(
                                state = state,
                                onToggleExecuteOnTest = viewModel::setExecuteOnLabTest,
                                onGestureDetected = viewModel::onLabGestureDetected
                            )
                        }
                        composable(ROUTE_SETTINGS) {
                            LauncherSettingsScreen(
                                state = state,
                                onSetMasterEnabled = viewModel::setMasterEnabled,
                                onSetFloatingOrb = viewModel::setFloatingOrbEnabled,
                                onSetEdgeZones = viewModel::setEdgeZonesEnabled,
                                onSetEdgeWidth = viewModel::setEdgeWidthDp,
                                onSetOrbOpacity = viewModel::setOrbOpacity,
                                onSetHaptic = viewModel::setHapticFeedback,
                                onSetMultiTapWindow = viewModel::setMultiTapWindowMs,
                                onSetDarkTheme = viewModel::setDarkTheme,
                                onResetDefaults = viewModel::resetDefaults
                            )
                        }
                    }
                }
            }
        }

        state.editingGesture?.let { editing ->
            ActionPickerSheet(
                gesture = editing,
                installedApps = state.installedApps,
                isLoadingApps = state.isLoadingApps,
                onDismiss = viewModel::closeEditor,
                onSelectApp = { name, pkg, url ->
                    viewModel.assignApp(editing.id, name, pkg, url)
                },
                onSelectShortcut = { actionType ->
                    viewModel.assignShortcut(editing.id, actionType)
                },
                onSelectCustomUrl = { label, url ->
                    viewModel.assignCustomUrl(editing.id, label, url)
                }
            )
        }
    }
}

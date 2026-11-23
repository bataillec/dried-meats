package me.bataillec.meatmelater

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import me.bataillec.meatmelater.domain.AppSettings
import me.bataillec.meatmelater.platform.BatteryOnboardingDialog
import me.bataillec.meatmelater.ui.pieces.NewPieceScreen
import me.bataillec.meatmelater.ui.pieces.PieceDetailScreen
import me.bataillec.meatmelater.ui.pieces.PiecesScreen
import me.bataillec.meatmelater.ui.recipes.RecipeEditScreen
import me.bataillec.meatmelater.ui.recipes.RecipesScreen
import me.bataillec.meatmelater.ui.settings.SettingsScreen
import me.bataillec.meatmelater.ui.stats.StatsScreen
import me.bataillec.meatmelater.ui.theme.AppTheme
import me.bataillec.meatmelater.ui.theme.usesDarkBars

class MainActivity : ComponentActivity() {
    private var pendingPieceId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingPieceId = pieceIdOf(intent)
        val container = (application as MeatMeLaterApp).container
        setContent {
            val settings by container.settings.observe().collectAsState(initial = null)
            val mode = (settings ?: AppSettings()).theme
            val accent = (settings ?: AppSettings()).accent
            val darkBars = usesDarkBars(mode, isSystemInDarkTheme())
            // Bar icon colours must follow the app theme, not the phone's: Clair on a dark phone needs dark icons.
            DisposableEffect(darkBars) {
                val bars =
                    if (darkBars) {
                        SystemBarStyle.dark(
                            Color.TRANSPARENT,
                        )
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    }
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
                onDispose {}
            }
            AppTheme(mode, accent) {
                Surface {
                    AppRoot(pendingPieceId) { pendingPieceId = null }
                    BatteryOnboardingDialog(
                        shouldShow = settings?.batteryHintShown == false,
                        onFinished = {
                            lifecycleScope.launch {
                                container.settings.update {
                                    it.copy(
                                        batteryHintShown = true,
                                    )
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pieceIdOf(intent)?.let { pendingPieceId = it }
    }

    private fun pieceIdOf(intent: Intent?): Long? = intent?.getLongExtra(EXTRA_PIECE_ID, -1L)?.takeIf { it >= 0 }

    companion object {
        const val EXTRA_PIECE_ID = "piece_id"
    }
}

private const val NAV_MAX_FONT_SCALE = 1.2f

private object Routes {
    const val PIECES = "pieces"
    const val NEW_PIECE = "pieces/new"
    const val PIECE = "pieces/{id}"
    const val RECIPES = "recipes"
    const val RECIPE_EDIT = "recipes/edit/{id}"
    const val STATS = "stats"
    const val SETTINGS = "settings"

    fun piece(id: Long) = "pieces/$id"

    fun recipeEdit(id: Long) = "recipes/edit/$id"
}

private data class TopLevel(
    val route: String,
    val label: Int,
    val icon: ImageVector,
)

private val TOP_LEVEL =
    listOf(
        TopLevel(Routes.PIECES, R.string.nav_pieces, Icons.Filled.Home),
        TopLevel(Routes.RECIPES, R.string.nav_recipes, Icons.AutoMirrored.Filled.MenuBook),
        TopLevel(Routes.STATS, R.string.nav_stats, Icons.Filled.Insights),
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppRoot(
    pendingPieceId: Long?,
    onPendingConsumed: () -> Unit,
) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route.orEmpty()
    val topRoute = TOP_LEVEL.firstOrNull { it.route == route }

    LaunchedEffect(pendingPieceId) {
        if (pendingPieceId != null) {
            nav.navigate(Routes.piece(pendingPieceId)) { launchSingleTop = true }
            onPendingConsumed()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(titleFor(route))) },
                navigationIcon = {
                    if (topRoute == null) {
                        IconButton(onClick = { nav.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    }
                },
                actions = {
                    if (route != Routes.SETTINGS) {
                        IconButton(onClick = { nav.navigate(Routes.SETTINGS) { launchSingleTop = true } }) {
                            Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (topRoute != null) {
                // Four labelled tabs cannot fit at 200 % text scale: cap the bar's scale so labels never split mid-word.
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(density.density, minOf(density.fontScale, NAV_MAX_FONT_SCALE)),
                ) {
                    NavigationBar {
                        TOP_LEVEL.forEach { item ->
                            NavigationBarItem(
                                selected = topRoute.route == item.route,
                                onClick = { navigateTopLevel(nav, item.route) },
                                icon = { Icon(item.icon, contentDescription = null) },
                                label = { Text(stringResource(item.label)) },
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Routes.PIECES, modifier = Modifier.padding(padding)) {
            composable(Routes.PIECES) {
                PiecesScreen(onOpen = { nav.navigate(Routes.piece(it)) }, onNew = { nav.navigate(Routes.NEW_PIECE) })
            }
            composable(Routes.NEW_PIECE) {
                NewPieceScreen(onCreated = { id ->
                    nav.navigate(Routes.piece(id)) { popUpTo(Routes.PIECES) }
                })
            }
            composable(Routes.PIECE, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                PieceDetailScreen(
                    pieceId = entry.arguments!!.getLong("id"),
                    onGone = { nav.popBackStack(Routes.PIECES, inclusive = false) },
                )
            }
            composable(Routes.RECIPES) { RecipesScreen(onEdit = { nav.navigate(Routes.recipeEdit(it)) }) }
            composable(Routes.RECIPE_EDIT, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                RecipeEditScreen(entry.arguments!!.getLong("id"), onDone = { nav.popBackStack() })
            }
            composable(Routes.STATS) { StatsScreen(onOpenPiece = { nav.navigate(Routes.piece(it)) }) }
            composable(Routes.SETTINGS) { SettingsScreen() }
        }
    }
}

private fun navigateTopLevel(
    nav: NavHostController,
    route: String,
) {
    nav.navigate(route) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun titleFor(route: String): Int =
    when {
        route.startsWith("pieces/new") -> R.string.new_piece
        route.startsWith("pieces/") -> R.string.piece_detail
        route.startsWith("recipes/edit") -> R.string.recipe_edit
        route.startsWith("recipes") -> R.string.nav_recipes
        route.startsWith("stats") -> R.string.nav_stats
        route.startsWith("settings") -> R.string.settings
        else -> R.string.app_name
    }

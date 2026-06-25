@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.animation.ExperimentalAnimationApi::class)

package com.sw.launcher

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.Collator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class LauncherApp(
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: Drawable,
    val category: String
) { val key: String get() = "$packageName/$activityName" }

enum class Screen { Onboarding, Home, Drawer, Settings }
enum class DrawerFilter(val title: String) { All("Todos"), Recent("Recentes"), Used("Usados"), Games("Jogos"), Social("Social"), Media("Mídia"), Tools("Ferramentas") }

class MainActivity : ComponentActivity() {
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("sw_launcher_material3", Context.MODE_PRIVATE)

        if (Build.VERSION.SDK_INT >= 21) {
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
        }

        setContent {
            var apps by remember { mutableStateOf(loadLauncherApps()) }
            var screen by rememberSaveable { mutableStateOf(if (prefs.getBoolean("onboarding_done", false)) Screen.Home else Screen.Onboarding) }
            var themeName by rememberSaveable { mutableStateOf(prefs.getString("theme", "aurora") ?: "aurora") }
            var dynamicColor by rememberSaveable { mutableStateOf(prefs.getBoolean("dynamic_color", true)) }
            var iconScale by rememberSaveable { mutableStateOf(prefs.getFloat("icon_scale", 1.0f)) }
            var drawerColumns by rememberSaveable { mutableIntStateOf(prefs.getInt("drawer_columns", 4)) }
            var workspaceColumns by rememberSaveable { mutableIntStateOf(prefs.getInt("workspace_columns", 4)) }
            var showLabels by rememberSaveable { mutableStateOf(prefs.getBoolean("show_labels", true)) }
            var hiddenApps by remember { mutableStateOf(prefs.getStringSet("hidden_apps", emptySet())?.toMutableSet() ?: mutableSetOf()) }
            var pinnedApps by remember { mutableStateOf(loadStringList("pinned_apps").toMutableList()) }
            var dockApps by remember { mutableStateOf(loadStringList("dock_apps").toMutableList()) }
            var renamedApps by remember { mutableStateOf(loadMap("renamed_apps").toMutableMap()) }
            var selectedApp by remember { mutableStateOf<LauncherApp?>(null) }

            SWMaterialTheme(themeName = themeName, dynamicColor = dynamicColor) {
                Box(Modifier.fillMaxSize()) {
                    AuroraBackground(themeName)
                    AnimatedContent(
                        targetState = screen,
                        transitionSpec = { (fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.98f)).togetherWith(fadeOut(tween(160)) + scaleOut(tween(160), targetScale = 1.02f)) },
                        label = "screen"
                    ) { target ->
                        when (target) {
                            Screen.Onboarding -> OnboardingScreen { prefs.edit().putBoolean("onboarding_done", true).apply(); screen = Screen.Home }
                            Screen.Home -> HomeScreen(apps, pinnedApps, dockApps, hiddenApps, renamedApps, iconScale, workspaceColumns, showLabels, { screen = Screen.Drawer }, { screen = Screen.Settings }, { apps = loadLauncherApps() }, { openApp(it) }, { selectedApp = it })
                            Screen.Drawer -> DrawerScreen(apps, hiddenApps, renamedApps, iconScale, drawerColumns, showLabels, { screen = Screen.Home }, { openApp(it) }, { selectedApp = it }, { getUseCount(it.key) }, { getRecentIndex(it.key) })
                            Screen.Settings -> SettingsScreen(apps.size, themeName, dynamicColor, iconScale, drawerColumns, workspaceColumns, showLabels, { screen = Screen.Home },
                                { themeName = it; prefs.edit().putString("theme", it).apply() },
                                { dynamicColor = it; prefs.edit().putBoolean("dynamic_color", it).apply() },
                                { iconScale = it; prefs.edit().putFloat("icon_scale", it).apply() },
                                { drawerColumns = it; prefs.edit().putInt("drawer_columns", it).apply() },
                                { workspaceColumns = it; prefs.edit().putInt("workspace_columns", it).apply() },
                                { showLabels = it; prefs.edit().putBoolean("show_labels", it).apply() },
                                { apps = loadLauncherApps() },
                                {
                                    prefs.edit().clear().apply(); themeName = "aurora"; dynamicColor = true; iconScale = 1.0f; drawerColumns = 4; workspaceColumns = 4; showLabels = true; hiddenApps = mutableSetOf(); pinnedApps = mutableListOf(); dockApps = mutableListOf(); renamedApps = mutableMapOf()
                                })
                        }
                    }
                    selectedApp?.let { app ->
                        AppActionsSheet(
                            app = app,
                            displayedName = renamedApps[app.key] ?: app.label,
                            isPinned = pinnedApps.contains(app.key),
                            inDock = dockApps.contains(app.key),
                            hidden = hiddenApps.contains(app.key),
                            onDismiss = { selectedApp = null },
                            onOpen = { selectedApp = null; openApp(app) },
                            onPinToggle = { pinnedApps = pinnedApps.toggle(app.key).take(24).toMutableList(); saveStringList("pinned_apps", pinnedApps) },
                            onDockToggle = { dockApps = dockApps.toggle(app.key).take(5).toMutableList(); saveStringList("dock_apps", dockApps) },
                            onHideToggle = { hiddenApps = hiddenApps.toMutableSet().apply { if (contains(app.key)) remove(app.key) else add(app.key) }; prefs.edit().putStringSet("hidden_apps", hiddenApps).apply() },
                            onRename = { newName -> renamedApps = renamedApps.toMutableMap().apply { if (newName.isBlank()) remove(app.key) else put(app.key, newName.trim()) }; saveMap("renamed_apps", renamedApps) },
                            onInfo = { openAppInfo(app.packageName) },
                            onUninstall = { uninstallApp(app.packageName) }
                        )
                    }
                }
            }
        }
    }

    private fun loadLauncherApps(): List<LauncherApp> {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val collator = Collator.getInstance(Locale.getDefault())
        return pm.queryIntentActivities(intent, 0).mapNotNull { info ->
            val label = info.loadLabel(pm)?.toString()?.trim().orEmpty()
            val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
            val act = info.activityInfo?.name ?: return@mapNotNull null
            LauncherApp(if (label.isBlank()) pkg else label, pkg, act, info.loadIcon(pm), detectCategory(label, pkg))
        }.sortedWith { a, b -> collator.compare(a.label, b.label) }
    }

    private fun openApp(app: LauncherApp) {
        try { recordUse(app.key); startActivity(Intent().setClassName(app.packageName, app.activityName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        catch (_: Exception) { openAppInfo(app.packageName) }
    }
    private fun openAppInfo(packageName: String) = startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    private fun uninstallApp(packageName: String) = startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    private fun detectCategory(label: String, pkg: String): String {
        val s = (label + " " + pkg).lowercase(Locale.getDefault())
        return when {
            listOf("game", "roblox", "brawl", "twitch", "gaming", "steam", "xbox").any { s.contains(it) } -> "Jogos"
            listOf("instagram", "whatsapp", "discord", "telegram", "facebook", "snap", "tiktok", "x.com", "twitter").any { s.contains(it) } -> "Social"
            listOf("youtube", "spotify", "music", "galeria", "photos", "camera", "capcut", "bandlab", "media").any { s.contains(it) } -> "Mídia"
            listOf("file", "arquivos", "settings", "config", "calcul", "clock", "termi", "tool").any { s.contains(it) } -> "Ferramentas"
            else -> "Apps"
        }
    }
    private fun recordUse(key: String) = prefs.edit().putInt("use_$key", prefs.getInt("use_$key", 0) + 1).putLong("recent_$key", System.currentTimeMillis()).apply()
    private fun getUseCount(key: String): Int = prefs.getInt("use_$key", 0)
    private fun getRecentIndex(key: String): Long = prefs.getLong("recent_$key", 0L)
    private fun loadStringList(name: String): List<String> = prefs.getString(name, "")?.split("|||")?.filter { it.isNotBlank() } ?: emptyList()
    private fun saveStringList(name: String, items: List<String>) = prefs.edit().putString(name, items.joinToString("|||")).apply()
    private fun loadMap(name: String): Map<String, String> = (prefs.getString(name, "") ?: "").split("|||").mapNotNull { val p = it.split("=>", limit = 2); if (p.size == 2) p[0] to p[1] else null }.toMap()
    private fun saveMap(name: String, map: Map<String, String>) = prefs.edit().putString(name, map.entries.joinToString("|||") { "${it.key}=>${it.value}" }).apply()
}

private fun MutableList<String>.toggle(value: String): List<String> = if (contains(value)) filter { it != value } else listOf(value) + this

@Composable
fun SWMaterialTheme(themeName: String, dynamicColor: Boolean, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= 31 && systemDark -> dynamicDarkColorScheme(context)
        dynamicColor && Build.VERSION.SDK_INT >= 31 && !systemDark -> dynamicLightColorScheme(context)
        themeName == "amoled" -> darkColorScheme(primary = Color(0xFFCDBDFF), onPrimary = Color(0xFF241047), primaryContainer = Color(0xFF4F378B), onPrimaryContainer = Color(0xFFEADDFF), background = Color.Black, surface = Color(0xFF0A0A0F), surfaceVariant = Color(0xFF211B2E), onSurface = Color(0xFFF5EFF7), onSurfaceVariant = Color(0xFFD0C4D8), outline = Color(0xFF7D7286))
        themeName == "blue" -> lightColorScheme(primary = Color(0xFF2D5BFF), onPrimary = Color.White, primaryContainer = Color(0xFFDCE2FF), onPrimaryContainer = Color(0xFF001A78), background = Color(0xFFF8F9FF), surface = Color.White, surfaceVariant = Color(0xFFE2E5F5), onSurface = Color(0xFF181A20), onSurfaceVariant = Color(0xFF454752), outline = Color(0xFF757786))
        themeName == "porcelain" -> lightColorScheme(primary = Color(0xFF6750A4), onPrimary = Color.White, primaryContainer = Color(0xFFEADDFF), onPrimaryContainer = Color(0xFF21005D), background = Color(0xFFFFFBFE), surface = Color(0xFFFFFBFE), surfaceVariant = Color(0xFFE7E0EC), onSurface = Color(0xFF1D1B20), onSurfaceVariant = Color(0xFF49454F), outline = Color(0xFF79747E))
        else -> lightColorScheme(primary = Color(0xFF7B61D1), onPrimary = Color.White, primaryContainer = Color(0xFFEADDFF), onPrimaryContainer = Color(0xFF21005D), background = Color(0xFFFDF7FF), surface = Color(0xFFFFF7FF), surfaceVariant = Color(0xFFEFE4F8), onSurface = Color(0xFF201A26), onSurfaceVariant = Color(0xFF51465D), outline = Color(0xFF83758E))
    }
    MaterialTheme(colorScheme = colors, shapes = Shapes(extraSmall = RoundedCornerShape(10.dp), small = RoundedCornerShape(16.dp), medium = RoundedCornerShape(24.dp), large = RoundedCornerShape(34.dp), extraLarge = RoundedCornerShape(42.dp)), content = content)
}

@Composable
fun AuroraBackground(themeName: String) {
    val c = MaterialTheme.colorScheme
    val brush = when (themeName) {
        "amoled" -> Brush.verticalGradient(listOf(Color.Black, Color(0xFF07030D), Color.Black))
        "blue" -> Brush.verticalGradient(listOf(Color(0xFFEFF3FF), Color(0xFFD7E0FF), Color(0xFFBFCBFF)))
        "porcelain" -> Brush.verticalGradient(listOf(Color(0xFFFFFBFE), Color(0xFFF3EBFF), Color(0xFFEFE7F7)))
        else -> Brush.verticalGradient(listOf(Color(0xFFF6E5FF), Color(0xFFFFD7EA), Color(0xFF7361D9)))
    }
    Box(Modifier.fillMaxSize().background(brush).background(Brush.radialGradient(listOf(c.primary.copy(alpha = 0.14f), Color.Transparent), center = Offset(900f, 250f), radius = 650f)))
}

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(28.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Spacer(Modifier.height(16.dp))
        Column {
            Text("SW", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            Text("Launcher", fontSize = 38.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(20.dp))
            GlassCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(24.dp)) { Icon(Icons.Rounded.Widgets, null, Modifier.size(46.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(16.dp)); Text("Personal. Expressive. Yours.", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold); Spacer(Modifier.height(8.dp)); Text("Nova base Material 3 com gaveta, dock, temas, ações de app e personalização salva.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        }
        Button(onClick = onFinish, Modifier.fillMaxWidth().height(62.dp), shape = RoundedCornerShape(28.dp)) { Text("Começar", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
    }
}

@Composable
fun HomeScreen(apps: List<LauncherApp>, pinnedKeys: List<String>, dockKeys: List<String>, hiddenApps: Set<String>, renamedApps: Map<String, String>, iconScale: Float, columns: Int, showLabels: Boolean, onOpenDrawer: () -> Unit, onOpenSettings: () -> Unit, onRefresh: () -> Unit, onAppClick: (LauncherApp) -> Unit, onAppLongClick: (LauncherApp) -> Unit) {
    val visible = apps.filterNot { hiddenApps.contains(it.key) }
    val pinned = pinnedKeys.mapNotNull { key -> apps.firstOrNull { it.key == key } }.ifEmpty { visible.take(8) }
    val dock = dockKeys.mapNotNull { key -> apps.firstOrNull { it.key == key } }.ifEmpty { visible.take(5) }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("SW", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Black); Text("Launcher", fontWeight = FontWeight.ExtraBold, fontSize = 30.sp) }; PillButton("Apps", onOpenDrawer); Spacer(Modifier.width(10.dp)); IconButton(onOpenSettings, Modifier.softCircle()) { Icon(Icons.Rounded.Settings, null) } }
            Spacer(Modifier.height(22.dp)); ClockWeatherCard(visible.size); Spacer(Modifier.height(18.dp)); SearchPill("Pesquisar ou abrir apps", onOpenDrawer); Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) { HomeActionCard(Icons.Rounded.Apps, "Gaveta", "${visible.size} apps", Modifier.weight(1f), onOpenDrawer); HomeActionCard(Icons.Rounded.Palette, "Aparência", "cores e layout", Modifier.weight(1f), onOpenSettings) }
            Spacer(Modifier.height(16.dp)); Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) { HomeActionCard(Icons.Rounded.Add, "Editar", "área de trabalho", Modifier.weight(1f), onOpenSettings); HomeActionCard(Icons.Rounded.Settings, "Ajustes", "launcher", Modifier.weight(1f), onOpenSettings) }
            Spacer(Modifier.height(28.dp)); Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Área de trabalho", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f)); TextButton(onClick = onOpenSettings) { Text("Editar") } }
            LazyVerticalGrid(columns = GridCells.Fixed(columns.coerceIn(3, 6)), modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(top = 10.dp, bottom = 120.dp), horizontalArrangement = Arrangement.spacedBy(18.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) { items(pinned, key = { it.key }) { app -> AppTile(app, renamedApps[app.key] ?: app.label, iconScale, showLabels, { onAppClick(app) }, { onAppLongClick(app) }) } }
        }
        DockBar(dock, renamedApps, iconScale, onOpenDrawer, onAppClick, onAppLongClick, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable fun ClockWeatherCard(appsCount: Int) { val now = remember { Date() }; val time = remember { SimpleDateFormat("HH:mm", Locale.getDefault()).format(now) }; val date = remember { SimpleDateFormat("EEEE, dd MMM.", Locale("pt", "BR")).format(now) }; GlassCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(26.dp)) { Text(time, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary); Text(date, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(12.dp)); Text("Bom dia! Seu espaço está pronto.", color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(14.dp)); Row(verticalAlignment = Alignment.CenterVertically) { Text("☀️", fontSize = 26.sp); Text(" 24°", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold); Spacer(Modifier.width(14.dp)); Column { Text("Parcialmente nublado", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("↑ 27° · ↓ 18° · $appsCount apps", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) } } } } }

@Composable fun SearchPill(text: String, onClick: () -> Unit) { Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().height(76.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.66f), shape = RoundedCornerShape(34.dp), tonalElevation = 1.dp) { Row(Modifier.padding(horizontal = 28.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(12.dp)); Text(text, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
@Composable fun HomeActionCard(icon: ImageVector, title: String, subtitle: String, modifier: Modifier = Modifier, onClick: () -> Unit) { Surface(onClick = onClick, modifier = modifier.height(128.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.58f), shape = RoundedCornerShape(30.dp), tonalElevation = 2.dp) { Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)); Spacer(Modifier.height(14.dp)); Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) } } }

@OptIn(ExperimentalFoundationApi::class)
@Composable fun AppTile(app: LauncherApp, name: String, iconScale: Float, showLabel: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) { var pressed by remember { mutableStateOf(false) }; val scale by animateFloatAsState(if (pressed) 0.94f else 1f, tween(130), label = "press"); Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale).combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(2.dp)) { AppIcon(app.icon, 64.dp * iconScale.coerceIn(0.75f, 1.45f)); if (showLabel) { Spacer(Modifier.height(8.dp)); Text(name, fontSize = 12.sp, maxLines = 2, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth()) } } }

@Composable fun AppIcon(drawable: Drawable, size: Dp) { val bitmap = remember(drawable) { drawableToImageBitmap(drawable, 144, 144) }; Box(Modifier.size(size).shadow(10.dp, RoundedCornerShape(size * 0.28f)).clip(RoundedCornerShape(size * 0.28f)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)), contentAlignment = Alignment.Center) { Image(bitmap, null, modifier = Modifier.fillMaxSize().padding(size * 0.08f), contentScale = ContentScale.Fit) } }
fun drawableToImageBitmap(drawable: Drawable, width: Int, height: Int): ImageBitmap { if (drawable is BitmapDrawable && drawable.bitmap != null) return drawable.bitmap.asImageBitmap(); val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888); val canvas = Canvas(bitmap); drawable.setBounds(0, 0, canvas.width, canvas.height); drawable.draw(canvas); return bitmap.asImageBitmap() }

@Composable fun DockBar(apps: List<LauncherApp>, renamedApps: Map<String, String>, iconScale: Float, onOpenDrawer: () -> Unit, onAppClick: (LauncherApp) -> Unit, onAppLongClick: (LauncherApp) -> Unit, modifier: Modifier = Modifier) { Surface(modifier = modifier.padding(horizontal = 24.dp).padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 14.dp).fillMaxWidth().height(82.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f), shape = RoundedCornerShape(36.dp), tonalElevation = 4.dp, shadowElevation = 10.dp) { Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) { apps.take(5).forEach { app -> Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { AppTile(app, renamedApps[app.key] ?: app.label, iconScale * 0.82f, false, { onAppClick(app) }, { onAppLongClick(app) }) } }; FloatingActionButton(onClick = onOpenDrawer, modifier = Modifier.size(64.dp), shape = CircleShape, containerColor = MaterialTheme.colorScheme.primaryContainer) { Icon(Icons.Rounded.Search, "Abrir gaveta") } } } }

@Composable fun DrawerScreen(apps: List<LauncherApp>, hiddenApps: Set<String>, renamedApps: Map<String, String>, iconScale: Float, columns: Int, showLabels: Boolean, onBack: () -> Unit, onAppClick: (LauncherApp) -> Unit, onAppLongClick: (LauncherApp) -> Unit, usageCount: (LauncherApp) -> Int, recentIndex: (LauncherApp) -> Long) { var query by rememberSaveable { mutableStateOf("") }; var filter by rememberSaveable { mutableStateOf(DrawerFilter.All) }; val base = apps.filterNot { hiddenApps.contains(it.key) }; val filtered = remember(base, query, filter) { val q = query.trim().lowercase(Locale.getDefault()); base.asSequence().filter { q.isBlank() || it.label.lowercase(Locale.getDefault()).contains(q) || it.packageName.lowercase(Locale.getDefault()).contains(q) }.filter { when (filter) { DrawerFilter.All -> true; DrawerFilter.Recent -> recentIndex(it) > 0L; DrawerFilter.Used -> usageCount(it) > 0; DrawerFilter.Games -> it.category == "Jogos"; DrawerFilter.Social -> it.category == "Social"; DrawerFilter.Media -> it.category == "Mídia"; DrawerFilter.Tools -> it.category == "Ferramentas" } }.sortedWith(compareByDescending<LauncherApp> { if (filter == DrawerFilter.Recent) recentIndex(it) else if (filter == DrawerFilter.Used) usageCount(it).toLong() else 0L }.thenBy { it.label.lowercase(Locale.getDefault()) }).toList() }
    Box(Modifier.fillMaxSize()) { Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onBack, Modifier.softCircle()) { Icon(Icons.Rounded.ArrowBack, "Voltar") }; Spacer(Modifier.width(12.dp)); Column { Text("Apps", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold); Text("${base.size} apps instalados", color = MaterialTheme.colorScheme.onSurfaceVariant) } }; Spacer(Modifier.height(22.dp)); SearchInput(query, { query = it }, "Pesquisar apps"); Spacer(Modifier.height(18.dp)); Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) { DrawerFilter.values().forEach { f -> FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(f.title, fontWeight = FontWeight.Bold) }, shape = RoundedCornerShape(24.dp)) } }; Spacer(Modifier.height(18.dp)); LazyVerticalGrid(columns = GridCells.Fixed(columns.coerceIn(3, 6)), modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp, end = 22.dp), horizontalArrangement = Arrangement.spacedBy(18.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) { items(filtered, key = { it.key }) { app -> AppTile(app, renamedApps[app.key] ?: app.label, iconScale, showLabels, { onAppClick(app) }, { onAppLongClick(app) }) } } }; AlphabetRail(Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)) }
}

@Composable fun AlphabetRail(modifier: Modifier = Modifier) { Surface(modifier = modifier.width(34.dp).fillMaxHeight(0.70f), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.36f), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly) { ('A'..'Z').forEach { Text(it.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.80f)) } } } }
@Composable fun SearchInput(value: String, onValueChange: (String) -> Unit, placeholder: String) { Surface(Modifier.fillMaxWidth().height(70.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.70f), shape = RoundedCornerShape(31.dp), tonalElevation = 1.dp) { Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.width(12.dp)); BasicTextField(value, onValueChange, textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp, fontWeight = FontWeight.Medium), singleLine = true, modifier = Modifier.weight(1f), decorationBox = { inner -> if (value.isBlank()) Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 17.sp); inner() }) } } }

@Composable fun SettingsScreen(appsCount: Int, themeName: String, dynamicColor: Boolean, iconScale: Float, drawerColumns: Int, workspaceColumns: Int, showLabels: Boolean, onBack: () -> Unit, onTheme: (String) -> Unit, onDynamicColor: (Boolean) -> Unit, onIconScale: (Float) -> Unit, onDrawerColumns: (Int) -> Unit, onWorkspaceColumns: (Int) -> Unit, onShowLabels: (Boolean) -> Unit, onReload: () -> Unit, onReset: () -> Unit) { var sheet by remember { mutableStateOf<String?>(null) }; Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onBack, Modifier.softCircle()) { Icon(Icons.Rounded.ArrowBack, "Voltar") }; Spacer(Modifier.width(12.dp)); Column { Text("Configurações", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold); Text("Material 3, layout, dock e personalização", color = MaterialTheme.colorScheme.onSurfaceVariant) } }; Spacer(Modifier.height(24.dp)); LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 80.dp)) { item { SettingsRow(Icons.Rounded.Home, "Tela inicial", "Colunas, apps fixos e edição da área") { sheet = "workspace" } }; item { SettingsRow(Icons.Rounded.Apps, "Gaveta de apps", "Busca, categorias, ocultos e rolagem A-Z") { sheet = "drawer" } }; item { SettingsRow(Icons.Rounded.Layers, "Dock", "Segure apps para adicionar/remover") { sheet = "dock" } }; item { SettingsRow(Icons.Rounded.Palette, "Aparência", "Aurora, Porcelain, Blue, AMOLED e Dynamic Color") { sheet = "appearance" } }; item { SettingsRow(Icons.Rounded.Brush, "Tamanho dos ícones", "Atual: ${(iconScale * 100).roundToInt()}%") { sheet = "icons" } }; item { SettingsRow(Icons.Rounded.Edit, "Nomes dos apps", if (showLabels) "Ativados" else "Ocultos") { sheet = "labels" } }; item { SettingsRow(Icons.Rounded.Refresh, "Recarregar apps", "$appsCount apps encontrados", onReload) }; item { SettingsRow(Icons.Rounded.RestartAlt, "Resetar layout", "Limpa área, dock, nomes e ocultos", onReset) }; item { SettingsRow(Icons.Rounded.Info, "Sobre", "SW Launcher v0.13 Compose Material 3") { sheet = "about" } } } }; sheet?.let { which -> SettingsSheet(when(which) { "appearance" -> "Aparência"; "drawer" -> "Gaveta de apps"; "workspace" -> "Área de trabalho"; "icons" -> "Ícones"; "labels" -> "Nomes dos apps"; "dock" -> "Dock"; else -> "Sobre" }, { sheet = null }) { when (which) { "appearance" -> AppearanceOptions(themeName, dynamicColor, onTheme, onDynamicColor); "drawer" -> Column { Text("Colunas da gaveta: $drawerColumns", fontWeight = FontWeight.Bold); Slider(drawerColumns.toFloat(), { onDrawerColumns(it.roundToInt().coerceIn(3, 6)) }, valueRange = 3f..6f, steps = 2) }; "workspace" -> Column { Text("Colunas da área: $workspaceColumns", fontWeight = FontWeight.Bold); Slider(workspaceColumns.toFloat(), { onWorkspaceColumns(it.roundToInt().coerceIn(3, 6)) }, valueRange = 3f..6f, steps = 2) }; "icons" -> Column { Text("Tamanho: ${(iconScale * 100).roundToInt()}%", fontWeight = FontWeight.Bold); Slider(iconScale, { onIconScale(it.coerceIn(0.75f, 1.45f)) }, valueRange = 0.75f..1.45f) }; "labels" -> Row(verticalAlignment = Alignment.CenterVertically) { Text("Mostrar nomes dos apps", Modifier.weight(1f), fontWeight = FontWeight.Bold); Switch(showLabels, onShowLabels) }; "dock" -> Text("Para editar o dock: segure um app e toque em Adicionar ao dock ou Remover do dock.", color = MaterialTheme.colorScheme.onSurfaceVariant); else -> Text("Base migrada para Kotlin + Jetpack Compose + Material 3. Esta versão cria a fundação visual profissional da SW Launcher.", color = MaterialTheme.colorScheme.onSurfaceVariant) } } } }

@Composable fun SettingsRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) { Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.64f), shape = RoundedCornerShape(28.dp), tonalElevation = 1.dp) { Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) { Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(58.dp)) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) } }; Spacer(Modifier.width(18.dp)); Column(Modifier.weight(1f)) { Text(title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) }; Icon(Icons.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) } } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun SettingsSheet(title: String, onDismiss: () -> Unit, content: @Composable () -> Unit) { ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f), shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp), tonalElevation = 6.dp) { Column(Modifier.padding(horizontal = 24.dp, vertical = 10.dp).padding(bottom = 32.dp)) { Text(title, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold); Spacer(Modifier.height(8.dp)); Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))); Spacer(Modifier.height(20.dp)); content() } } }

@Composable fun AppearanceOptions(themeName: String, dynamicColor: Boolean, onTheme: (String) -> Unit, onDynamicColor: (Boolean) -> Unit) { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Text("Usar cores do sistema/wallpaper", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Switch(dynamicColor, onDynamicColor) }; listOf("aurora" to "Aurora Glass", "porcelain" to "Pixel Porcelain", "blue" to "Android Blue", "amoled" to "AMOLED Ink").forEach { (key, label) -> Surface(onClick = { onTheme(key) }, modifier = Modifier.fillMaxWidth().height(64.dp), shape = RoundedCornerShape(25.dp), color = if (themeName == key) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)) { Row(Modifier.padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically) { Text(if (themeName == key) "✓ $label" else label, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f)) } } } } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun AppActionsSheet(app: LauncherApp, displayedName: String, isPinned: Boolean, inDock: Boolean, hidden: Boolean, onDismiss: () -> Unit, onOpen: () -> Unit, onPinToggle: () -> Unit, onDockToggle: () -> Unit, onHideToggle: () -> Unit, onRename: (String) -> Unit, onInfo: () -> Unit, onUninstall: () -> Unit) { var renameMode by remember { mutableStateOf(false) }; var newName by remember { mutableStateOf(displayedName) }; ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f), shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp), tonalElevation = 8.dp) { Column(Modifier.padding(24.dp).padding(bottom = 28.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { AppIcon(app.icon, 76.dp); Spacer(Modifier.width(18.dp)); Column(Modifier.weight(1f)) { Text(displayedName, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(app.packageName, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) } }; Spacer(Modifier.height(20.dp)); Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) { ActionButton("Abrir", Icons.Rounded.OpenInNew, Modifier.weight(1f), onOpen); ActionButton(if (isPinned) "Desfixar" else "Fixar", Icons.Rounded.PushPin, Modifier.weight(1f), onPinToggle); ActionButton("Info", Icons.Rounded.Info, Modifier.weight(1f), onInfo) }; Spacer(Modifier.height(12.dp)); if (renameMode) { SearchInput(newName, { newName = it }, "Novo nome"); Spacer(Modifier.height(10.dp)); Button({ onRename(newName); renameMode = false }, Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(24.dp)) { Text("Salvar nome", fontWeight = FontWeight.Bold) } } else SheetOption("Editar nome exibido", Icons.Rounded.Edit) { renameMode = true }; SheetOption(if (inDock) "Remover do dock" else "Adicionar ao dock", Icons.Rounded.Layers, onDockToggle); SheetOption(if (hidden) "Mostrar na gaveta" else "Ocultar da gaveta", Icons.Rounded.VisibilityOff, onHideToggle); SheetOption("Cache e dados", Icons.Rounded.Settings, onInfo); SheetOption("Desinstalar", Icons.Rounded.Delete, onUninstall) } } }

@Composable fun ActionButton(text: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) { Surface(onClick = onClick, modifier = modifier.height(58.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f), shape = RoundedCornerShape(24.dp)) { Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(text, fontWeight = FontWeight.Bold) } } }
@Composable fun SheetOption(text: String, icon: ImageVector, onClick: () -> Unit) { Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(62.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f), shape = RoundedCornerShape(24.dp)) { Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null); Spacer(Modifier.width(14.dp)); Text(text, fontWeight = FontWeight.ExtraBold) } } }
@Composable fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) { Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.64f), shape = RoundedCornerShape(34.dp), tonalElevation = 2.dp, shadowElevation = 8.dp, content = content) }
@Composable fun PillButton(text: String, onClick: () -> Unit) { Surface(onClick = onClick, modifier = Modifier.height(60.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.70f), shape = RoundedCornerShape(30.dp), tonalElevation = 2.dp) { Box(Modifier.padding(horizontal = 28.dp), contentAlignment = Alignment.Center) { Text(text, fontWeight = FontWeight.ExtraBold) } } }
@Composable fun Modifier.softCircle(): Modifier = this.size(60.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f))

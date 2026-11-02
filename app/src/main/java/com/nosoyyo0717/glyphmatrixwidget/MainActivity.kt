package com.nosoyyo0717.glyphmatrixwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import com.nosoyyo0717.glyphmatrixwidget.designs.minecraft.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContent {
            Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                AppRouter()
            }
        }
    }
}

object MatrixCache {
    val bitmaps = mutableMapOf<String, ImageBitmap>()
    val matrices = mutableMapOf<String, IntArray>()

    suspend fun initialize(deviceModel: String, activeCategories: Map<String, List<String>>) {
        withContext(Dispatchers.Default) {
            bitmaps.clear()
            matrices.clear()
            val allMobs = activeCategories.values.flatten().toSet()
            val imageSize = 500f

            allMobs.forEach { mobName ->
                val matrix = GameData.getMasterMatrixDesign(mobName, deviceModel)
                matrices[mobName] = matrix

                val gridSize = if (deviceModel == "4aPro") 13 else 25
                val activeWidths = if (deviceModel == "4aPro") PHONE_4A_PRO_ACTIVE_WIDTHS else PHONE_3_ACTIVE_WIDTHS
                val bmp = Bitmap.createBitmap(imageSize.toInt(), imageSize.toInt(), Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bmp)
                val paint = android.graphics.Paint().apply { isAntiAlias = true }
                val visualGridSize = if (deviceModel == "4aPro") 13f else 25f
                val dotSize = imageSize / visualGridSize
                val xOffset = 0f
                val gap = dotSize * 0.05f
                val cornerRadius = dotSize * 0.15f

                for (row in 0 until gridSize) {
                    val activeCount = activeWidths.getOrElse(row) { 0 }
                    if (activeCount == 0) continue
                    val emptySpaces = (gridSize - activeCount) / 2
                    for (col in emptySpaces until (emptySpaces + activeCount)) {
                        val index = row * gridSize + col
                        val brightness = matrix.getOrElse(index) { 0 }
                        if (brightness > 0) {
                            paint.color = android.graphics.Color.argb(brightness, 255, 255, 255)
                        } else {
                            paint.color = android.graphics.Color.rgb(35, 35, 35)
                        }
                        val left = (col * dotSize) + xOffset
                        val top = row * dotSize
                        canvas.drawRoundRect(left + gap, top + gap, left + dotSize - gap, top + dotSize - gap, cornerRadius, cornerRadius, paint)
                    }
                }
                bitmaps[mobName] = bmp.asImageBitmap()
            }
        }
    }
}

@Composable
fun AppRouter() {
    var currentScreen by remember { mutableStateOf("HOME") }
    var selectedCategory by remember { mutableStateOf("") }
    var selectedMob by remember { mutableStateOf("") }
    var currentGame by remember { mutableStateOf("Minecraft") }

    val actualDeviceModel = remember { Build.MODEL ?: "" }
    val isNative4aPro = remember { actualDeviceModel.contains("4a", ignoreCase = true) || actualDeviceModel.contains("A069P", ignoreCase = true) }
    val isNativePhone3 = remember { actualDeviceModel.contains("Phone 3", ignoreCase = true) || actualDeviceModel == "A014" || actualDeviceModel == "A013" }
    val isSimulatorMode = remember { !(isNative4aPro || isNativePhone3) }
    var activeDeviceMode by remember { mutableStateOf(if (isNative4aPro) "4aPro" else "3") }

    val completed4aProItems = listOf("Creeper", "Skeleton", "Carved Snow Golem", "Turtle Egg")

    val displayCategories = remember(activeDeviceMode, currentGame) {
        val rawCategories = GameData.ALL_GAMES[currentGame] ?: emptyMap()

        rawCategories.mapValues { (_, mobList) ->
            mobList.filter { mobName ->
                val supportedTags = GameData.getMasterTags(mobName)
                val isSupported = supportedTags.contains(activeDeviceMode)
                val isFinishedFor4a = if (activeDeviceMode == "4aPro") completed4aProItems.contains(mobName) else true
                isSupported && isFinishedFor4a
            }
        }.filterValues { it.isNotEmpty() }
    }

    var isCacheReady by remember { mutableStateOf(false) }
    LaunchedEffect(activeDeviceMode, currentGame) {
        isCacheReady = false
        MatrixCache.initialize(activeDeviceMode, displayCategories)
        isCacheReady = true
    }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }

    val drawerWidthPx = screenWidthPx * 0.5f
    val minLeftWidthPx = with(density) { 64.dp.toPx() }

    val rightOffsetX = remember { Animatable(drawerWidthPx) }
    val leftWidthPx = remember { Animatable(minLeftWidthPx) }
    val scope = rememberCoroutineScope()

    val view = LocalView.current

    fun openRight() {
        if (rightOffsetX.value > 0f) view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        scope.launch { rightOffsetX.animateTo(0f, tween(300, easing = FastOutSlowInEasing)) }
    }
    fun closeRight() { scope.launch { rightOffsetX.animateTo(drawerWidthPx, tween(300, easing = FastOutSlowInEasing)) } }

    fun openLeft() {
        if (leftWidthPx.value < drawerWidthPx) view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        scope.launch { leftWidthPx.animateTo(drawerWidthPx, tween(300, easing = FastOutSlowInEasing)) }
    }
    fun closeLeft() { scope.launch { leftWidthPx.animateTo(minLeftWidthPx, tween(300, easing = FastOutSlowInEasing)) } }

    val isRightOpen = rightOffsetX.targetValue < drawerWidthPx
    val isLeftOpen = leftWidthPx.targetValue > minLeftWidthPx

    var lastDragAmount by remember { mutableFloatStateOf(0f) }

    BackHandler(enabled = currentScreen != "HOME" || isRightOpen || isLeftOpen) {
        if (isLeftOpen) closeLeft()
        else if (isRightOpen) closeRight()
        else if (currentScreen == "MATRIX") currentScreen = "CATEGORY_DETAILS"
        else if (currentScreen == "CATEGORY_DETAILS") currentScreen = "HOME"
        else if (currentScreen == "ABOUT_APP" || currentScreen == "ABOUT_DEVELOPER") currentScreen = "HOME"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val isFlickRight = lastDragAmount > 5f
                        val isFlickLeft = lastDragAmount < -5f

                        val rightIsActive = rightOffsetX.value < drawerWidthPx
                        val leftIsActive = leftWidthPx.value > minLeftWidthPx

                        if (rightIsActive) {
                            if (isFlickLeft) openRight()
                            else if (isFlickRight) closeRight()
                            else if (rightOffsetX.value < drawerWidthPx / 2) openRight()
                            else closeRight()
                        } else if (leftIsActive) {
                            if (isFlickRight) openLeft()
                            else if (isFlickLeft) closeLeft()
                            else if (leftWidthPx.value > minLeftWidthPx + (drawerWidthPx - minLeftWidthPx) / 3) openLeft()
                            else closeLeft()
                        } else {
                            if (isFlickLeft) openRight()
                            else if (isFlickRight) openLeft()
                        }

                        lastDragAmount = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        lastDragAmount = dragAmount

                        scope.launch {
                            if (dragAmount < 0 && leftWidthPx.value == minLeftWidthPx) {
                                rightOffsetX.snapTo((rightOffsetX.value + dragAmount).coerceIn(0f, drawerWidthPx))
                            } else if (dragAmount > 0 && rightOffsetX.value < drawerWidthPx) {
                                rightOffsetX.snapTo((rightOffsetX.value + dragAmount).coerceIn(0f, drawerWidthPx))
                            }

                            if (dragAmount > 0 && rightOffsetX.value == drawerWidthPx) {
                                leftWidthPx.snapTo((leftWidthPx.value + dragAmount).coerceIn(minLeftWidthPx, drawerWidthPx))
                            } else if (dragAmount < 0 && leftWidthPx.value > minLeftWidthPx) {
                                leftWidthPx.snapTo((leftWidthPx.value + dragAmount).coerceIn(minLeftWidthPx, drawerWidthPx))
                            }
                        }
                    }
                )
            }
    ) {
        if (!isCacheReady) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(start = 64.dp)) {
                Crossfade(targetState = currentScreen, label = "ScreenTransitionFade") { screen ->
                    when (screen) {
                        "HOME" -> {
                            HomeScreen(
                                currentGame = currentGame,
                                categories = displayCategories,
                                deviceModel = activeDeviceMode,
                                isSimulatorMode = isSimulatorMode,
                                onDeviceSwitch = {
                                    isCacheReady = false
                                    activeDeviceMode = it
                                },
                                onCategoryClick = { categoryName ->
                                    selectedCategory = categoryName
                                    currentScreen = "CATEGORY_DETAILS"
                                },
                                onOpenRightMenu = { openRight() }
                            )
                        }
                        "CATEGORY_DETAILS" -> {
                            CategoryScreen(
                                categoryName = selectedCategory,
                                mobList = displayCategories[selectedCategory] ?: emptyList(),
                                onBackClick = { currentScreen = "HOME" },
                                onMobClick = { mobName ->
                                    selectedMob = mobName
                                    currentScreen = "MATRIX"
                                }
                            )
                        }
                        "MATRIX" -> MatrixPreviewScreen(mobName = selectedMob, onBackClick = { currentScreen = "CATEGORY_DETAILS" })
                        "ABOUT_APP" -> AboutAppScreen(onBackClick = { currentScreen = "HOME" })
                        "ABOUT_DEVELOPER" -> AboutDeveloperScreen(onBackClick = { currentScreen = "HOME" })
                    }
                }
            }
        }

        val rightFraction = (1f - (rightOffsetX.value / drawerWidthPx)).coerceIn(0f, 1f)
        val leftFraction = ((leftWidthPx.value - minLeftWidthPx) / (drawerWidthPx - minLeftWidthPx)).coerceIn(0f, 1f)
        val maxFraction = maxOf(rightFraction, leftFraction)

        if (maxFraction > 0f) {
            Box(modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f * maxFraction))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    closeRight()
                    closeLeft()
                }
            )
        }

        LeftSidebar(
            currentGame = currentGame,
            isExpanded = leftWidthPx.value > minLeftWidthPx + 20f,
            onGameSelected = { game ->
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                isCacheReady = false
                currentGame = game
                closeLeft()
                currentScreen = "HOME"
            },
            onToggleExpand = {
                if (isLeftOpen) closeLeft() else openLeft()
            },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(with(density) { leftWidthPx.value.toDp() })
        )

        RightSidebar(onClose = { closeRight() }, onNavigateToAboutApp = { closeRight(); currentScreen = "ABOUT_APP" }, onNavigateToDeveloper = { closeRight(); currentScreen = "ABOUT_DEVELOPER" }, modifier = Modifier.align(Alignment.CenterEnd).offset { IntOffset(rightOffsetX.value.roundToInt(), 0) })
    }
}

@Composable
fun HomeScreen(currentGame: String, categories: Map<String, List<String>>, deviceModel: String, isSimulatorMode: Boolean, onDeviceSwitch: (String) -> Unit, onCategoryClick: (String) -> Unit, onOpenRightMenu: () -> Unit) {

    fun getGameColor(gameName: String): Color {
        return when(gameName.uppercase()) {
            "MINECRAFT" -> Color(0xFF55FF55)
            "HOLLOW KNIGHT" -> Color(0xFFFCEE0A)
            "TERRARIA" -> Color(0xFF86E18D)
            "STARDEW" -> Color(0xFFFFB24A)
            "HOME" -> Color.White
            else -> Color.White
        }
    }

    val dynamicGameColor = getGameColor(currentGame)

    Column(modifier = Modifier.fillMaxSize().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {

        Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 24.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.Center)) {

                Text(
                    text = "GLYPH MATRIX WIDGET",
                    color = Color.White,
                    fontSize = 18.sp,
                    letterSpacing = 2.sp,
                    fontFamily = MonocraftFont
                )

                Text(
                    text = currentGame.uppercase(),
                    color = dynamicGameColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    fontFamily = MonocraftFont
                )
            }

            Text(
                text = "⚙",
                color = Color.White,
                fontSize = 28.sp,
                modifier = Modifier.align(Alignment.CenterEnd).clickable { onOpenRightMenu() }.padding(end = 24.dp, top = 6.dp)
            )
        }

        if (isSimulatorMode) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.Center) {
                Button(onClick = { onDeviceSwitch("3") }, colors = ButtonDefaults.buttonColors(containerColor = if (deviceModel == "3") Color.White else Color(0xFF222222), contentColor = if (deviceModel == "3") Color.Black else Color.Gray)) { Text("PHONE 3", fontFamily = MonocraftFont, fontWeight = FontWeight.Bold) }
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = { onDeviceSwitch("4aPro") }, colors = ButtonDefaults.buttonColors(containerColor = if (deviceModel == "4aPro") Color.White else Color(0xFF222222), contentColor = if (deviceModel == "4aPro") Color.Black else Color.Gray)) { Text("PHONE 4a PRO", fontFamily = MonocraftFont, fontWeight = FontWeight.Bold) }
            }
        }

        if (categories.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("COMING SOON...", color = Color.Gray, fontSize = 16.sp, fontFamily = MonocraftFont) }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                val categoryList = categories.keys.toList()
                items(categoryList) { categoryName ->
                    val mobList = categories[categoryName] ?: emptyList()

                    Card(
                        modifier = Modifier.fillMaxWidth().aspectRatio(0.9f).clickable { onCategoryClick(categoryName) },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1C)),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (mobList.isNotEmpty()) {
                                var currentIndex by remember(deviceModel, mobList) { mutableIntStateOf(if (mobList.isNotEmpty()) mobList.indices.random() else 0) }
                                LaunchedEffect(mobList) {
                                    while (true) {
                                        delay(5000)
                                        if (mobList.size > 1) { currentIndex = (currentIndex + (1 until mobList.size).random()) % mobList.size }
                                    }
                                }
                                Crossfade(targetState = mobList[currentIndex], animationSpec = tween(1500), label = "Morph", modifier = Modifier.align(Alignment.Center).padding(bottom = 32.dp)) { currentMobName ->
                                    MiniMatrixPreview(mobName = currentMobName, modifier = Modifier.size(110.dp))
                                }
                            }

                            Text(
                                text = categoryName.uppercase(),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                fontFamily = MonocraftFont,
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp, start = 8.dp, end = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryScreen(categoryName: String, mobList: List<String>, onBackClick: () -> Unit, onMobClick: (String) -> Unit) {
    val displayName = categoryName.uppercase()
    Column(modifier = Modifier.fillMaxSize().padding(top = 24.dp, bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp, bottom = 40.dp)) {
            Text("<", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Medium, fontFamily = MonocraftFont, modifier = Modifier.align(Alignment.CenterStart).clickable { onBackClick() }.padding(start = 24.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.Center).padding(horizontal = 32.dp)) {
                Text("CATEGORY", color = Color.White, fontSize = 12.sp, letterSpacing = 4.sp, fontFamily = MonocraftFont)
                Text(displayName, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontFamily = MonocraftFont)
            }
        }
        LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            if (mobList.isNotEmpty()) {
                items(mobList) { mobName ->
                    Card(modifier = Modifier.fillMaxWidth().aspectRatio(0.85f).clickable { onMobClick(mobName) }, colors = CardDefaults.cardColors(containerColor = Color(0xFF151515)), shape = RoundedCornerShape(24.dp)) {
                        Column(modifier = Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0xFF0A0A0A)), contentAlignment = Alignment.Center) {
                                MiniMatrixPreview(mobName = mobName, modifier = Modifier.padding(8.dp).aspectRatio(1f))
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = mobName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = MonocraftFont, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MatrixPreviewScreen(mobName: String, onBackClick: () -> Unit) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().padding(top = 24.dp, bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
        Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp, bottom = 40.dp)) {
            Text("<", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Medium, fontFamily = MonocraftFont, modifier = Modifier.align(Alignment.CenterStart).clickable { onBackClick() }.padding(start = 24.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.Center).padding(horizontal = 32.dp)) {
                Text("DESIGN PREVIEW", color = Color.White, fontSize = 12.sp, letterSpacing = 4.sp, fontFamily = MonocraftFont)
                Text(mobName.uppercase(), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontFamily = MonocraftFont)
            }
        }
        Box(modifier = Modifier.size(320.dp), contentAlignment = Alignment.Center) { MiniMatrixPreview(mobName = mobName, modifier = Modifier.size(280.dp)) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Button(onClick = { pinWidgetToHomeScreen(context, mobName) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black), shape = RoundedCornerShape(16.dp)) {
                Text("ADD TO HOME SCREEN", fontFamily = MonocraftFont, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        } else {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
fun AboutAppScreen(onBackClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp, bottom = 40.dp)) {
            Text("<", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Medium, fontFamily = MonocraftFont, modifier = Modifier.align(Alignment.CenterStart).clickable { onBackClick() })
            Text("ABOUT APP", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontFamily = MonocraftFont, modifier = Modifier.align(Alignment.Center))
        }
        Text(text = "Welcome to Glyph Matrix Widget!\n\nThis app allows you to customize your home screen with a massive library of high-quality dot-matrix designs.\n\nChoose from wide variety of your favorite games and add them to your home screen.\n\nMore Game Matrix Designs will be added soon.", color = Color.White, fontSize = 16.sp, fontFamily = MonocraftFont, lineHeight = 28.sp, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun AboutDeveloperScreen(onBackClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp, bottom = 40.dp)) {
            Text("<", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Medium, fontFamily = MonocraftFont, modifier = Modifier.align(Alignment.CenterStart).clickable { onBackClick() })
            Text("ABOUT DEVELOPER", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontFamily = MonocraftFont, modifier = Modifier.align(Alignment.Center))
        }
        Text(text = "This is my first app.\n\nI'll be building more unique apps (or games one day). Soo see ya.\n\n-NoSoyYo0717\n\nemail: nosoyyo0717@gmail.com", color = Color.White, fontSize = 16.sp, fontFamily = MonocraftFont, lineHeight = 28.sp, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun MiniMatrixPreview(mobName: String, modifier: Modifier = Modifier) {
    val bitmap = MatrixCache.bitmaps[mobName]
    if (bitmap != null) { Image(bitmap = bitmap, contentDescription = mobName, contentScale = ContentScale.Fit, modifier = modifier) }
}

fun pinWidgetToHomeScreen(context: Context, mobName: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val myProvider = ComponentName(context, MatrixWidgetProvider::class.java)

        if (appWidgetManager.isRequestPinAppWidgetSupported) {
            val successIntent = Intent(context, MatrixWidgetProvider::class.java).apply {
                action = "ACTION_PIN_WIDGET_SUCCESS"
                putExtra("PINNED_MOB", mobName)
            }
            val successPendingIntent = PendingIntent.getBroadcast(context, mobName.hashCode(), successIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
            appWidgetManager.requestPinAppWidget(myProvider, null, successPendingIntent)
        } else {
            Toast.makeText(context, "Your launcher does not support in-app pinning.", Toast.LENGTH_SHORT).show()
        }
    }
}

fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open link.", Toast.LENGTH_SHORT).show()
    }
}

fun shareApp(context: Context) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, "Check out GlyphMatrix Widget! It lets you add custom game designs to your home screen: https://play.google.com/store/apps/details?id=com.nosoyyo0717.glyphmatrixwidget")
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Share app via")
    context.startActivity(shareIntent)
}
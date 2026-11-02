package com.nosoyyo0717.glyphmatrixwidget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource

val MonocraftFont = FontFamily(Font(R.font.monocraft))

@Composable
fun LeftSidebar(
    currentGame: String,
    isExpanded: Boolean,
    onGameSelected: (String) -> Unit,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(Color(0xFF151515))
            .padding(vertical = 24.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .padding(top = 2.dp)
                .clickable { onToggleExpand() },
            contentAlignment = Alignment.Center
        ) {
            if (isExpanded) {
                Text("GAMES", color = Color.White, fontSize = 20.sp, fontFamily = MonocraftFont, fontWeight = FontWeight.Bold)
            } else {
                Text("☰", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        val gamesList = listOf("Minecraft", "Hollow Knight")

        gamesList.forEach { game ->
            val isSelected = game == currentGame

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onGameSelected(game) }
                    .padding(vertical = 2.dp)
                    .padding(start = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) Color.White else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    val iconResource = when (game) {
                        "Minecraft" -> R.drawable.creeper_icon
                        // "Hollow Knight" -> R.drawable.hollow_knight_icon
                        // "Terraria" -> R.drawable.terraria_icon
                        // "Stardew" -> R.drawable.stardew_icon
                        else -> 0
                    }

                    if (iconResource != 0) {
                        Image(
                            bitmap = ImageBitmap.imageResource(id = iconResource),
                            contentDescription = "$game Icon",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            contentScale = ContentScale.Fit,
                            filterQuality = FilterQuality.None
                        )
                    } else {
                        Text(
                            text = game.take(1),
                            color = if (isSelected) Color.Black else Color.LightGray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                    }
                }

                if (isExpanded) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = game,
                        color = if (isSelected) Color.White else Color.LightGray,
                        fontSize = 14.sp,
                        fontFamily = MonocraftFont,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun RightSidebar(onClose: () -> Unit, onNavigateToAboutApp: () -> Unit, onNavigateToDeveloper: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier = modifier.fillMaxHeight().fillMaxWidth(0.5f).background(Color(0xFF151515)).padding(start = 24.dp, end = 24.dp, top = 64.dp, bottom = 24.dp)) {
        Text("SETTINGS", color = Color.White, fontSize = 24.sp, fontFamily = MonocraftFont, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))

        @Composable fun SettingItem(text: String, onClick: () -> Unit) {
            Text(text = text, color = Color.LightGray, fontSize = 14.sp, fontFamily = MonocraftFont, modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 16.dp))
        }

        SettingItem("About App") { onNavigateToAboutApp() }
        SettingItem("About Developer") { onNavigateToDeveloper() }
        SettingItem("Report Bug") { openUrl(context, "https://docs.google.com/forms/d/e/1FAIpQLScy8yWh4xkFDx_Z77Omw_6ort-OS_R1GPqxu17pY_eMpvkNAA/viewform?usp=publish-editor") }
        SettingItem("Suggest Feature") { openUrl(context, "https://docs.google.com/forms/d/e/1FAIpQLSftuacoSqaQCEqnuLfgGEjOCxyIYmhD1VOZqQPBS1VtzQeOhQ/viewform?usp=publish-editor") }
        SettingItem("Rate App") { openUrl(context, "https://play.google.com/store/apps/details?id=com.nosoyyo0717.glyphmatrixwidget") }
        SettingItem("Share App") { shareApp(context) }
        SettingItem("Privacy Policy") { openUrl(context, "https://sites.google.com/view/privacypolicyglyphmatrixwidget") }

        Spacer(modifier = Modifier.weight(1f))

        //support me button
        Text(
            text = "Support Me",
            color = Color(0xFFFFA500),
            fontSize = 14.sp,
            fontFamily = MonocraftFont,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { openUrl(context, "https://www.buymeacoffee.com/nosoyyo0717") }
                .padding(vertical = 16.dp)
        )

        Text("CLOSE >", color = Color.Red, fontSize = 16.sp, fontFamily = MonocraftFont, modifier = Modifier.padding(vertical = 16.dp).clickable { onClose() })
    }
}
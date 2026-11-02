package com.nosoyyo0717.glyphmatrixwidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.widget.RemoteViews
import com.nosoyyo0717.glyphmatrixwidget.designs.minecraft.PHONE_3_ACTIVE_WIDTHS
import com.nosoyyo0717.glyphmatrixwidget.designs.minecraft.PHONE_4A_PRO_ACTIVE_WIDTHS

class MatrixWidgetProvider : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        startSixtySecondLoop(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        stopSixtySecondLoop(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "ACTION_PIN_WIDGET_SUCCESS") {
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            val pinnedMob = intent.getStringExtra("PINNED_MOB")

            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID && pinnedMob != null) {
                val prefs = context.getSharedPreferences("WidgetPrefs", Context.MODE_PRIVATE)
                prefs.edit().putString("widget_$appWidgetId", pinnedMob).apply()

                val appWidgetManager = AppWidgetManager.getInstance(context)
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }

        if (intent.action == "ACTION_REFRESH_MATRIX") {
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }

        if (intent.action == "ACTION_AUTO_UPDATE") {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, MatrixWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    private fun startSixtySecondLoop(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MatrixWidgetProvider::class.java).apply {
            action = "ACTION_AUTO_UPDATE"
        }
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val interval = 60 * 1000L
        alarmManager.setRepeating(AlarmManager.RTC, System.currentTimeMillis() + interval, interval, pendingIntent)
    }

    private fun stopSixtySecondLoop(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MatrixWidgetProvider::class.java).apply {
            action = "ACTION_AUTO_UPDATE"
        }
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarmManager.cancel(pendingIntent)
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val prefs = context.getSharedPreferences("WidgetPrefs", Context.MODE_PRIVATE)
        val savedValue = prefs.getString("widget_$appWidgetId", "Random (All)") ?: "Random (All)"
        val views = RemoteViews(context.packageName, R.layout.matrix_widget)

        // 🚀 Checks ALL games instead of just Minecraft!
        if (GameData.isMobValid(savedValue)) {
            val supportedDevices = GameData.getMasterTags(savedValue)
            val renderDevice = supportedDevices.first()

            val bitmap = generateWidgetBitmap(savedValue, renderDevice)
            views.setImageViewBitmap(R.id.widget_matrix_image, bitmap)
            views.setOnClickPendingIntent(R.id.widget_matrix_image, null)

        } else {
            val mobList = if (savedValue == "Random (All)") {
                GameData.getAllMobNames()
            } else {
                getMobsForCategory(savedValue)
            }

            val randomMob = mobList.random()
            val supportedDevices = GameData.getMasterTags(randomMob)
            val randomDevice = supportedDevices.random()

            val bitmap = generateWidgetBitmap(randomMob, randomDevice)
            views.setImageViewBitmap(R.id.widget_matrix_image, bitmap)

            val refreshIntent = Intent(context, MatrixWidgetProvider::class.java).apply {
                action = "ACTION_REFRESH_MATRIX"
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val pendingIntent = PendingIntent.getBroadcast(context, appWidgetId, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_matrix_image, pendingIntent)
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun getMobsForCategory(category: String): List<String> {
        GameData.ALL_GAMES.values.forEach { gameCategories ->
            if (gameCategories.containsKey(category)) {
                return gameCategories[category]!!
            }
        }
        return GameData.getAllMobNames()
    }

    private fun generateWidgetBitmap(mobName: String, deviceModel: String): Bitmap {
        val matrix = GameData.getMasterMatrixDesign(mobName, deviceModel)

        val gridSize = if (deviceModel == "4aPro") 13 else 25
        val activeWidths = if (deviceModel == "4aPro") PHONE_4A_PRO_ACTIVE_WIDTHS else PHONE_3_ACTIVE_WIDTHS

        val imageSize = 500f
        val bmp = Bitmap.createBitmap(imageSize.toInt(), imageSize.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val bgPaint = Paint().apply {
            color = Color.parseColor("#151515")
            isAntiAlias = true
        }
        canvas.drawRoundRect(RectF(0f, 0f, imageSize, imageSize), 80f, 80f, bgPaint)

        val paint = Paint().apply { isAntiAlias = true }
        val padding = 60f
        val visualGridSize = if (deviceModel == "4aPro") 13f else 25f
        val dotSize = (imageSize - (padding * 2)) / visualGridSize
        val xOffset = padding
        val yOffset = padding
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
                    paint.color = Color.argb(brightness, 255, 255, 255)
                } else {
                    paint.color = Color.rgb(35, 35, 35)
                }

                val left = (col * dotSize) + xOffset
                val top = (row * dotSize) + yOffset

                canvas.drawRoundRect(left + gap, top + gap, left + dotSize - gap, top + dotSize - gap, cornerRadius, cornerRadius, paint)
            }
        }
        return bmp
    }
}
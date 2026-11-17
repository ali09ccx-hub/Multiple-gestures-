package com.example.service

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.example.MainActivity

class GestureQuickTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        val active = GestureAccessibilityService.instance != null
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val service = GestureAccessibilityService.instance
        if (service != null) {
            service.showGesturePadOverlay()
        } else {
            Toast.makeText(
                applicationContext,
                "فعّل محرك الإيماءات من داخل التطبيق أولاً",
                Toast.LENGTH_SHORT
            ).show()
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}

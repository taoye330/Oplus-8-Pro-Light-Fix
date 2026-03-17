package com.shiyunjin.oplus.fuckfingerlight

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService


class TileService : TileService() {
    override fun onClick() {
        // 发送广播给 system_server 中的 Hook 代码
        val intent = Intent(ACTION_TOGGLE_LIGHT)
        sendBroadcast(intent)
    }

    companion object {
        const val ACTION_TOGGLE_LIGHT: String = "com.shiyunjin.oplus.fuckfingerlight.ACTION_TOGGLE_LIGHT"
    }
}
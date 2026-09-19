package com.qingning.cloudrest.tile

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.qingning.cloudrest.R
import com.qingning.cloudrest.audio.SoundEngine
import com.qingning.cloudrest.audio.SoundService
import com.qingning.cloudrest.audio.SoundType

/** 快捷设置磁贴：一键开关雨声 */
class RainTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        SoundEngine.init(applicationContext)
        SoundEngine.toggle(SoundType.RAIN)
        SoundService.update(applicationContext)
        refreshTile()
    }

    private fun refreshTile() {
        val on = SoundEngine.channelOn[SoundType.RAIN] == true
        qsTile?.apply {
            state = if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            icon = Icon.createWithResource(this@RainTileService, R.drawable.ic_tile_rain)
            label = "雨声"
            updateTile()
        }
    }
}
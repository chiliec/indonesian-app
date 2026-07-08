package com.axveer.lancar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.axveer.lancar.data.ContentRepository
import com.axveer.lancar.data.DriverFactory
import com.axveer.lancar.data.ProgressRepository
import com.axveer.lancar.data.SettingsRepository
import com.axveer.lancar.db.LancarDatabase
import com.axveer.lancar.platform.AndroidAudioPlayer
import com.axveer.lancar.ui.App
import com.axveer.lancar.ui.AppModule

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = LancarDatabase(DriverFactory(applicationContext).createDriver())
        val appModule = AppModule(
            content = ContentRepository(),
            progress = ProgressRepository(db),
            settings = SettingsRepository(db),
            audio = AndroidAudioPlayer(applicationContext),
        )
        setContent { App(appModule) }
    }
}

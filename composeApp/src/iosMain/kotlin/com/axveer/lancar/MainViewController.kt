package com.axveer.lancar

import androidx.compose.ui.window.ComposeUIViewController
import com.axveer.lancar.data.ContentRepository
import com.axveer.lancar.data.DriverFactory
import com.axveer.lancar.data.ProgressRepository
import com.axveer.lancar.db.LancarDatabase
import com.axveer.lancar.platform.IosAudioPlayer
import com.axveer.lancar.ui.App
import com.axveer.lancar.ui.AppModule

fun MainViewController() = ComposeUIViewController {
    val db = LancarDatabase(DriverFactory().createDriver())
    val appModule = AppModule(
        content = ContentRepository(),
        progress = ProgressRepository(db),
        audio = IosAudioPlayer(),
    )
    App(appModule)
}

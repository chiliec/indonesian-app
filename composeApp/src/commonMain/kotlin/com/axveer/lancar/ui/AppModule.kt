package com.axveer.lancar.ui

import com.axveer.lancar.data.ContentRepository
import com.axveer.lancar.data.ProgressRepository
import com.axveer.lancar.domain.QuestionFactory
import com.axveer.lancar.platform.AudioPlayer

class AppModule(
    val content: ContentRepository,
    val progress: ProgressRepository,
    val audio: AudioPlayer,
) {
    val questionFactory = QuestionFactory()
}

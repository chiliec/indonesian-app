package com.axveer.lancar.data

import com.axveer.lancar.domain.Card
import com.axveer.lancar.domain.ModuleMeta
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import lancar.composeapp.generated.resources.Res

@Serializable private data class ManifestEntry(val id: String, val title: String, val cardCount: Int)
@Serializable private data class Manifest(val modules: List<ManifestEntry>)

const val MIXED_ID = "mixed"

class ContentRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val cardCache = mutableMapOf<String, List<Card>>()
    private var metaCache: List<ModuleMeta>? = null

    suspend fun modules(): List<ModuleMeta> {
        metaCache?.let { return it }
        val text = Res.readBytes("files/content/manifest.json").decodeToString()
        val manifest = json.decodeFromString<Manifest>(text)
        val total = manifest.modules.sumOf { it.cardCount }
        val list = buildList {
            add(ModuleMeta(MIXED_ID, "🎲 Mixed (all words)", total))
            manifest.modules.forEach { add(ModuleMeta(it.id, it.title, it.cardCount)) }
        }
        metaCache = list
        return list
    }

    suspend fun cards(moduleId: String): List<Card> {
        cardCache[moduleId]?.let { return it }
        val result = if (moduleId == MIXED_ID) {
            modules().filter { it.id != MIXED_ID }.flatMap { cards(it.id) }
        } else {
            val text = Res.readBytes("files/content/$moduleId.json").decodeToString()
            json.decodeFromString<List<Card>>(text)
        }
        cardCache[moduleId] = result
        return result
    }
}

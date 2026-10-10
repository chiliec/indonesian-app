package cx.viz.lancar.data

import cx.viz.lancar.domain.Card
import cx.viz.lancar.domain.Lesson
import cx.viz.lancar.domain.ModuleMeta
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import lancar.composeapp.generated.resources.Res

@Serializable private data class ManifestEntry(val id: String, val title: String, val cardCount: Int)
@Serializable private data class Manifest(val modules: List<ManifestEntry>)

@Serializable private data class CourseFile(val lessons: List<Lesson>)

// Separate Json instance: the default discriminator "type" collides with nothing here,
// but it is set explicitly so course.json stays readable and K/N never guesses.
private val courseJson = Json {
    ignoreUnknownKeys = true
    useAlternativeNames = false
    classDiscriminator = "type"
}

internal fun parseCourse(text: String): List<Lesson> =
    courseJson.decodeFromString(CourseFile.serializer(), text).lessons

const val MIXED_ID = "mixed"

open class ContentRepository {
    private val json = Json { ignoreUnknownKeys = true; useAlternativeNames = false }
    private val cacheMutex = Mutex()
    private val cardCache = mutableMapOf<String, List<Card>>()
    private var metaCache: List<ModuleMeta>? = null
    private var courseCache: List<Lesson>? = null

    open suspend fun modules(): List<ModuleMeta> {
        cacheMutex.withLock { metaCache }?.let { return it }
        val text = Res.readBytes("files/content/manifest.json").decodeToString()
        val manifest = json.decodeFromString<Manifest>(text)
        val total = manifest.modules.sumOf { it.cardCount }
        val list = buildList {
            add(ModuleMeta(MIXED_ID, "🎲 Mixed (all words)", total))
            manifest.modules.forEach { add(ModuleMeta(it.id, it.title, it.cardCount)) }
        }
        cacheMutex.withLock { metaCache = list }
        return list
    }

    open suspend fun cards(moduleId: String): List<Card> {
        cacheMutex.withLock { cardCache[moduleId] }?.let { return it }
        val result = if (moduleId == MIXED_ID) {
            modules().filter { it.id != MIXED_ID }.flatMap { cards(it.id) }
        } else {
            val text = Res.readBytes("files/content/$moduleId.json").decodeToString()
            json.decodeFromString(ListSerializer(Card.serializer()), text)
        }
        cacheMutex.withLock { cardCache[moduleId] = result }
        return result
    }

    open suspend fun course(): List<Lesson> {
        cacheMutex.withLock { courseCache }?.let { return it }
        val list = parseCourse(Res.readBytes("files/content/course.json").decodeToString())
        cacheMutex.withLock { courseCache = list }
        return list
    }
}

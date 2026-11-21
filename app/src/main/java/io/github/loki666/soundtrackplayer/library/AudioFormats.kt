package io.github.loki666.soundtrackplayer.library

/** Which audio format to keep when a soundtrack ships several (e.g. MP3 + WAV, MP3 + FLAC). */
object AudioFormats {
    /** Best quality first: lossless, then lossy, MP3 last. */
    val PREFERENCE = listOf("flac", "wav", "ogg", "opus", "m4a", "aac", "mp3")

    fun extensionOf(path: String): String = path.substringAfterLast('/').substringAfterLast('\\')
        .substringAfterLast('.', "").lowercase()

    fun isAudio(path: String) = extensionOf(path) in PREFERENCE

    /** The preferred audio format among these file names, or null if none is audio. */
    fun best(paths: Collection<String>): String? {
        val present = paths.mapTo(HashSet()) { extensionOf(it) }
        return PREFERENCE.firstOrNull { it in present }
    }

    /**
     * Full-match patterns selecting the files worth downloading: audio in [format] only, plus every
     * non-audio file (cover art, booklets, liner notes).
     */
    fun downloadFilter(format: String): List<Regex> {
        val others = PREFERENCE.joinToString("|")
        return listOf(
            Regex(""".*\.${Regex.escape(format)}""", RegexOption.IGNORE_CASE),
            Regex("""(?!.*\.(?:$others)$).*""", RegexOption.IGNORE_CASE),
        )
    }
}

package io.github.loki666.soundtrackplayer.library

/** A file listed in one of an app's depot manifests. */
data class ManifestFile(val path: String, val bytes: Long)

/** The user's default when an album ships several audio formats. */
enum class DownloadQuality { Best, Smaller }

/** One way to download an album: an audio format and its total download size, extras included. */
data class FormatOption(val format: String, val bytes: Long) {
    val lossless: Boolean get() = format in LOSSLESS
    val label: String get() = format.uppercase()

    private companion object {
        val LOSSLESS = setOf("flac", "wav")
    }
}

object FormatOptions {
    /**
     * One option per audio format found in the manifests, best quality first. Each option's size
     * counts its own audio files plus every non-audio file (cover art, booklets), which are always
     * downloaded.
     */
    fun from(files: List<ManifestFile>): List<FormatOption> {
        val extras = files.filterNot { AudioFormats.isAudio(it.path) }.sumOf { it.bytes }
        return files.filter { AudioFormats.isAudio(it.path) }
            .groupBy { AudioFormats.extensionOf(it.path) }
            .map { (format, audio) -> FormatOption(format, audio.sumOf { it.bytes } + extras) }
            .sortedBy { AudioFormats.PREFERENCE.indexOf(it.format) }
    }

    fun preferred(options: List<FormatOption>, quality: DownloadQuality): FormatOption? = when (quality) {
        DownloadQuality.Best -> options.firstOrNull()
        DownloadQuality.Smaller -> options.minByOrNull { it.bytes }
    }
}

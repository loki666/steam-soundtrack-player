package io.github.loki666.soundtrackplayer.library

import android.media.MediaMetadataRetriever
import java.io.File

/** Turns a downloaded depot folder into an ordered track list. */
object TrackScanner {
    private val COVER_NAMES = listOf("cover", "folder", "front", "album", "artwork")

    /**
     * [downloadedFormat] is the format just downloaded; it wins over files of another format left
     * in [dir] (e.g. a cancelled WAV download before an MP3 one).
     */
    fun scan(appId: Int, dir: File, downloadedFormat: String? = null): LocalAlbum? {
        val files = dir.walkTopDown()
            .onEnter { !it.name.startsWith(".") }
            .filter { it.isFile }
            .toList()

        val audioByFormat = files.groupBy { it.extension.lowercase() }.filterKeys { it in AudioFormats.PREFERENCE }
        val format = chooseFormat(audioByFormat.keys, downloadedFormat) ?: return null

        val tracks = audioByFormat.getValue(format)
            .map { readTrack(it) }
            .sortedWith(compareBy<LocalTrack>({ it.disc }, { it.number }).thenBy(NaturalOrder) { it.path })

        val images = files.filter { it.extension.lowercase() in setOf("jpg", "jpeg", "png", "webp") }
        val cover = images.firstOrNull { img -> COVER_NAMES.any { img.nameWithoutExtension.lowercase().contains(it) } }
            ?: images.maxByOrNull { it.length() }

        return LocalAlbum(appId = appId, format = format.uppercase(), coverPath = cover?.absolutePath, tracks = tracks)
    }

    /** The format to keep among those on disk: the one just downloaded if present, else the best. */
    fun chooseFormat(available: Set<String>, downloaded: String?): String? =
        downloaded?.lowercase()?.takeIf { it in available }
            ?: AudioFormats.PREFERENCE.firstOrNull { it in available }

    /** Deletes audio in formats other than the one kept, e.g. the MP3 copy of a WAV album. */
    fun deleteOtherFormats(dir: File, keptFormat: String) {
        dir.walkTopDown()
            .onEnter { !it.name.startsWith(".") }
            .filter { it.isFile && AudioFormats.isAudio(it.name) && it.extension.lowercase() != keptFormat.lowercase() }
            .forEach { it.delete() }
    }

    private fun readTrack(file: File): LocalTrack {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            fun meta(key: Int) = retriever.extractMetadata(key)?.takeIf { it.isNotBlank() }
            LocalTrack(
                path = file.absolutePath,
                title = meta(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: titleFromFileName(file),
                disc = meta(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER).leadingInt() ?: 1,
                number = meta(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER).leadingInt()
                    ?: file.nameWithoutExtension.leadingInt() ?: 0,
                durationMs = meta(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L,
            )
        } catch (e: Exception) {
            LocalTrack(file.absolutePath, titleFromFileName(file), 1, file.nameWithoutExtension.leadingInt() ?: 0, 0L)
        } finally {
            retriever.release()
        }
    }

    /** "03 - Lanterns on the Water.flac" → "Lanterns on the Water" */
    private fun titleFromFileName(file: File) =
        file.nameWithoutExtension.replace(Regex("""^\s*\d+\s*[-._)]?\s*"""), "").ifBlank { file.nameWithoutExtension }

    /** Handles "3", "03" and "3/18". */
    private fun String?.leadingInt(): Int? = this?.let { Regex("""^\s*(\d+)""").find(it)?.groupValues?.get(1)?.toIntOrNull() }

    private object NaturalOrder : Comparator<String> {
        private val chunk = Regex("""\d+|\D+""")
        override fun compare(a: String, b: String): Int {
            val x = chunk.findAll(a).map { it.value }.toList()
            val y = chunk.findAll(b).map { it.value }.toList()
            for (i in 0 until minOf(x.size, y.size)) {
                val (p, q) = x[i] to y[i]
                val c = if (p[0].isDigit() && q[0].isDigit()) {
                    p.toBigInteger().compareTo(q.toBigInteger())
                } else {
                    p.compareTo(q, ignoreCase = true)
                }
                if (c != 0) return c
            }
            return x.size - y.size
        }
    }
}

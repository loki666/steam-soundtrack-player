package io.github.loki666.soundtrackplayer.download

import android.util.Log
import io.github.loki666.soundtrackplayer.library.ManifestFile
import `in`.dragonbra.javasteam.depotdownloader.DepotDownloader
import java.io.File

/** Helpers around JavaSteam's DepotDownloader for picking which files of a soundtrack to fetch. */
internal object DepotFiles {
    private const val TAG = "DepotFiles"
    private val MANIFEST_LINE = Regex("""^\s*(\d+)\s+\d+\s+[0-9a-f]+\s+[0-9a-f]+\s+(.+)$""")

    /**
     * Files (name and size) listed in the `manifest_<depot>_<id>.txt` files that a manifest-only
     * pass writes into [dir]. The dumps are deleted once read.
     */
    fun readManifestDumps(dir: File): List<ManifestFile> {
        val dumps = dir.listFiles { f -> f.isFile && f.name.startsWith("manifest_") && f.name.endsWith(".txt") }.orEmpty()
        return dumps.flatMap { dump ->
            val files = parseManifestDump(dump.readLines())
            dump.delete()
            files
        }
    }

    fun parseManifestDump(lines: List<String>): List<ManifestFile> = lines.mapNotNull { line ->
        val match = MANIFEST_LINE.find(line) ?: return@mapNotNull null
        ManifestFile(path = match.groupValues[2].trim(), bytes = match.groupValues[1].toLong())
    }

    /**
     * Turns on DepotDownloader's per-file filter, which exists internally but isn't exposed in its
     * public API yet (its `Config.usingFileList` / `filesToDownloadRegex`). Must be called before
     * adding items. Returns false if JavaSteam's internals changed; the caller then downloads
     * everything and cleans up afterwards.
     */
    fun enableFileFilter(downloader: DepotDownloader, patterns: List<Regex>): Boolean = try {
        val configField = DepotDownloader::class.java.getDeclaredField("config").apply { isAccessible = true }
        val config = configField.get(downloader) ?: error("config is null")
        config.javaClass.getDeclaredField("usingFileList").apply { isAccessible = true }.setBoolean(config, true)
        config.javaClass.getDeclaredField("filesToDownloadRegex").apply { isAccessible = true }.set(config, patterns)
        true
    } catch (e: Exception) {
        Log.w(TAG, "Couldn't enable DepotDownloader's file filter; downloading all files", e)
        false
    }
}

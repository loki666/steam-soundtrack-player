package io.github.loki666.soundtrackplayer.data

import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

class SessionExpiredException : IOException("Your Steam session has expired. Please sign in again.")

class SteamApi(
    private val http: OkHttpClient = OkHttpClient(),
    private val json: Json = DefaultJson,
) {
    /** All app ids on the account (games, DLC, soundtracks…), from the store's user data. */
    suspend fun ownedAppIds(): List<Int> = withContext(Dispatchers.IO) {
        val cookies = SteamSession.storeCookies() ?: throw SessionExpiredException()
        val request = Request.Builder()
            .url("${SteamSession.STORE_URL}/dynamicstore/userdata/?t=${System.currentTimeMillis()}")
            .header("Cookie", cookies)
            .build()
        val body = http.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("Steam store returned HTTP ${resp.code}")
            resp.body!!.string()
        }
        val data = json.decodeFromString<UserData>(body)
        // Without a valid session Steam answers 200 with empty lists.
        if (data.ownedApps.isEmpty() && data.ownedPackages.isEmpty()) throw SessionExpiredException()
        data.ownedApps
    }

    /** Looks the given apps up in the store catalog and keeps only soundtracks. */
    suspend fun soundtracks(appIds: List<Int>): List<Album> = coroutineScope {
        val gate = Semaphore(4)
        appIds.distinct().chunked(BATCH_SIZE)
            .map { batch -> async { gate.withPermit { storeItems(batch) } } }
            .awaitAll()
            .flatten()
            .filter { it.type == STORE_APP_TYPE_MUSIC && it.appid != null }
            .map { it.toAlbum() }
            .sortedBy { it.name.lowercase() }
    }

    private suspend fun storeItems(appIds: List<Int>): List<StoreItem> = withContext(Dispatchers.IO) {
        val input = buildJsonObject {
            putJsonArray("ids") { appIds.forEach { id -> addJsonObject { put("appid", id) } } }
            putJsonObject("context") {
                put("language", "english")
                put("country_code", "US")
                put("steam_realm", 1)
            }
            putJsonObject("data_request") {
                put("include_assets", true)
                put("include_basic_info", true)
            }
        }
        val url = "https://api.steampowered.com/IStoreBrowseService/GetItems/v1/".toHttpUrl()
            .newBuilder()
            .addQueryParameter("input_json", input.toString())
            .build()
        val body = http.newCall(Request.Builder().url(url).build()).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("Steam API returned HTTP ${resp.code}")
            resp.body!!.string()
        }
        json.decodeFromString<GetItemsResponse>(body).response.storeItems
    }

    private fun StoreItem.toAlbum(): Album {
        val id = appid!!
        val file = assets?.header ?: assets?.mainCapsule
        val path = if (assets?.urlFormat != null && file != null) {
            assets.urlFormat.replace("\${FILENAME}", file)
        } else {
            "steam/apps/$id/header.jpg"
        }
        return Album(
            appId = id,
            name = name ?: "App $id",
            artist = basicInfo?.developers?.firstNotNullOfOrNull { it.name }
                ?: basicInfo?.publishers?.firstNotNullOfOrNull { it.name },
            imageUrl = ASSET_HOST + path,
            parentAppId = related?.parentAppId,
        )
    }

    companion object {
        private const val BATCH_SIZE = 100
        /** EStoreAppType.Music */
        private const val STORE_APP_TYPE_MUSIC = 11
        private const val ASSET_HOST = "https://shared.akamai.steamstatic.com/store_item_assets/"

        val DefaultJson = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
    }
}

@Serializable
private data class UserData(
    @SerialName("rgOwnedApps") val ownedApps: List<Int> = emptyList(),
    @SerialName("rgOwnedPackages") val ownedPackages: List<Int> = emptyList(),
)

@Serializable
private data class GetItemsResponse(val response: Inner = Inner()) {
    @Serializable
    data class Inner(@SerialName("store_items") val storeItems: List<StoreItem> = emptyList())
}

@Serializable
private data class StoreItem(
    val appid: Int? = null,
    val name: String? = null,
    val type: Int? = null,
    val assets: Assets? = null,
    @SerialName("basic_info") val basicInfo: BasicInfo? = null,
    @SerialName("related_items") val related: RelatedItems? = null,
)

@Serializable
private data class Assets(
    @SerialName("asset_url_format") val urlFormat: String? = null,
    val header: String? = null,
    @SerialName("main_capsule") val mainCapsule: String? = null,
)

@Serializable
private data class BasicInfo(
    val developers: List<Named>? = null,
    val publishers: List<Named>? = null,
)

@Serializable
private data class Named(val name: String? = null)

@Serializable
private data class RelatedItems(@SerialName("parent_appid") val parentAppId: Int? = null)

package com.example.data.service

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Service that fetches actor and cast photos exclusively from Wikimedia Commons API.
 * Results (both found URLs and empty/not-found statuses) are permanently cached locally
 * so each actor is requested at most once.
 */
object WikimediaActorService {
    private const val TAG = "WikimediaActorService"
    private const val PREFS_NAME = "tivions_wikimedia_actors"
    private const val USER_AGENT = "TivionsIPTV/1.0 (Android; support@tivions.com)"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    // Fast in-memory cache to avoid disk reads during lazy grid scrolling
    private val memoryCache = ConcurrentHashMap<String, String>()

    suspend fun getActorPhotoUrl(context: Context, rawActorName: String): String? = withContext(Dispatchers.IO) {
        val cleanName = normalizeActorName(rawActorName)
        if (cleanName.isBlank()) return@withContext null

        val cacheKey = "actor_" + cleanName.lowercase().replace(Regex("[^a-z0-9]"), "_")

        // 1. Check in-memory cache
        memoryCache[cacheKey]?.let { cached ->
            return@withContext cached.ifBlank { null }
        }

        // 2. Check persistent SharedPreferences cache
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.contains(cacheKey)) {
            val stored = prefs.getString(cacheKey, "") ?: ""
            memoryCache[cacheKey] = stored
            return@withContext stored.ifBlank { null }
        }

        // 3. Query Wikimedia Commons API
        try {
            val encodedQuery = URLEncoder.encode(cleanName, "UTF-8")
            val url = "https://commons.wikimedia.org/w/api.php?action=query&generator=search&gsrnamespace=6&gsrsearch=$encodedQuery&gsrlimit=1&prop=imageinfo&iiprop=url&iiurlwidth=300&format=json"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    // Mark as empty in cache on failure so we don't spam
                    prefs.edit().putString(cacheKey, "").apply()
                    memoryCache[cacheKey] = ""
                    return@withContext null
                }

                val bodyString = response.body?.string()
                if (bodyString.isNullOrBlank()) {
                    prefs.edit().putString(cacheKey, "").apply()
                    memoryCache[cacheKey] = ""
                    return@withContext null
                }

                val root = JSONObject(bodyString)
                val queryObj = root.optJSONObject("query")
                val pagesObj = queryObj?.optJSONObject("pages")

                var photoUrl: String? = null
                if (pagesObj != null) {
                    val keys = pagesObj.keys()
                    if (keys.hasNext()) {
                        val pageKey = keys.next()
                        val page = pagesObj.optJSONObject(pageKey)
                        val imageInfoArray = page?.optJSONArray("imageinfo")
                        if (imageInfoArray != null && imageInfoArray.length() > 0) {
                            val firstInfo = imageInfoArray.optJSONObject(0)
                            photoUrl = firstInfo?.optString("thumburl")?.takeIf { it.isNotBlank() }
                                ?: firstInfo?.optString("url")?.takeIf { it.isNotBlank() }
                        }
                    }
                }

                val finalUrl = photoUrl ?: ""
                prefs.edit().putString(cacheKey, finalUrl).apply()
                memoryCache[cacheKey] = finalUrl

                return@withContext photoUrl
            }
        } catch (e: Exception) {
            Log.w(TAG, "Wikimedia photo fetch error for $cleanName: ${e.message}")
            prefs.edit().putString(cacheKey, "").apply()
            memoryCache[cacheKey] = ""
            return@withContext null
        }
    }

    private fun normalizeActorName(name: String): String {
        return name
            .substringBefore(" as ")
            .substringBefore("(")
            .trim()
    }
}

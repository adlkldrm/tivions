package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

/**
 * Custom Application class that sets up high-performance Coil image caching
 * with dedicated in-memory and on-disk caches for IPTV logos and movie posters.
 */
class TivionsApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // Use up to 25% of app memory for logo/poster cache
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(64L * 1024 * 1024) // 64 MB disk cache
                    .build()
            }
            .crossfade(true)
            .respectCacheHeaders(false) // Always cache IPTV channel logos even if headers are missing
            .build()
    }
}

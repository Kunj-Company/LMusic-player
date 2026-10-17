package com.twilighty.lmusic

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.os.Build
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.twilighty.lmusic.data.preferences.UserPreferencesRepository
import com.twilighty.lmusic.data.repository.ArtistImageRepository
import com.twilighty.lmusic.data.telegram.TelegramRepository
import com.twilighty.lmusic.presentation.viewmodel.LibraryStateHolder
import com.twilighty.lmusic.presentation.viewmodel.ThemeStateHolder
import com.twilighty.lmusic.utils.AlbumArtCacheManager
import com.twilighty.lmusic.utils.AlbumArtUtils
import com.twilighty.lmusic.utils.CrashHandler
import com.twilighty.lmusic.utils.AppLocaleManager
import com.twilighty.lmusic.utils.MediaMetadataRetrieverPool
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class LMusicApplication : Application(), ImageLoaderFactory, Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var imageLoader: dagger.Lazy<ImageLoader>

    @Inject
    lateinit var telegramCoilFetcherFactory: dagger.Lazy<com.twilighty.lmusic.data.image.TelegramCoilFetcher.Factory>

    @Inject
    lateinit var navidromeCoilFetcherFactory: dagger.Lazy<com.twilighty.lmusic.data.image.NavidromeCoilFetcher.Factory>

    @Inject
    lateinit var jellyfinCoilFetcherFactory: dagger.Lazy<com.twilighty.lmusic.data.image.JellyfinCoilFetcher.Factory>

    @Inject
    lateinit var localArtworkCoilFetcherFactory: dagger.Lazy<com.twilighty.lmusic.data.image.LocalArtworkCoilFetcher.Factory>

    @Inject
    lateinit var themeStateHolder: dagger.Lazy<ThemeStateHolder>

    @Inject
    lateinit var artistImageRepository: dagger.Lazy<ArtistImageRepository>

    @Inject
    lateinit var telegramRepository: dagger.Lazy<TelegramRepository>

    @Inject
    lateinit var libraryStateHolder: dagger.Lazy<LibraryStateHolder>

    @Inject
    lateinit var userPreferencesRepository: dagger.Lazy<UserPreferencesRepository>

    @Inject
    lateinit var updatePreferences: dagger.Lazy<com.twilighty.lmusic.data.update.UpdatePreferences>

    @Inject
    lateinit var updateScheduler: dagger.Lazy<com.twilighty.lmusic.data.update.UpdateScheduler>

    @Inject
    lateinit var musicFoldersRepository: dagger.Lazy<com.twilighty.lmusic.data.library.MusicFoldersRepository>

    @Inject
    lateinit var smartShuffleController: dagger.Lazy<com.twilighty.lmusic.data.playback.SmartShuffleController>

    private val startupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "lmusic_playback"
    }

    private val appLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            libraryStateHolder.get().restoreAfterTrimIfNeeded()
        }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLocaleManager.wrapContext(base))
    }

    override fun onCreate() {
        super.onCreate()

        // Benchmark variant intentionally restarts/kills app process during tests.
        // Avoid persisting those events as user-facing crash reports.
        if (BuildConfig.BUILD_TYPE != "benchmark") {
            CrashHandler.install(this)
        }

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            // Release tree: only WARN/ERROR/WTF - no DEBUG/VERBOSE/INFO
            Timber.plant(ReleaseTree())
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            // Channels from before the LMusic rename would otherwise linger in system settings.
            listOf("pixelplay_music_channel", "pixelplay_cast_server", "pixelplay_watch_transfers").forEach { legacyId ->
                runCatching { notificationManager.deleteNotificationChannel(legacyId) }
            }
        }

        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)

        startupScope.launch {
            AlbumArtUtils.migrateLegacyCacheLocation(this@LMusicApplication)
            val savedLimit = runCatching {
                userPreferencesRepository.get().albumArtCacheLimitMbFlow.first()
            }.getOrNull()
            if (savedLimit != null) {
                AlbumArtCacheManager.configuredCacheLimitMb = savedLimit.toLong()
            }
        }

        startupScope.launch {
            // One-time move from "excluded folders" to the "music folders" include list.
            musicFoldersRepository.get().migrateIfNeeded()
        }

        // Smart shuffle setting -> shuffle order used by every shuffle path (QueueUtils).
        smartShuffleController.get().start(startupScope)

        if (BuildConfig.BUILD_TYPE != "benchmark") {
            startupScope.launch {
                // Weekly update check (KEEP keeps the existing schedule); cancelled when switched off.
                val enabled = runCatching { updatePreferences.get().checksEnabled.first() }.getOrDefault(true)
                runCatching { updateScheduler.get().apply(enabled) }
            }
        }
    }

    override fun newImageLoader(): ImageLoader {
        return imageLoader.get().newBuilder()
            .components {
                add(localArtworkCoilFetcherFactory.get())
                add(telegramCoilFetcherFactory.get())
                add(navidromeCoilFetcherFactory.get())
                add(jellyfinCoilFetcherFactory.get())
            }
            .build()
    }

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)

        imageLoader.get().memoryCache?.trimMemory(level)

        if (
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE ||
            level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND ||
            level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
        ) {
            themeStateHolder.get().trimMemory(level)
        }

        if (
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW ||
            level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND ||
            level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
        ) {
            artistImageRepository.get().clearCache()
            telegramRepository.get().clearMemoryCache()
            MediaMetadataRetrieverPool.clear()
        }

        libraryStateHolder.get().trimMemory(level)

        if (
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL ||
            level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE
        ) {
            imageLoader.get().memoryCache?.clear()
        }
    }

    // 3. Sobrescribe el método para proveer la configuración de WorkManager
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

}

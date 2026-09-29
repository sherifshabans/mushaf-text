package io.github.sherifshabans.mushaf.sample

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import io.github.sherifshabans.mushaf.sample.audio.QuranAudioPrefs
import io.github.sherifshabans.mushaf.sample.audio.RecitersRepository
import io.github.sherifshabans.mushaf.sample.audio.download.QuranAudioDownloader
import io.github.sherifshabans.mushaf.sample.audio.player.QuranPlayerController
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** One DataStore for the whole sample — the audio prefs and the ayah marks. */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mushaf_sample")

/**
 * The app wires these with Hilt; the sample has four singletons, so it builds
 * them by hand. Same lifetimes as the app's `@Singleton`s: one per process.
 */
object SampleGraph {

    private lateinit var app: Context

    val appContext: Context get() = app

    fun init(application: Application) {
        app = application
    }

    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    val audioPrefs: QuranAudioPrefs by lazy { QuranAudioPrefs(app) }
    val reciters: RecitersRepository by lazy { RecitersRepository(app, http) }
    val downloader: QuranAudioDownloader by lazy { QuranAudioDownloader(app, http) }
    val player: QuranPlayerController by lazy { QuranPlayerController(app) }
}

class SampleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SampleGraph.init(this)
    }
}

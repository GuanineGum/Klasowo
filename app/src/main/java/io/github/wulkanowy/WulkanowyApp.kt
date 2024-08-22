package io.github.guanine

import android.app.Application
import android.util.Log.DEBUG
import android.util.Log.INFO
import android.util.Log.VERBOSE
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.yariksoffice.lingver.Lingver
import dagger.hilt.android.HiltAndroidApp
import fr.bipi.treessence.file.FileLoggerTree
import io.github.guanine.data.repositories.PreferencesRepository
import io.github.guanine.ui.base.ThemeManager
import io.github.guanine.utils.ActivityLifecycleLogger
import io.github.guanine.utils.AnalyticsHelper
import io.github.guanine.utils.AppInfo
import io.github.guanine.utils.CrashLogExceptionTree
import io.github.guanine.utils.CrashLogTree
import io.github.guanine.utils.DebugLogTree
import io.github.guanine.utils.RemoteConfigHelper
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class WulkanowyApp : Application(), Configuration.Provider {

    @Inject
    lateinit var themeManager: ThemeManager

    @Inject
    lateinit var appInfo: AppInfo

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    @Inject
    lateinit var analyticsHelper: AnalyticsHelper

    @Inject
    lateinit var remoteConfigHelper: RemoteConfigHelper

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (appInfo.isDebug) VERBOSE else INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        initializeAppLanguage()
        themeManager.applyDefaultTheme()
        remoteConfigHelper.initialize()
        initLogging()
    }

    private fun initLogging() {
        if (appInfo.isDebug) {
            Timber.plant(DebugLogTree())
            Timber.plant(
                FileLoggerTree.Builder()
                    .withFileName("wulkanowy.%g.log")
                    .withDirName(applicationContext.filesDir.absolutePath)
                    .withFileLimit(10)
                    .withMinPriority(DEBUG)
                    .build()
            )
        } else {
            Timber.plant(CrashLogExceptionTree())
            Timber.plant(CrashLogTree())
        }
        registerActivityLifecycleCallbacks(ActivityLifecycleLogger())
    }

    private fun initializeAppLanguage() {
        Lingver.init(this)

        if (preferencesRepository.appLanguage == "system") {
            Lingver.getInstance().setFollowSystemLocale(this)
            analyticsHelper.logEvent("language", "startup" to appInfo.systemLanguage)
        } else {
            analyticsHelper.logEvent("language", "startup" to preferencesRepository.appLanguage)
        }
    }
}

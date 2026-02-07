package jp.co.screentime.slackreporter.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import jp.co.screentime.slackreporter.di.IoDispatcher
import jp.co.screentime.slackreporter.domain.model.App
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AppListRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun getAllApps(): List<App> = withContext(ioDispatcher) {
        val packageManager = context.packageManager
        val apps = packageManager.getInstalledApplications(0)
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 } // システムアプリを除外
            .map {
                App(
                    packageName = it.packageName,
                    appName = packageManager.getApplicationLabel(it).toString()
                )
            }
        apps.sortedBy { it.appName }
    }
}

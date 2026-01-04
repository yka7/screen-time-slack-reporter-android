package jp.co.screentime.slackreporter.platform

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * パッケージ名からアプリ名・アイコンを解決するヘルパー
 */
@Singleton
class AppLabelResolver @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val packageManager: PackageManager = context.packageManager

    // キャッシュ
    private val labelCache = mutableMapOf<String, String>()
    private val iconCache = mutableMapOf<String, Drawable?>()

    /**
     * パッケージ名からアプリ名（ラベル）を取得
     *
     * @param packageName パッケージ名
     * @return アプリ名（取得できない場合はパッケージ名）
     */
    @Synchronized
    fun getAppLabel(packageName: String): String {
        return labelCache.getOrPut(packageName) {
            try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (e: PackageManager.NameNotFoundException) {
                packageName
            }
        }
    }

    /**
     * パッケージ名からアプリアイコンを取得
     *
     * @param packageName パッケージ名
     * @return アプリアイコン（取得できない場合はnull）
     */
    @Synchronized
    fun getAppIcon(packageName: String): Drawable? {
        return iconCache.getOrPut(packageName) {
            try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                packageManager.getApplicationIcon(appInfo)
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }
        }
    }

    /**
     * キャッシュをクリア
     */
    @Synchronized
    fun clearCache() {
        labelCache.clear()
        iconCache.clear()
    }
}

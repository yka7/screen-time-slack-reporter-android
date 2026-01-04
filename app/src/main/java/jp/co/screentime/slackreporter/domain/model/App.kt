package jp.co.screentime.slackreporter.domain.model

import android.graphics.drawable.Drawable

data class App(
    val packageName: String,
    val appName: String,
    val icon: Drawable?
)

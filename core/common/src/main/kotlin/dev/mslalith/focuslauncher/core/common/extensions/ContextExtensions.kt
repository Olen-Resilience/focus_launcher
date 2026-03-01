package dev.mslalith.focuslauncher.core.common.extensions

import android.annotation.SuppressLint
import android.app.StatusBarManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import dev.mslalith.focuslauncher.core.lint.kover.IgnoreInKoverReport
import dev.mslalith.focuslauncher.core.model.app.App
import java.lang.reflect.Method

@IgnoreInKoverReport
@SuppressLint("WrongConstant")
fun Context.openNotificationShade() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // ✅ FIX: Use the public StatusBarManager API on API 31+
        // This is the correct, non-reflection, non-blocked approach
        val statusBarManager = getSystemService(StatusBarManager::class.java)
        statusBarManager.expandNotificationsPanel()
    } else {
        // Reflection still works on API 28-30 but can be blocked on some OEMs.
        // Wrapping in try-catch prevents a hard crash.
        try {
            val statusBarService = getSystemService("statusbar")
            val statusBarManager: Class<*> = Class.forName("android.app.StatusBarManager")
            val method: Method = statusBarManager.getMethod("expandNotificationsPanel")
            method.invoke(statusBarService)
        } catch (_: Exception) {
            // Silently fail — notification shade is a non-critical UX feature
        }
    }
}

@IgnoreInKoverReport
fun Context.launchApp(app: App) {
    packageManager.getLaunchIntentForPackage(app.packageName)?.let {
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        startActivity(it)
    }
}

@IgnoreInKoverReport
fun Context.showAppInfo(packageName: String) {
    with(Intent()) {
        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        data = Uri.fromParts("package", packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(this)
    }
}

@IgnoreInKoverReport
fun Context.uninstallApp(app: App) {
    with(Intent(Intent.ACTION_DELETE)) {
        data = Uri.parse("package:${app.packageName}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(this)
    }
}
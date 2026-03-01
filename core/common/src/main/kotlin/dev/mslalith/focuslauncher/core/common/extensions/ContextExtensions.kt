package dev.mslalith.focuslauncher.core.common.extensions

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import dev.mslalith.focuslauncher.core.lint.kover.IgnoreInKoverReport
import dev.mslalith.focuslauncher.core.model.app.App
import java.lang.reflect.Method

@IgnoreInKoverReport
@SuppressLint("WrongConstant")
fun Context.openNotificationShade() {
    // StatusBarManager.expandNotificationsPanel() is @SystemApi — not in the public SDK.
    // Reflection is the correct cross-version approach. Wrapped in try-catch so it
    // never crashes the app if the OEM has blocked it.
    try {
        val statusBarService = getSystemService("statusbar")
        val statusBarManager: Class<*> = Class.forName("android.app.StatusBarManager")
        val method: Method = statusBarManager.getMethod("expandNotificationsPanel")
        method.invoke(statusBarService)
    } catch (_: Exception) {
        // Silently fail — notification shade is a non-critical UX feature
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
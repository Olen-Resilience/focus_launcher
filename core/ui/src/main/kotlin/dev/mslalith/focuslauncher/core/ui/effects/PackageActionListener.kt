package dev.mslalith.focuslauncher.core.ui.effects

import android.content.pm.LauncherApps
import android.os.UserHandle
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import dev.mslalith.focuslauncher.core.model.PackageAction

@Composable
fun PackageActionListener(
    onAction: (PackageAction) -> Unit
) {
    val context = LocalContext.current
    val updatedOnAction by rememberUpdatedState(newValue = onAction)

    DisposableEffect(key1 = context, key2 = updatedOnAction) {
        val launcherApps = context.getSystemService(LauncherApps::class.java)
        val callback = object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String?, user: UserHandle?) {
                try {
                    packageName ?: return
                    updatedOnAction(PackageAction.Removed(packageName = packageName))
                } catch (e: Exception) {
                    Log.e("PackageActionListener", "Error in onPackageRemoved", e)
                }
            }

            override fun onPackageAdded(packageName: String?, user: UserHandle?) {
                try {
                    packageName ?: return
                    updatedOnAction(PackageAction.Added(packageName = packageName))
                } catch (e: Exception) {
                    Log.e("PackageActionListener", "Error in onPackageAdded", e)
                }
            }

            override fun onPackageChanged(packageName: String?, user: UserHandle?) {
                try {
                    packageName ?: return
                    updatedOnAction(PackageAction.Updated(packageName = packageName))
                } catch (e: Exception) {
                    Log.e("PackageActionListener", "Error in onPackageChanged", e)
                }
            }

            override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = Unit
            override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = Unit
        }

        try {
            launcherApps.registerCallback(callback)
        } catch (e: Exception) {
            Log.e("PackageActionListener", "Failed to register callback", e)
        }

        onDispose {
            try {
                launcherApps.unregisterCallback(callback)
            } catch (e: Exception) {
                Log.e("PackageActionListener", "Failed to unregister callback", e)
            }
        }
    }
}
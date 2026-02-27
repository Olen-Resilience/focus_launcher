package dev.mslalith.focuslauncher.core.domain.launcherapps

import dev.mslalith.focuslauncher.core.data.repository.AppDrawerRepo
import dev.mslalith.focuslauncher.core.launcherapps.manager.launcherapps.LauncherAppsManager
import javax.inject.Inject

class LoadAllAppsUseCase @Inject constructor(
    private val launcherAppsManager: LauncherAppsManager,
    private val appDrawerRepo: AppDrawerRepo
) {
    suspend operator fun invoke(forceLoad: Boolean = false) {
        val dbIsEmpty = appDrawerRepo.areAppsEmptyInDatabase()

        // First launch: DB is empty, load everything fresh
        if (dbIsEmpty) {
            val apps = launcherAppsManager.loadAllApps().map { it.app }
            appDrawerRepo.addApps(apps = apps)
            return
        }

        // Subsequent launches: lightweight background sync only.
        // DB already has apps so UI loads instantly from cache.
        if (forceLoad) {
            val systemApps = launcherAppsManager.loadAllApps()
            for (appWithComponent in systemApps) {
                val existing = appDrawerRepo.getAppBy(packageName = appWithComponent.app.packageName)
                if (existing == null) {
                    appDrawerRepo.addApp(app = appWithComponent.app)
                }
            }
        }
    }
}
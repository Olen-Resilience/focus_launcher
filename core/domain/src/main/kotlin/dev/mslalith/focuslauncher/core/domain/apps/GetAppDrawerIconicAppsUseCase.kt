package dev.mslalith.focuslauncher.core.domain.apps

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import dev.mslalith.focuslauncher.core.common.appcoroutinedispatcher.AppCoroutineDispatcher
import dev.mslalith.focuslauncher.core.data.repository.AppDrawerRepo
import dev.mslalith.focuslauncher.core.data.repository.FavoritesRepo
import dev.mslalith.focuslauncher.core.data.repository.HiddenAppsRepo
import dev.mslalith.focuslauncher.core.launcherapps.manager.launcherapps.LauncherAppsManager
import dev.mslalith.focuslauncher.core.launcherapps.providers.icons.IconProvider
import dev.mslalith.focuslauncher.core.model.IconPackType
import dev.mslalith.focuslauncher.core.model.app.App
import dev.mslalith.focuslauncher.core.model.appdrawer.AppDrawerItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

class GetAppDrawerIconicAppsUseCase @Inject internal constructor(
    private val launcherAppsManager: LauncherAppsManager,
    private val iconProvider: IconProvider,
    private val appDrawerRepo: AppDrawerRepo,
    private val hiddenAppsRepo: HiddenAppsRepo,
    private val favoritesRepo: FavoritesRepo,
    private val appCoroutineDispatcher: AppCoroutineDispatcher
) {
    private val iconCache = ConcurrentHashMap<String, Drawable>()
    private val iconVersionFlow = MutableStateFlow(0)
    private val iconScope = CoroutineScope(SupervisorJob() + appCoroutineDispatcher.io)

    operator fun invoke(searchQueryFlow: Flow<String>): Flow<List<AppDrawerItem>> {
        val visibleAppsFlow: Flow<List<App>> = appDrawerRepo.allAppsFlow
            .combine(hiddenAppsRepo.onlyHiddenAppsFlow) { all, hidden ->
                all - hidden.toSet()
            }
            .distinctUntilChanged()

        val allItemsFlow: Flow<List<AppDrawerItem>> = visibleAppsFlow
            .combine(favoritesRepo.onlyFavoritesFlow) { apps, favorites ->
                apps to favorites.map { it.packageName }.toHashSet()
            }
            .combine(iconVersionFlow) { (apps, favoriteSet), _ ->
                scheduleIconLoad(apps)
                apps.map { app ->
                    AppDrawerItem(
                        app = app,
                        isFavorite = app.packageName in favoriteSet,
                        icon = iconCache[app.packageName] ?: fallbackIcon(),
                        color = null
                    )
                }
            }
            .flowOn(appCoroutineDispatcher.io)

        return allItemsFlow
            .combine(searchQueryFlow.debounce(80L)) { items, query ->
                filterAndSort(items, query)
            }
            .flowOn(appCoroutineDispatcher.io)
    }

    fun onIconPackChanged() {
        iconCache.clear()
        iconVersionFlow.update { it + 1 }
    }

    private fun scheduleIconLoad(apps: List<App>) {
        val missing = apps.filter { !iconCache.containsKey(it.packageName) }
        if (missing.isEmpty()) return

        iconScope.launch {
            missing.chunked(20).forEach { chunk ->
                var loaded = false
                for (app in chunk) {
                    if (iconCache.containsKey(app.packageName)) continue
                    try {
                        val component = launcherAppsManager.loadApp(app.packageName) ?: continue
                        iconCache[app.packageName] = iconProvider.iconFor(
                            appWithComponent = component,
                            iconPackType = IconPackType.System
                        )
                        loaded = true
                    } catch (_: Exception) {
                        // Skip — fallback icon will be shown
                    }
                }
                if (loaded) iconVersionFlow.update { it + 1 }
            }
        }
    }

    private fun fallbackIcon(): Drawable = ColorDrawable(android.graphics.Color.TRANSPARENT)

    private fun filterAndSort(items: List<AppDrawerItem>, query: String): List<AppDrawerItem> {
        if (query.isBlank()) return items
        val q = query.trim().lowercase()
        return items
            .mapNotNull { item ->
                val lower = item.app.displayName.lowercase()
                if (lower.contains(q)) item to lower else null
            }
            .sortedWith(
                compareByDescending<Pair<AppDrawerItem, String>> { (_, lower) ->
                    lower.startsWith(q)
                }.thenBy { (_, lower) -> lower }
            )
            .map { (item, _) -> item }
    }
}
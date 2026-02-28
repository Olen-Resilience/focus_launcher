package dev.mslalith.focuslauncher.core.domain.apps

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import dev.mslalith.focuslauncher.core.common.appcoroutinedispatcher.AppCoroutineDispatcher
import dev.mslalith.focuslauncher.core.data.repository.AppDrawerRepo
import dev.mslalith.focuslauncher.core.data.repository.FavoritesRepo
import dev.mslalith.focuslauncher.core.data.repository.HiddenAppsRepo
import dev.mslalith.focuslauncher.core.launcherapps.manager.iconcache.IconCacheManager
import dev.mslalith.focuslauncher.core.launcherapps.manager.launcherapps.LauncherAppsManager
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
    private val iconCacheManager: IconCacheManager,
    private val appDrawerRepo: AppDrawerRepo,
    private val hiddenAppsRepo: HiddenAppsRepo,
    private val favoritesRepo: FavoritesRepo,
    private val appCoroutineDispatcher: AppCoroutineDispatcher
) {
    // In-memory icon cache: packageName -> Drawable
    private val iconCache = ConcurrentHashMap<String, Drawable>()

    // Bumped after each background icon batch so the list flow re-emits
    private val iconVersionFlow = MutableStateFlow(0)

    // Dedicated scope for fire-and-forget icon loading
    private val iconScope = CoroutineScope(SupervisorJob() + appCoroutineDispatcher.io)

    operator fun invoke(searchQueryFlow: Flow<String>): Flow<List<AppDrawerItem>> {
        // Visible apps = all apps minus hidden ones
        val visibleAppsFlow: Flow<List<App>> = appDrawerRepo.allAppsFlow
            .combine(hiddenAppsRepo.onlyHiddenAppsFlow) { all, hidden ->
                all - hidden.toSet()
            }
            .distinctUntilChanged()

        // Full list: emits immediately from Room, icons stream in via iconVersionFlow
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

        // Debounced search — pure in-memory filter, no IO
        return allItemsFlow
            .combine(searchQueryFlow.debounce(80L)) { items, query ->
                filterAndSort(items, query)
            }
            .flowOn(appCoroutineDispatcher.io)
    }

    /** Clears the icon cache when the icon pack changes and triggers a re-emit. */
    fun onIconPackChanged() {
        iconCache.clear()
        iconVersionFlow.update { it + 1 }
    }

    /**
     * Schedules background icon loading for apps not yet in the cache.
     * Returns immediately without blocking the calling coroutine.
     * Bumps iconVersionFlow after each chunk so on-screen icons appear fast.
     */
    private fun scheduleIconLoad(apps: List<App>) {
        val missing = apps.filter { !iconCache.containsKey(it.packageName) }
        if (missing.isEmpty()) return

        iconScope.launch {
            var loaded = false
            missing.chunked(20).forEach { chunk ->
                for (app in chunk) {
                    if (iconCache.containsKey(app.packageName)) continue
                    try {
                        val component = launcherAppsManager.loadApp(app.packageName) ?: continue
                        val drawable = iconCacheManager.iconFor(
                            appWithComponent = component,
                            iconPackType = IconPackType.System
                        )
                        iconCache[app.packageName] = drawable
                        loaded = true
                    } catch (_: Exception) {
                        // Skip unloadable icons — fallback will be used
                    }
                }
                if (loaded) {
                    iconVersionFlow.update { it + 1 }
                    loaded = false
                }
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
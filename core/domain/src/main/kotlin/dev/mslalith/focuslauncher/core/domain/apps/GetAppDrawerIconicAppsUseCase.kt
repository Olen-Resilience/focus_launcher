package dev.mslalith.focuslauncher.core.domain.apps

import dev.mslalith.focuslauncher.core.common.appcoroutinedispatcher.AppCoroutineDispatcher
import dev.mslalith.focuslauncher.core.data.repository.AppDrawerRepo
import dev.mslalith.focuslauncher.core.data.repository.FavoritesRepo
import dev.mslalith.focuslauncher.core.data.repository.HiddenAppsRepo
import dev.mslalith.focuslauncher.core.domain.apps.core.GetAppsIconPackAwareUseCase
import dev.mslalith.focuslauncher.core.domain.extensions.toFavoriteItem
import dev.mslalith.focuslauncher.core.model.appdrawer.AppDrawerItem
import dev.mslalith.focuslauncher.core.model.app.App
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject

/**
 * PERFORMANCE FIX: Previously the pipeline was:
 *   searchQuery → filter apps → load icons for filtered set → display
 *
 * Problem: every keystroke triggered icon loading for all visible apps,
 * which is extremely slow (each icon requires PackageManager lookup + bitmap decode).
 *
 * New pipeline:
 *   ALL apps → load icons ONCE → cache full AppDrawerItem list
 *   searchQuery → filter the already-loaded cache → display
 *
 * Icons load once at startup. Search/delete is now instant — it only
 * filters an in-memory list of pre-loaded items.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GetAppDrawerIconicAppsUseCase @Inject internal constructor(
    private val getAppsIconPackAwareUseCase: GetAppsIconPackAwareUseCase,
    private val appDrawerRepo: AppDrawerRepo,
    private val hiddenAppsRepo: HiddenAppsRepo,
    private val favoritesRepo: FavoritesRepo,
    private val appCoroutineDispatcher: AppCoroutineDispatcher
) {
    operator fun invoke(searchQueryFlow: Flow<String>): Flow<List<AppDrawerItem>> {

        // Step 1: Build the full icon-loaded cache from ALL visible apps.
        // This runs once (and re-runs only when apps are installed/uninstalled
        // or the icon pack changes). It never re-runs on search changes.
        val visibleAppsFlow: Flow<List<App>> = appDrawerRepo.allAppsFlow
            .combine(hiddenAppsRepo.onlyHiddenAppsFlow) { all, hidden ->
                all - hidden.toSet()
            }

        val allItemsWithIconsFlow: Flow<List<AppDrawerItem>> =
            getAppsIconPackAwareUseCase.appsWithIcons(appsFlow = visibleAppsFlow)
                .combine(favoritesRepo.onlyFavoritesFlow) { appsWithIcons, favorites ->
                    appsWithIcons.map { appWithIcon ->
                        appWithIcon.toFavoriteItem(
                            isFavorite = favorites.any { it.packageName == appWithIcon.app.packageName }
                        )
                    }
                }
                .flowOn(context = appCoroutineDispatcher.io)

        // Step 2: Debounce the query then filter the pre-loaded cache.
        // No icon loading happens here — just a fast in-memory filter + sort.
        val debouncedQuery = searchQueryFlow.debounce(timeoutMillis = 80L)

        return allItemsWithIconsFlow
            .combine(debouncedQuery) { allItems, query ->
                if (query.isBlank()) {
                    allItems
                } else {
                    val normalizedQuery = query.trim().lowercase()
                    allItems
                        .mapNotNull { item ->
                            val lower = item.app.displayName.lowercase()
                            if (lower.contains(normalizedQuery)) item to lower else null
                        }
                        .sortedWith(
                            compareByDescending<Pair<AppDrawerItem, String>> { (_, lower) ->
                                lower.startsWith(normalizedQuery)
                            }.thenBy { (_, lower) -> lower }
                        )
                        .map { (item, _) -> item }
                }
            }
            .flowOn(context = appCoroutineDispatcher.io)
    }
}

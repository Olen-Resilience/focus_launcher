package dev.mslalith.focuslauncher.feature.appdrawerpage.apps.grid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import dev.mslalith.focuslauncher.core.model.appdrawer.AppDrawerIconViewType
import dev.mslalith.focuslauncher.core.model.appdrawer.AppDrawerItem
import dev.mslalith.focuslauncher.core.ui.VerticalSpacer
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AppsGrid(
    apps: ImmutableList<AppDrawerItem>,
    appDrawerIconViewType: AppDrawerIconViewType,
    onAppClick: (AppDrawerItem) -> Unit,
    onAppLongClick: (AppDrawerItem) -> Unit
) {
    val columnCount = 4

    val configuration = LocalConfiguration.current
    val topSpacing = configuration.screenHeightDp.dp * 0.2f
    val bottomSpacing = configuration.screenHeightDp.dp * 0.05f

    LazyVerticalGrid(
        columns = GridCells.Fixed(count = columnCount),
        modifier = Modifier.padding(horizontal = 24.dp)
    ) {
        repeat(times = columnCount) {
            item { VerticalSpacer(spacing = topSpacing) }
        }

        items(
            items = apps,
            // Stable packageName-based key: LazyVerticalGrid can reuse
            // existing item compositions instead of recreating them on
            // every list update (e.g., search result changes).
            key = { it.app.packageName }
        ) { app ->
            // animateItemPlacement removed — it forces a layout pass on every
            // grid change including each search keystroke, which is the main
            // cause of scroll and search lag in the grid view.
            AppDrawerGridItem(
                appDrawerItem = app,
                appDrawerIconViewType = appDrawerIconViewType,
                onClick = onAppClick,
                onLongClick = onAppLongClick,
                modifier = Modifier
            )
        }

        repeat(times = columnCount) {
            item { VerticalSpacer(spacing = bottomSpacing) }
        }
    }
}

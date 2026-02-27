package dev.mslalith.focuslauncher.feature.appdrawerpage.apps.grid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import dev.mslalith.focuslauncher.core.model.appdrawer.AppDrawerIconViewType
import dev.mslalith.focuslauncher.core.model.appdrawer.AppDrawerItem
import dev.mslalith.focuslauncher.core.ui.VerticalSpacer
import dev.mslalith.focuslauncher.core.ui.remember.rememberAppColor
import dev.mslalith.focuslauncher.feature.appdrawerpage.utils.Constants.APP_ICON_SIZE
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
            // Pure String key — structural equality works.
            // The old uniqueKey included a Drawable hashCode which is
            // identity-based → always unique → grid could never reuse cells.
            key = { it.app.packageName },
            contentType = { "app" }
        ) { app ->
            // No animateItemPlacement — removed to eliminate the per-frame
            // layout-animation measurement that caused fling jank.
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AppDrawerGridItem(
    modifier: Modifier,
    appDrawerItem: AppDrawerItem,
    appDrawerIconViewType: AppDrawerIconViewType,
    onClick: (AppDrawerItem) -> Unit,
    onLongClick: (AppDrawerItem) -> Unit
) {
    @Composable
    fun IconViewContent() {
        when (appDrawerIconViewType) {
            AppDrawerIconViewType.TEXT -> Unit

            AppDrawerIconViewType.ICONS -> {
                // produceState: bitmap decode runs off the main thread,
                // keyed by packageName. It only re-runs when the app
                // actually changes (not on every scroll frame).
                val iconBitmap by produceState<ImageBitmap?>(
                    initialValue = null,
                    key1 = appDrawerItem.app.packageName
                ) {
                    value = appDrawerItem.icon.toBitmap().asImageBitmap()
                }

                iconBitmap?.let { bmp ->
                    Image(
                        bitmap = bmp,
                        contentDescription = appDrawerItem.app.displayName,
                        modifier = Modifier.fillMaxSize()
                    )
                } ?: Box(modifier = Modifier.fillMaxSize())
            }

            AppDrawerIconViewType.COLORED -> {
                val appIconBasedColor = rememberAppColor(graphicsColor = appDrawerItem.color)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = appIconBasedColor, shape = CircleShape)
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 4.dp)
            .clip(shape = MaterialTheme.shapes.small)
            .combinedClickable(
                onClick = { onClick(appDrawerItem) },
                onLongClick = { onLongClick(appDrawerItem) }
            )
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(size = APP_ICON_SIZE * 1.5f)) {
            IconViewContent()
        }
        VerticalSpacer(spacing = 8.dp)
        Text(
            text = appDrawerItem.app.displayName,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

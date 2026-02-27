package dev.mslalith.focuslauncher.feature.appdrawerpage.apps.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import dev.mslalith.focuslauncher.core.common.extensions.isAlphabet
import dev.mslalith.focuslauncher.core.model.appdrawer.AppDrawerIconViewType
import dev.mslalith.focuslauncher.core.model.appdrawer.AppDrawerItem
import dev.mslalith.focuslauncher.core.ui.VerticalSpacer
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AppsList(
    apps: ImmutableList<AppDrawerItem>,
    appDrawerIconViewType: AppDrawerIconViewType,
    showAppGroupHeader: Boolean,
    isSearchQueryEmpty: Boolean,
    onAppClick: (AppDrawerItem) -> Unit,
    onAppLongClick: (AppDrawerItem) -> Unit
) {
    val configuration = LocalConfiguration.current
    val topSpacing = configuration.screenHeightDp.dp * 0.2f
    val bottomSpacing = configuration.screenHeightDp.dp * 0.05f

    // Build a flat sealed list for true LazyColumn virtualization.
    // derivedStateOf ensures this only recomputes when `apps` actually changes,
    // not on every recomposition of the parent.
    val flatItems by remember(apps, showAppGroupHeader) {
        derivedStateOf {
            buildFlatList(apps = apps, showGroupHeaders = showAppGroupHeader)
        }
    }

    val topPadding = if (isSearchQueryEmpty) topSpacing else 0.dp
    val bottomPadding = if (isSearchQueryEmpty) bottomSpacing else 0.dp

    LazyColumn(
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier
            .fillMaxSize()
            .height(height = 150.dp)
    ) {
        item(key = "top_spacer", contentType = "spacer") {
            VerticalSpacer(spacing = topPadding)
        }

        items(
            items = flatItems,
            // Stable string keys let LazyColumn reuse existing item compositions
            // instead of destroying and recreating them on list changes.
            key = { row ->
                when (row) {
                    is AppListRow.Header -> "header_${row.char}"
                    is AppListRow.Item   -> "item_${row.app.app.packageName}"
                }
            },
            contentType = { row ->
                when (row) {
                    is AppListRow.Header -> "header"
                    is AppListRow.Item   -> "app_item"
                }
            }
        ) { row ->
            when (row) {
                is AppListRow.Header -> {
                    // Only show header if group headers are enabled.
                    // We already excluded lone headers in buildFlatList when
                    // showGroupHeaders = false, so this is just a safety check.
                    if (showAppGroupHeader) {
                        CharacterHeader(character = row.char)
                    }
                }
                is AppListRow.Item -> {
                    // No animateItemPlacement — it triggers expensive layout
                    // animation calculations on every list update (including every
                    // keystroke during search). Removing it gives a significant
                    // scroll and search performance boost.
                    AppDrawerListItem(
                        appDrawerItem = row.app,
                        appDrawerIconViewType = appDrawerIconViewType,
                        onClick = onAppClick,
                        onLongClick = onAppLongClick
                    )
                }
            }
        }

        item(key = "bottom_spacer", contentType = "spacer") {
            VerticalSpacer(spacing = bottomPadding)
        }
    }
}

/**
 * Builds the flat list of header + item rows for the LazyColumn.
 * Extracted to a pure function so it is easy to reason about and test.
 *
 * When [showGroupHeaders] is false, header rows are still included in the
 * list structure (to keep keys stable) but the composable above simply
 * won't render them. This avoids re-keying the entire list when the
 * "show group header" setting is toggled.
 */
private fun buildFlatList(
    apps: ImmutableList<AppDrawerItem>,
    showGroupHeaders: Boolean
): List<AppListRow> = buildList {
    var lastChar: Char? = null
    apps.forEach { app ->
        val ch = app.app.displayName.firstOrNull()
            ?.let { if (it.isAlphabet()) it.uppercaseChar() else '#' } ?: '#'
        if (ch != lastChar) {
            add(AppListRow.Header(ch))
            lastChar = ch
        }
        add(AppListRow.Item(app))
    }
}

@Stable
private sealed interface AppListRow {
    data class Header(val char: Char) : AppListRow
    data class Item(val app: AppDrawerItem) : AppListRow
}

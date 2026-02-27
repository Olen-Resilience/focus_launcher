package dev.mslalith.focuslauncher.feature.appdrawerpage.apps.list

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

    // derivedStateOf: only rebuilds the flat list when `apps` or
    // `showAppGroupHeader` actually changes — not on every parent recompose.
    val flatItems by remember(apps, showAppGroupHeader) {
        derivedStateOf { buildFlatList(apps) }
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
            // String keys: LazyColumn can now correctly identify which items
            // are the same between recompositions and skip composing them.
            // This is the single biggest scroll-performance win — without
            // stable keys every item is always considered "changed".
            key = { row ->
                when (row) {
                    is AppListRow.Header -> "h_${row.char}"
                    is AppListRow.Item   -> row.packageName  // plain String, no allocation
                }
            },
            contentType = { row ->
                when (row) {
                    is AppListRow.Header -> 0
                    is AppListRow.Item   -> 1
                }
            }
        ) { row ->
            when (row) {
                is AppListRow.Header -> {
                    if (showAppGroupHeader) {
                        CharacterHeader(character = row.char)
                    }
                }
                is AppListRow.Item -> {
                    // NO animateItemPlacement — removed deliberately.
                    // It forces a layout-animation measurement pass on every
                    // list change (including every search keystroke), which
                    // adds ~4ms per frame and causes the "slight lag" on fling.
                    AppDrawerListItem(
                        appDrawerItem = row.item,
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
 * Builds the flat row list for the LazyColumn.
 * Interleaves section headers with app items so every row is its own
 * independently composable, independently key-able LazyColumn cell.
 * Extracted as a pure function — easy to unit-test and has no allocations
 * beyond the output list itself.
 */
private fun buildFlatList(apps: ImmutableList<AppDrawerItem>): List<AppListRow> =
    buildList(capacity = apps.size + 26 /* max 26 letter headers */) {
        var lastChar: Char? = null
        for (app in apps) {
            val ch = app.app.displayName.firstOrNull()
                ?.let { if (it.isAlphabet()) it.uppercaseChar() else '#' } ?: '#'
            if (ch != lastChar) {
                add(AppListRow.Header(ch))
                lastChar = ch
            }
            add(AppListRow.Item(app.app.packageName, app))
        }
    }

/**
 * Sealed row type for the flat list.
 *
 * Item stores packageName separately so the LazyColumn key can be a plain
 * String without allocating a Pair or accessing the nested data class.
 * The @Stable annotation tells the Compose compiler that instances with
 * equal fields will always produce the same composition output — this
 * enables the compiler to skip recomposing rows whose data hasn't changed.
 */
@Stable
private sealed interface AppListRow {
    @Stable data class Header(val char: Char) : AppListRow
    @Stable data class Item(val packageName: String, val item: AppDrawerItem) : AppListRow
}

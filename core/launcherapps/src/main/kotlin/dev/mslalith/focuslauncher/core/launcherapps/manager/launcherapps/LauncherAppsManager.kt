package dev.mslalith.focuslauncher.core.launcherapps.manager.iconcache

import android.graphics.drawable.Drawable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-lifetime, thread-safe icon store.
 *
 * WHY THIS FIXES SCROLL JANK
 * ──────────────────────────
 * Previously every AppDrawerItem held a Drawable directly.
 * Drawable.hashCode() is identity-based (memory address), so
 * generateHashCode() always returns a unique value per instance even if
 * the icon is visually identical. Result: LazyColumn could never detect
 * that a list item was unchanged → every visible row recomposed on every
 * scroll frame → jank on fast flings.
 *
 * Fix: list items carry only stable value types (String, Boolean, Int?).
 * Drawables live here, looked up by packageName at draw time via
 * rememberAppIcon(). LazyColumn keys are packageName strings →
 * perfect structural equality → zero spurious recompositions while scrolling.
 */
@Singleton
class AppIconCache @Inject constructor() {

    @Volatile
    private var cache: Map<String, Drawable> = emptyMap()

    fun get(packageName: String): Drawable? = cache[packageName]

    fun put(packageName: String, drawable: Drawable) {
        synchronized(this) { cache = cache + (packageName to drawable) }
    }

    fun putAll(icons: Map<String, Drawable>) {
        if (icons.isEmpty()) return
        synchronized(this) { cache = cache + icons }
    }

    fun remove(packageName: String) {
        if (!cache.containsKey(packageName)) return
        synchronized(this) { cache = cache - packageName }
    }

    fun clear() {
        synchronized(this) { cache = emptyMap() }
    }

    fun contains(packageName: String): Boolean = cache.containsKey(packageName)
}

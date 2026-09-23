package com.example.sidehustle.ui.common.loading

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.sidehustle.R

/**
 * Pull-to-refresh indicator in the SideHustle palette: lime / cyan spinner on the surface colour.
 *
 * Call once when the view is created:
 * ```
 * binding.swipeRefresh.applySideHustleStyle()
 * binding.swipeRefresh.setOnRefreshListener { reload() }
 * // when the request finishes:
 * binding.swipeRefresh.isRefreshing = false
 * ```
 *
 * Use pull-to-refresh only when the list already has content. The first load of a list uses the
 * skeleton (view_list_loading) instead.
 */
fun SwipeRefreshLayout.applySideHustleStyle() {
    // Dark variants: the bright lime/cyan are too faint on a light circle.
    setColorSchemeResources(R.color.lime_dark, R.color.cyan_dark)
    setProgressBackgroundColorSchemeResource(R.color.surface)
}

package com.example.sidehustle.ui.navigation

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.example.sidehustle.R
import com.google.android.material.animation.AnimationUtils

/**
 * Custom curved bottom nav used instead of Material BottomNavigationView.
 *
 * Structure:
 * 1. [CurvedNavBackgroundView] — clear glass island with animated notch
 * 2. Row of icon + label tabs (cyan, muted when inactive)
 * 3. Floating bubble — active icon moves here; inline icon hides
 *
 * Wired to Navigation Component in [setup].
 */
class CurvedBottomNavBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    data class Tab(
        val destinationId: Int,
        val itemView: LinearLayout,
        val iconView: ImageView,
        val labelView: TextView,
        val iconRes: Int,
    )

    private val backgroundView: CurvedNavBackgroundView
    private val bubble: FrameLayout
    private val bubbleIcon: ImageView
    private val tabs: List<Tab>
    private var selectedIndex = 0
    private var navController: NavController? = null
    private var bubbleAnimator: ValueAnimator? = null

    // All nav text/icons use cyan — alpha differs for active vs inactive
    private val inactiveColor = ContextCompat.getColor(context, R.color.nav_icon_inactive)
    private val activeColor = ContextCompat.getColor(context, R.color.nav_icon_active)
    private val labelInactiveColor = ContextCompat.getColor(context, R.color.nav_label_inactive)
    private val labelActiveColor = ContextCompat.getColor(context, R.color.nav_label_active)

    init {
        clipChildren = false
        clipToPadding = false

        LayoutInflater.from(context).inflate(R.layout.view_curved_bottom_nav, this, true)

        backgroundView = findViewById(R.id.nav_background)
        bubble = findViewById(R.id.nav_bubble)
        bubbleIcon = findViewById(R.id.nav_bubble_icon)

        tabs = listOf(
            Tab(R.id.dashboardFragment, findViewById(R.id.nav_item_dashboard),
                findViewById(R.id.nav_icon_dashboard), findViewById(R.id.nav_label_dashboard),
                R.drawable.ic_nav_home_outline),
            Tab(R.id.clientsFragment, findViewById(R.id.nav_item_clients),
                findViewById(R.id.nav_icon_clients), findViewById(R.id.nav_label_clients),
                R.drawable.ic_nav_clients_outline),
            Tab(R.id.projectsFragment, findViewById(R.id.nav_item_projects),
                findViewById(R.id.nav_icon_projects), findViewById(R.id.nav_label_projects),
                R.drawable.ic_nav_projects_outline),
            Tab(R.id.invoicesFragment, findViewById(R.id.nav_item_finances),
                findViewById(R.id.nav_icon_finances), findViewById(R.id.nav_label_finances),
                R.drawable.ic_nav_wallet_outline),
            Tab(R.id.settingsFragment, findViewById(R.id.nav_item_settings),
                findViewById(R.id.nav_icon_settings), findViewById(R.id.nav_label_settings),
                R.drawable.ic_nav_settings_outline),
        )

        tabs.forEachIndexed { index, tab ->
            styleInactive(tab)
            tab.itemView.setOnClickListener {
                if (index == selectedIndex) return@setOnClickListener
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                selectTab(index, animate = true)
                navController?.navigate(tab.destinationId, null, buildNavOptions())
            }
        }

        doOnLayout {
            if (bubble.x == 0f && width > 0) selectTab(0, animate = false)
        }
    }

    /** Connect to NavController — syncs bubble position when destination changes. */
    fun setup(navController: NavController) {
        this.navController = navController

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val index = tabs.indexOfFirst { it.destinationId == destination.id }
            if (index >= 0 && index != selectedIndex) selectTab(index, animate = true)
        }

        val currentIndex = tabs.indexOfFirst { it.destinationId == navController.currentDestination?.id }
        if (currentIndex >= 0) post { selectTab(currentIndex, animate = false) }
    }

    private fun buildNavOptions(): NavOptions {
        return NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setRestoreState(true)
            .setPopUpTo(R.id.dashboardFragment, inclusive = false, saveState = true)
            .build()
    }

    private fun selectTab(index: Int, animate: Boolean) {
        selectedIndex = index

        tabs.forEachIndexed { tabIndex, tab ->
            val selected = tabIndex == index
            tab.iconView.isVisible = !selected // active icon lives in the bubble
            if (selected) {
                tab.labelView.setTextColor(labelActiveColor)
                tab.labelView.setTypeface(null, Typeface.BOLD)
            } else {
                styleInactive(tab)
            }
        }

        val target = tabs[index].itemView
        target.doOnLayout { moveBubbleToTab(target, tabs[index].iconRes, animate) }
        if (target.isLaidOut && width > 0) moveBubbleToTab(target, tabs[index].iconRes, animate)
    }

    /** Slides bubble + notch to the selected tab center. */
    private fun moveBubbleToTab(tabItem: View, iconRes: Int, animate: Boolean) {
        val tabCenterX = tabItem.x + tabItem.width / 2f
        val bubbleSize = resources.getDimension(R.dimen.curved_nav_bubble_size)
        val targetX = tabCenterX - bubbleSize / 2f
        val targetY = backgroundView.bubbleCenterY() - bubbleSize / 2f

        bubble.isVisible = true
        bubbleIcon.setImageResource(iconRes)
        bubbleIcon.setColorFilter(activeColor)

        backgroundView.setNotchCenter(tabCenterX, animate)

        bubbleAnimator?.cancel()
        if (animate) {
            bubbleAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 380L
                interpolator = AnimationUtils.FAST_OUT_SLOW_IN_INTERPOLATOR
                val startX = bubble.x
                val startY = bubble.y
                addUpdateListener { animator ->
                    val fraction = animator.animatedValue as Float
                    bubble.x = startX + (targetX - startX) * fraction
                    bubble.y = startY + (targetY - startY) * fraction
                }
                start()
            }
        } else {
            bubble.x = targetX
            bubble.y = targetY
        }
    }

    private fun styleInactive(tab: Tab) {
        tab.iconView.isVisible = true
        tab.iconView.setImageResource(tab.iconRes)
        tab.iconView.setColorFilter(inactiveColor)
        tab.labelView.setTextColor(labelInactiveColor)
        tab.labelView.setTypeface(null, Typeface.NORMAL)
    }
}

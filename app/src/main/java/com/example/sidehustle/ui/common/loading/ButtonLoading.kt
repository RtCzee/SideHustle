package com.example.sidehustle.ui.common.loading

import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.StringRes
import com.example.sidehustle.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicatorSpec
import com.google.android.material.progressindicator.IndeterminateDrawable

/**
 * Button loading pattern for forms (login, register, save, delete):
 *
 *  1. the button is disabled, so it cannot be tapped twice,
 *  2. its label becomes a loading label ("Saving…"),
 *  3. a small spinner appears next to the label.
 *
 * Turning loading off puts back the original label, icon and icon tint.
 *
 * ```
 * // on submit
 * binding.saveButton.setLoading(true, R.string.action_saving)
 * // when the request finishes (success or error)
 * binding.saveButton.setLoading(false, R.string.action_saving)
 * ```
 *
 * Safe to call repeatedly: the original state is captured once, on the first `true`.
 * Other controls on the form (other buttons, text fields) are the caller's to disable.
 * Enabling the button again is done here; if the button should stay disabled for another
 * reason (e.g. invalid form), re-apply that after calling `setLoading(false, ...)`.
 *
 * The spinner's animation is stopped when loading ends and when the button is detached from
 * the window, so a destroyed screen never leaves an animator running.
 *
 * @param loadingTextRes label shown while loading, e.g. R.string.action_saving
 */
fun MaterialButton.setLoading(loading: Boolean, @StringRes loadingTextRes: Int) {
    val saved = getTag(R.id.tag_button_loading_state) as? ButtonLoadingState

    if (loading) {
        if (saved == null) startLoading()
        text = context.getString(loadingTextRes)
        isEnabled = false
    } else {
        if (saved != null) stopLoading(saved)
        isEnabled = true
    }
}

private fun MaterialButton.startLoading() {
    val spinner = createSpinnerDrawable()

    // Pause the spinner when the button leaves the window; resume if it comes back.
    val attachListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            spinner.setVisible(true, false)
        }

        override fun onViewDetachedFromWindow(v: View) {
            spinner.setVisible(false, false)
        }
    }
    addOnAttachStateChangeListener(attachListener)

    setTag(
        R.id.tag_button_loading_state,
        ButtonLoadingState(
            text = text,
            icon = icon,
            iconGravity = iconGravity,
            iconTint = iconTint,
            spinner = spinner,
            attachListener = attachListener,
        ),
    )

    iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
    // Drop the tint so the spinner keeps its own colour on the disabled background.
    iconTint = null
    icon = spinner
}

private fun MaterialButton.stopLoading(saved: ButtonLoadingState) {
    setTag(R.id.tag_button_loading_state, null)
    removeOnAttachStateChangeListener(saved.attachListener)

    text = saved.text
    iconGravity = saved.iconGravity
    iconTint = saved.iconTint
    icon = saved.icon

    // The button no longer owns the spinner, so make sure its animator does not keep running.
    saved.spinner.setVisible(false, false)
}

private fun MaterialButton.createSpinnerDrawable(): Drawable {
    val spec = CircularProgressIndicatorSpec(
        context,
        /* attrs = */ null,
        /* defStyleAttr = */ 0,
        R.style.Widget_SideHustle_CircularProgressIndicator_Button,
    )
    return IndeterminateDrawable.createCircularDrawable(context, spec)
}

private class ButtonLoadingState(
    val text: CharSequence?,
    val icon: Drawable?,
    val iconGravity: Int,
    val iconTint: ColorStateList?,
    val spinner: Drawable,
    val attachListener: View.OnAttachStateChangeListener,
)

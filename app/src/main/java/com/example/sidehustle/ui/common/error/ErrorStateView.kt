package com.example.sidehustle.ui.common.error

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import com.example.sidehustle.R
import com.example.sidehustle.databinding.ViewErrorStateBinding

/** Reusable, scrollable full-page state. Call render again when the failed operation changes. */
class ErrorStateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {
    private val binding = ViewErrorStateBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        ViewCompat.setAccessibilityHeading(binding.errorTitle, true)
    }

    fun render(state: ErrorUiState, onAction: () -> Unit, onBack: (() -> Unit)? = null) {
        binding.errorTitle.setText(state.title)
        binding.errorMessage.text = state.validationMessage ?: context.getString(state.message)
        binding.errorAction.setText(state.action.label)
        binding.errorAction.setOnClickListener { onAction() }
        binding.errorSecondary.isVisible = onBack != null
        binding.errorSecondary.setText(R.string.action_back)
        binding.errorSecondary.setOnClickListener { onBack?.invoke() }
        isVisible = true
    }
}

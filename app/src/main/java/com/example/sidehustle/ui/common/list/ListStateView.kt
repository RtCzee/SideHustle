package com.example.sidehustle.ui.common.list

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.sidehustle.data.remote.ApiResult
import com.example.sidehustle.databinding.ViewListStateBinding
import com.example.sidehustle.ui.common.error.showFullPageError

class ListStateView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    private val binding = ViewListStateBinding.inflate(LayoutInflater.from(context), this, true)

    fun showLoading() {
        isVisible = true
        binding.loadingGroup.isVisible = true
        binding.emptyGroup.isVisible = false
        binding.errorState.isVisible = false
    }

    fun showEmpty(@StringRes title: Int, @StringRes message: Int, @StringRes action: Int, onAction: () -> Unit) {
        isVisible = true
        binding.loadingGroup.isVisible = false
        binding.errorState.isVisible = false
        binding.emptyGroup.isVisible = true
        binding.emptyTitle.setText(title)
        binding.emptyMessage.setText(message)
        binding.emptyAction.setText(action)
        binding.emptyAction.setOnClickListener { onAction() }
    }

    fun showError(fragment: Fragment, error: ApiResult.Error, retry: () -> Unit) {
        isVisible = true
        binding.loadingGroup.isVisible = false
        binding.emptyGroup.isVisible = false
        fragment.showFullPageError(binding.errorState, error, retry)
    }
}

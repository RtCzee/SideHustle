package com.example.sidehustle.ui.common.error

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.sidehustle.R
import com.example.sidehustle.data.remote.ApiResult

class NotFoundFragment : Fragment(R.layout.fragment_not_found) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        showFullPageError(view as ErrorStateView, ApiResult.Error("", 404)) {}
    }
}

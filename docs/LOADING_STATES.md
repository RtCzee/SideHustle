# Loading states

Shared loading UI for every screen that talks to the API. Use these instead of one-off spinners.
Code lives in `ui/common/loading/`, layouts in `res/layout/view_*.xml`, styles in
`res/values/styles_loading.xml`.

## Which pattern do I use?

| Situation | Pattern | You use |
| --- | --- | --- |
| Form submit, save, delete | **Button loading** | `button.setLoading(true, R.string.action_saving)` |
| First load of a screen (dashboard metrics) | **Screen loading** | `view_loading.xml` + `LoadingStateHandler` |
| First load of a list (clients, projects) | **List skeleton** | `view_list_loading.xml` + `LoadingStateHandler` |
| User refreshes a list that already has rows | **Pull-to-refresh** | `SwipeRefreshLayout` + `applySideHustleStyle()` |
| Background / non-blocking work | **Inline bar** | `view_loading_inline.xml` (`show()` / `hide()`) |
| Short critical action (create invoice) | **Blocking overlay** | `view_loading_overlay.xml` + `BlockingLoadingOverlay` |

Rule of thumb: **nothing on screen yet → skeleton/spinner. Something on screen already → pull-to-refresh or
a button spinner. Never replace existing content with a spinner just to refresh it.**

## 1. Button loading (forms)

Disables the button (no double-submit), swaps the label for a loading label, and shows a small spinner.
The original label/icon come back when loading ends.

```kotlin
import com.example.sidehustle.ui.common.loading.setLoading

// on submit
binding.saveButton.setLoading(true, R.string.action_saving)

// when the request finishes, on success AND on error
binding.saveButton.setLoading(false, R.string.action_saving)
```

Add a `loading` string for your button (existing ones: `action_saving`, `action_login_loading`,
`action_register_loading`). Disable the *other* controls on the form yourself
(`googleButton.isEnabled = !loading`). See `LoginFragment.setLoading` for a full example.

## 2. Screen loading (centred)

In the layout, put the content and `view_loading` in a `FrameLayout`. The include starts hidden.

```xml
<FrameLayout ...>
    <androidx.core.widget.NestedScrollView android:id="@+id/content" ... />
    <include android:id="@+id/loading" layout="@layout/view_loading" />
</FrameLayout>
```

In the Fragment:

```kotlin
private var loading: LoadingStateHandler? = null

override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    binding.loading.loadingMessage.setText(R.string.loading_clients)   // optional custom text
    loading = LoadingStateHandler.forViews(
        loadingView = binding.loading.root,
        contentView = binding.content,
        lifecycleOwner = viewLifecycleOwner,   // cleans up when the view is destroyed
    )

    viewLifecycleOwner.lifecycleScope.launch {
        val result = loading!!.whileLoading { repository.fetchSomething() }
        // ... show result
    }
}

override fun onDestroyView() { loading = null; _binding = null; super.onDestroyView() }
```

`whileLoading { }` turns loading on, runs the block, and turns it off even if the block throws or the
coroutine is cancelled. Overlapping calls are counted, so loading stays on until the last one finishes.
You can also call `loading.setLoading(true/false)` directly.

**Screens that extend `PlaceholderFragment`** get all of this for free: just use `loading.whileLoading { }`
and override `loadingMessageRes` (see `DashboardFragment`).

## 3. List skeleton (shimmer)

Same as screen loading, but include `view_list_loading` over the list. Pass the list (or its
`SwipeRefreshLayout`) as `contentView`. See `fragment_clients.xml` and `ClientsFragment`.

```xml
<FrameLayout ...>
    <androidx.swiperefreshlayout.widget.SwipeRefreshLayout android:id="@+id/swipe_refresh" ...>
        <androidx.recyclerview.widget.RecyclerView ... />
    </androidx.swiperefreshlayout.widget.SwipeRefreshLayout>
    <include android:id="@+id/list_loading" layout="@layout/view_list_loading" />
</FrameLayout>
```

To change the row shape, edit `item_skeleton_row.xml`. The shimmer stops itself when hidden or detached
and stays static if the user has animations turned off in system settings.

## 4. Pull-to-refresh

```kotlin
binding.swipeRefresh.applySideHustleStyle()          // lime / cyan colours
binding.swipeRefresh.setOnRefreshListener { reload() }
// when the request finishes (in a finally block):
binding.swipeRefresh.isRefreshing = false
```

On the first load use the skeleton (section 3) and do **not** set `isRefreshing`. See `ClientsFragment.loadClients`,
which also shows how to make sure only the newest request clears the indicators.

## 5. Blocking overlay (optional)

Dims the screen and swallows touches. Use for short critical actions only. Put the include as the **last**
child of a `FrameLayout` so it sits on top:

```xml
<include android:id="@+id/overlay" layout="@layout/view_loading_overlay" />
```

```kotlin
overlay = BlockingLoadingOverlay(binding.overlay, viewLifecycleOwner, requireActivity().onBackPressedDispatcher)

overlay.show(R.string.creating_invoice)                          // Back still works as normal
overlay.show(R.string.creating_invoice) { job.cancel() }         // shows Cancel; Back cancels too
overlay.hide()
```

The overlay covers the Fragment, not the bottom navigation bar.

## Rules (from the issue)

- **Back button:** loading never blocks Back. The only exception is the overlay when you pass `onCancel`,
  which deliberately turns Back into "cancel".
- **No crashes on fast show/hide:** `LoadingStateHandler` waits 150 ms before showing (a fast response never
  flashes a spinner) and keeps the UI up for at least 350 ms once shown (it never blinks). Rapid on/off toggling
  is safe, and passing `lifecycleOwner = viewLifecycleOwner` cancels pending callbacks when the view is
  destroyed. Both timings are parameters of `forViews(...)`. Never touch `binding` after a suspend call
  unless the coroutine is scoped to `viewLifecycleOwner` (or use `_binding?.`).
- **Colours and text:** indicators use `Widget.SideHustle.*` styles (never the stock purple Material ones);
  all loading text is in `strings.xml`. Indicators use `lime_dark` because plain lime is too faint on the
  light surface.
- **Accessibility:** the spinner beside a visible message is silent and the message is announced; indicators
  with no text (inline bar, skeleton) have a content description.
- **Out of scope:** empty states (#46) and error pages (#47).

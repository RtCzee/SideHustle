package com.example.sidehustle.ui.navigation

private val resourceId = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")

/** Creating is explicit. Absent, blank or malformed IDs must never open a create form. */
fun isValidDetailRequest(id: String?, isCreating: Boolean): Boolean =
    if (isCreating) id == null else id != null && resourceId.matches(id)

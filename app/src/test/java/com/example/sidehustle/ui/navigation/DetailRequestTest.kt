package com.example.sidehustle.ui.navigation

import org.junit.Assert.*
import org.junit.Test

class DetailRequestTest {
    private val id = "e4b061f5-8efd-4e84-a6de-f42dc8a4dcab"

    @Test fun `missing or malformed detail IDs cannot accidentally create a job`() {
        listOf(null, "", " ", "unknown", "../jobs", "1-1-1-1-1").forEach {
            assertFalse(isValidDetailRequest(it, isCreating = false))
        }
    }

    @Test fun `valid IDs allow details and creation requires explicit intent without an ID`() {
        assertTrue(isValidDetailRequest(id, isCreating = false))
        assertTrue(isValidDetailRequest(id.uppercase(), isCreating = false))
        assertTrue(isValidDetailRequest(null, isCreating = true))
        assertFalse(isValidDetailRequest(id, isCreating = true))
        assertFalse(isValidDetailRequest("", isCreating = true))
    }
}

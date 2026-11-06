package com.example.sidehustle.ui.navigation

import com.example.sidehustle.R
import org.junit.Assert.*
import org.junit.Test

class SessionNavigationTest {
    @Test fun `signed in sessions cannot navigate to either auth screen`() {
        listOf(R.id.loginFragment, R.id.registerFragment).forEach {
            assertEquals(R.id.dashboardFragment, requiredSessionDestination(it, true))
            assertNull(requiredSessionDestination(it, false))
        }
    }

    @Test fun `every main section and valid detail screen requires a session`() {
        (mainDestinations + setOf(R.id.clientDetailsFragment, R.id.projectDetailsFragment)).forEach {
            assertEquals(R.id.loginFragment, requiredSessionDestination(it, false))
            assertNull(requiredSessionDestination(it, true))
        }
    }

    @Test fun `not found remains accessible with or without a session`() {
        assertNull(requiredSessionDestination(R.id.notFoundFragment, false))
        assertNull(requiredSessionDestination(R.id.notFoundFragment, true))
    }
}

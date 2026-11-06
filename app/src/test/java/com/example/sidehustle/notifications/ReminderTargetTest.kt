package com.example.sidehustle.notifications

import org.junit.Assert.*
import org.junit.Test

class ReminderTargetTest {
    private val jobId = "11111111-1111-4111-8111-111111111111"

    @Test fun `valid job reminder retains account and target`() {
        assertEquals(ReminderTarget("alice", jobId), ReminderTarget.from("job_due", "alice", jobId))
    }

    @Test fun `malformed or incomplete payloads have no target`() {
        for (job in listOf(null, "", "not-a-job", "$jobId/other")) {
            assertNull(ReminderTarget.from("job_due", "alice", job))
        }
        assertNull(ReminderTarget.from("invoice_overdue", "alice", jobId))
        assertNull(ReminderTarget.from(null, "alice", jobId))
        assertNull(ReminderTarget.from("job_due", null, jobId))
        assertNull(ReminderTarget.from("job_due", " ", jobId))
    }

    @Test fun `disabled notifications and signed out or different accounts suppress display`() {
        val target = ReminderTarget("alice", jobId)
        assertTrue(target.canShow("alice", true))
        assertFalse(target.canShow("alice", false))
        assertFalse(target.canShow(null, true))
        assertFalse(target.canShow("bob", true))
    }
}

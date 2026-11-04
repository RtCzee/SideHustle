package com.example.sidehustle.notifications

/** Validate remote input before displaying or navigating. Keep account identity on every message. */
data class ReminderTarget(val userId: String, val jobId: String) {
    fun canShow(currentUserId: String?, enabled: Boolean): Boolean = enabled && userId == currentUserId

    companion object {
        private val idPattern = Regex("[a-fA-F0-9]{8}(-[a-fA-F0-9]{4}){3}-[a-fA-F0-9]{12}")
        fun from(type: String?, userId: String?, jobId: String?): ReminderTarget? {
            if (type != "job_due" || userId.isNullOrBlank() || jobId == null || !idPattern.matches(jobId)) return null
            return ReminderTarget(userId, jobId)
        }
    }
}

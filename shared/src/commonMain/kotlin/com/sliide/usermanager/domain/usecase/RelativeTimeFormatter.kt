package com.sliide.usermanager.domain.usecase

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Converts an [Instant] to a human-readable relative timestamp.
 * This is shared logic — runs on both Android and iOS.
 *
 * Examples: "just now", "5 minutes ago", "2 hours ago", "3 days ago"
 */
object RelativeTimeFormatter {

    fun format(instant: Instant, now: Instant = Clock.System.now()): String {
        val durationSinceEvent = now - instant

        val seconds = durationSinceEvent.inWholeSeconds
        val minutes = durationSinceEvent.inWholeMinutes
        val hours = durationSinceEvent.inWholeHours
        val days = durationSinceEvent.inWholeDays

        return when {
            seconds < 0 -> "just now" // Future timestamps
            seconds < 60 -> "just now"
            minutes == 1L -> "1 minute ago"
            minutes < 60 -> "$minutes minutes ago"
            hours == 1L -> "1 hour ago"
            hours < 24 -> "$hours hours ago"
            days == 1L -> "yesterday"
            days < 7 -> "$days days ago"
            days < 30 -> "${days / 7} week${if (days / 7 > 1) "s" else ""} ago"
            days < 365 -> "${days / 30} month${if (days / 30 > 1) "s" else ""} ago"
            else -> "${days / 365} year${if (days / 365 > 1) "s" else ""} ago"
        }
    }
}

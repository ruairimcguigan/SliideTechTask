package com.sliide.usermanager

import com.sliide.usermanager.domain.usecase.RelativeTimeFormatter
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class RelativeTimeFormatterTest {

    private val now = Clock.System.now()

    @Test
    fun justNow_forLessThan60Seconds() {
        val instant = now - 30.seconds
        assertEquals("just now", RelativeTimeFormatter.format(instant, now))
    }

    @Test
    fun justNow_forZeroSeconds() {
        assertEquals("just now", RelativeTimeFormatter.format(now, now))
    }

    @Test
    fun justNow_forFutureTimestamps() {
        val future = now + 10.minutes
        assertEquals("just now", RelativeTimeFormatter.format(future, now))
    }

    @Test
    fun oneMinuteAgo() {
        val instant = now - 1.minutes
        assertEquals("1 minute ago", RelativeTimeFormatter.format(instant, now))
    }

    @Test
    fun multipleMinutesAgo() {
        val instant = now - 45.minutes
        assertEquals("45 minutes ago", RelativeTimeFormatter.format(instant, now))
    }

    @Test
    fun oneHourAgo() {
        val instant = now - 1.hours
        assertEquals("1 hour ago", RelativeTimeFormatter.format(instant, now))
    }

    @Test
    fun multipleHoursAgo() {
        val instant = now - 5.hours
        assertEquals("5 hours ago", RelativeTimeFormatter.format(instant, now))
    }

    @Test
    fun yesterday() {
        val instant = now - 1.days
        assertEquals("yesterday", RelativeTimeFormatter.format(instant, now))
    }

    @Test
    fun multipleDaysAgo() {
        val instant = now - 4.days
        assertEquals("4 days ago", RelativeTimeFormatter.format(instant, now))
    }

    @Test
    fun oneWeekAgo() {
        val instant = now - 7.days
        assertEquals("1 week ago", RelativeTimeFormatter.format(instant, now))
    }

    @Test
    fun multipleWeeksAgo() {
        val instant = now - 21.days
        assertEquals("3 weeks ago", RelativeTimeFormatter.format(instant, now))
    }

    @Test
    fun oneMonthAgo() {
        val instant = now - 35.days
        assertEquals("1 month ago", RelativeTimeFormatter.format(instant, now))
    }

    @Test
    fun multipleMonthsAgo() {
        val instant = now - 90.days
        assertEquals("3 months ago", RelativeTimeFormatter.format(instant, now))
    }

    @Test
    fun oneYearAgo() {
        val instant = now - 400.days
        assertEquals("1 year ago", RelativeTimeFormatter.format(instant, now))
    }

    @Test
    fun multipleYearsAgo() {
        val instant = now - 800.days
        assertEquals("2 years ago", RelativeTimeFormatter.format(instant, now))
    }

    @Test
    fun boundaryAt59Seconds_showsJustNow() {
        val instant = now - 59.seconds
        assertEquals("just now", RelativeTimeFormatter.format(instant, now))
    }

    @Test
    fun boundaryAt60Seconds_showsOneMinute() {
        val instant = now - 60.seconds
        assertEquals("1 minute ago", RelativeTimeFormatter.format(instant, now))
    }

    @Test
    fun boundaryAt59Minutes_showsMinutes() {
        val instant = now - 59.minutes
        assertEquals("59 minutes ago", RelativeTimeFormatter.format(instant, now))
    }

    @Test
    fun boundaryAt23Hours_showsHours() {
        val instant = now - 23.hours
        assertEquals("23 hours ago", RelativeTimeFormatter.format(instant, now))
    }
}

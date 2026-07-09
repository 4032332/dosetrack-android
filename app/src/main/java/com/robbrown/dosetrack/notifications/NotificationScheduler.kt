package com.robbrown.dosetrack.notifications

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules and cancels medication reminders via AlarmManager exact alarms.
 *
 * Implemented in the notifications phase. Per the project rules, this MUST use exact
 * alarms rescheduled on device boot (BOOT_COMPLETED) — never inexact/repeating alarms
 * for medication reminders.
 */
@Singleton
class NotificationScheduler @Inject constructor()

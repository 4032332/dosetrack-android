package com.robbrown.dosetrack.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A recurring time at which a [Medication] is due. Translated 1:1 from the iOS Core
 * Data `Schedule` entity. `daysOfWeek` (1=Sun..7=Sat, empty = every day) and
 * `notificationIds` are stored as JSON via
 * [com.robbrown.dosetrack.data.converters.ListConverters].
 */
@Entity(
    tableName = "schedules",
    foreignKeys = [
        ForeignKey(
            entity = Medication::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("medicationId")],
)
data class Schedule(
    @PrimaryKey val id: String,
    val medicationId: String,
    val hour: Int,
    val minute: Int,
    val daysOfWeek: List<Int> = emptyList(),
    val frequency: String,
    val intervalDays: Int = 0,
    val isEnabled: Boolean = true,
    val notificationIds: List<String> = emptyList(),
    val updatedAt: Long? = null,
)

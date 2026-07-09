package com.robbrown.dosetrack.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A record of a dose being taken, skipped, or missed. Translated 1:1 from the iOS Core
 * Data `DoseLog` entity. `status` is one of "taken", "skipped", "missed".
 */
@Entity(
    tableName = "dose_logs",
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
data class DoseLog(
    @PrimaryKey val id: String,
    val medicationId: String,
    val scheduledAt: Long? = null,
    val loggedAt: Long? = null,
    val status: String,
    val notes: String? = null,
    val updatedAt: Long? = null,
)

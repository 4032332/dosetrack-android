package com.robbrown.dosetrack.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A medication or supplement the user tracks. Translated 1:1 from the iOS Core Data
 * `Medication` entity. Dates are stored as epoch millis (`Long`); UUIDs as `String`.
 * Relationships to [Schedule] and [DoseLog] are modelled as foreign keys on those
 * child entities (cascade delete).
 */
@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey val id: String,
    val name: String,
    val dosage: String,
    val unit: String,
    val colorHex: String? = null,
    val photoData: ByteArray? = null,
    val escriptData: ByteArray? = null,
    val notes: String? = null,
    val isActive: Boolean = true,
    val currentCount: Int = 0,
    val refillThreshold: Int = 0,
    val totalDosesPerDay: Int = 0,
    val sortOrder: Int = 0,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
) {
    // ByteArray fields require explicit equals/hashCode for a correct data class.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Medication) return false
        return id == other.id &&
            name == other.name &&
            dosage == other.dosage &&
            unit == other.unit &&
            colorHex == other.colorHex &&
            photoData.contentEqualsNullable(other.photoData) &&
            escriptData.contentEqualsNullable(other.escriptData) &&
            notes == other.notes &&
            isActive == other.isActive &&
            currentCount == other.currentCount &&
            refillThreshold == other.refillThreshold &&
            totalDosesPerDay == other.totalDosesPerDay &&
            sortOrder == other.sortOrder &&
            createdAt == other.createdAt &&
            updatedAt == other.updatedAt
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + dosage.hashCode()
        result = 31 * result + unit.hashCode()
        result = 31 * result + (colorHex?.hashCode() ?: 0)
        result = 31 * result + (photoData?.contentHashCode() ?: 0)
        result = 31 * result + (escriptData?.contentHashCode() ?: 0)
        result = 31 * result + (notes?.hashCode() ?: 0)
        result = 31 * result + isActive.hashCode()
        result = 31 * result + currentCount
        result = 31 * result + refillThreshold
        result = 31 * result + totalDosesPerDay
        result = 31 * result + sortOrder
        result = 31 * result + (createdAt?.hashCode() ?: 0)
        result = 31 * result + (updatedAt?.hashCode() ?: 0)
        return result
    }
}

private fun ByteArray?.contentEqualsNullable(other: ByteArray?): Boolean =
    if (this == null) other == null else other != null && this.contentEquals(other)

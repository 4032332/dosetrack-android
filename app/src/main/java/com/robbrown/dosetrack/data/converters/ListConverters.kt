package com.robbrown.dosetrack.data.converters

import androidx.room.TypeConverter
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Room type converters for the two transformable list fields carried over from the iOS
 * Core Data model: [Schedule.daysOfWeek] (`List<Int>`) and [Schedule.notificationIds]
 * (`List<String>`). Stored as JSON strings.
 */
class ListConverters {

    @TypeConverter
    fun intListToJson(value: List<Int>?): String = Json.encodeToString(value ?: emptyList())

    @TypeConverter
    fun jsonToIntList(value: String?): List<Int> =
        if (value.isNullOrEmpty()) emptyList() else Json.decodeFromString(value)

    @TypeConverter
    fun stringListToJson(value: List<String>?): String = Json.encodeToString(value ?: emptyList())

    @TypeConverter
    fun jsonToStringList(value: String?): List<String> =
        if (value.isNullOrEmpty()) emptyList() else Json.decodeFromString(value)
}

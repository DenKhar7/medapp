package iut.butinfo3.app_mobile.utils

import androidx.room.TypeConverter
import iut.butinfo3.app_mobile.model.entity.IntakeStatus
import iut.butinfo3.app_mobile.model.entity.RecurrenceUnit
import iut.butinfo3.app_mobile.model.entity.SexEnum
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.sql.Timestamp
import java.time.LocalDate
import java.util.Date

/**
 * Classe utilisé dans la création de la db,
 * Permet d'ajouter des types de données suplémentaires dans les entités
 */
class Converters {

    @TypeConverter
    fun fromTimestamp(value: Long?): Timestamp? {
        return value?.let { Timestamp(it) }
    }
    @TypeConverter
    fun fromLocalDate(value: Long?): LocalDate? {
        return value?.let { LocalDate.ofEpochDay(it) }
    }

    @TypeConverter
    fun LocalDateToLong(localDate: LocalDate?): Long? {
        return localDate?.toEpochDay()
    }

    @TypeConverter
    fun timestampToLong(timestamp: Timestamp?): Long? {
        return timestamp?.time
    }

    // Converter for java.util.Date
    @TypeConverter
    fun fromDate(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToLong(date: Date?): Long? {
        return date?.time
    }

    // Converter for SexEnum
    @TypeConverter
    fun fromString(value: String?): SexEnum? {
        return value?.let { enumValueOf<SexEnum>(it) }
    }

    @TypeConverter
    fun fromSexEnum(sex: SexEnum?): String? {
        return sex?.name
    }

    // Converter for RecurrenceUnit
    @TypeConverter
    fun fromRecurrenceUnitString(value: String?): RecurrenceUnit {
        return RecurrenceUnit.fromString(value)
    }

    @TypeConverter
    fun fromRecurrenceUnit(unit: RecurrenceUnit?): String {
        return unit?.name ?: RecurrenceUnit.DAY.name
    }

    // Converter for IntakeStatus
    @TypeConverter
    fun fromIntakeStatus(status: IntakeStatus?): String {
        return status?.name ?: IntakeStatus.TAKEN.name
    }

    @TypeConverter
    fun toIntakeStatus(value: String?): IntakeStatus {
        return try {
            value?.let { IntakeStatus.valueOf(it) } ?: IntakeStatus.TAKEN
        } catch (e: IllegalArgumentException) {
            IntakeStatus.TAKEN
        }
    }

    // Converter for List<String>
    @TypeConverter
    fun fromString(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType) ?: emptyList()
    }

    @TypeConverter
    fun fromList(list: List<String>): String {
        return Gson().toJson(list)
    }
}
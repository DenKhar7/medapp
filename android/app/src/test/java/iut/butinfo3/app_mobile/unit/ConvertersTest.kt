package iut.butinfo3.app_mobile.unit

import iut.butinfo3.app_mobile.model.entity.RecurrenceUnit
import iut.butinfo3.app_mobile.model.entity.SexEnum
import iut.butinfo3.app_mobile.utils.Converters
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Timestamp
import java.time.LocalDate
import java.util.Date

class ConvertersTest {

    private lateinit var converters: Converters

    @BeforeEach
    fun setup() {
        converters = Converters()
    }

    // --- Timestamp ---

    @Test
    fun fromTimestamp_nonNull_returnsTimestamp() {
        val millis = 1700000000000L
        val result = converters.fromTimestamp(millis)
        assertNotNull(result)
        assertEquals(millis, result!!.time)
    }

    @Test
    fun fromTimestamp_null_returnsNull() {
        assertNull(converters.fromTimestamp(null))
    }

    @Test
    fun timestampToLong_nonNull_returnsMillis() {
        val ts = Timestamp(1700000000000L)
        assertEquals(1700000000000L, converters.timestampToLong(ts))
    }

    @Test
    fun timestampToLong_null_returnsNull() {
        assertNull(converters.timestampToLong(null))
    }

    @Test
    fun timestamp_roundTrip() {
        val original = 1700000000000L
        val ts = converters.fromTimestamp(original)
        val result = converters.timestampToLong(ts)
        assertEquals(original, result)
    }

    // --- Date ---

    @Test
    fun fromDate_nonNull_returnsDate() {
        val millis = 1700000000000L
        val result = converters.fromDate(millis)
        assertNotNull(result)
        assertEquals(millis, result!!.time)
    }

    @Test
    fun fromDate_null_returnsNull() {
        assertNull(converters.fromDate(null))
    }

    @Test
    fun dateToLong_nonNull_returnsMillis() {
        val date = Date(1700000000000L)
        assertEquals(1700000000000L, converters.dateToLong(date))
    }

    @Test
    fun dateToLong_null_returnsNull() {
        assertNull(converters.dateToLong(null))
    }

    @Test
    fun date_roundTrip() {
        val original = 1700000000000L
        val date = converters.fromDate(original)
        val result = converters.dateToLong(date)
        assertEquals(original, result)
    }

    // --- LocalDate ---

    @Test
    fun fromLocalDate_nonNull_returnsLocalDate() {
        val epochDay = 19000L
        val result = converters.fromLocalDate(epochDay)
        assertNotNull(result)
        assertEquals(LocalDate.ofEpochDay(epochDay), result)
    }

    @Test
    fun fromLocalDate_null_returnsNull() {
        assertNull(converters.fromLocalDate(null))
    }

    @Test
    fun localDateToLong_nonNull_returnsEpochDay() {
        val localDate = LocalDate.of(2024, 1, 15)
        val result = converters.LocalDateToLong(localDate)
        assertEquals(localDate.toEpochDay(), result)
    }

    @Test
    fun localDateToLong_null_returnsNull() {
        assertNull(converters.LocalDateToLong(null))
    }

    @Test
    fun localDate_roundTrip() {
        val original = LocalDate.of(2024, 6, 15)
        val asLong = converters.LocalDateToLong(original)
        val result = converters.fromLocalDate(asLong)
        assertEquals(original, result)
    }

    // --- SexEnum ---

    @Test
    fun fromString_F_returnsSexEnumF() {
        val input: String? = "F"
        assertEquals(SexEnum.F, converters.fromString(input))
    }

    @Test
    fun fromString_M_returnsSexEnumM() {
        val input: String? = "M"
        assertEquals(SexEnum.M, converters.fromString(input))
    }

    @Test
    fun fromString_X_returnsSexEnumX() {
        val input: String? = "X"
        assertEquals(SexEnum.X, converters.fromString(input))
    }

    @Test
    fun fromString_null_returnsNull() {
        val input: String? = null
        assertNull(converters.fromString(input))
    }

    @Test
    fun fromSexEnum_nonNull_returnsName() {
        assertEquals("F", converters.fromSexEnum(SexEnum.F))
        assertEquals("M", converters.fromSexEnum(SexEnum.M))
        assertEquals("X", converters.fromSexEnum(SexEnum.X))
    }

    @Test
    fun fromSexEnum_null_returnsNull() {
        assertNull(converters.fromSexEnum(null))
    }

    @Test
    fun sexEnum_roundTrip() {
        for (sex in SexEnum.entries) {
            val asString: String? = converters.fromSexEnum(sex)
            val result = converters.fromString(asString)
            assertEquals(sex, result)
        }
    }

    // --- RecurrenceUnit ---

    @Test
    fun fromRecurrenceUnitString_validValues_returnsCorrectUnit() {
        for (unit in RecurrenceUnit.entries) {
            assertEquals(unit, converters.fromRecurrenceUnitString(unit.name))
        }
    }

    @Test
    fun fromRecurrenceUnitString_null_returnsDay() {
        assertEquals(RecurrenceUnit.DAY, converters.fromRecurrenceUnitString(null))
    }

    @Test
    fun fromRecurrenceUnitString_invalid_returnsDay() {
        assertEquals(RecurrenceUnit.DAY, converters.fromRecurrenceUnitString("INVALIDE"))
    }

    @Test
    fun fromRecurrenceUnit_nonNull_returnsName() {
        for (unit in RecurrenceUnit.entries) {
            assertEquals(unit.name, converters.fromRecurrenceUnit(unit))
        }
    }

    @Test
    fun fromRecurrenceUnit_null_returnsDayName() {
        assertEquals(RecurrenceUnit.DAY.name, converters.fromRecurrenceUnit(null))
    }

    @Test
    fun recurrenceUnit_roundTrip() {
        for (unit in RecurrenceUnit.entries) {
            val asString = converters.fromRecurrenceUnit(unit)
            val result = converters.fromRecurrenceUnitString(asString)
            assertEquals(unit, result)
        }
    }

    // --- List<String> ---

    @Test
    fun fromString_jsonList_returnsList() {
        val json = """["a","b","c"]"""
        val result = converters.fromString(json)
        assertEquals(listOf("a", "b", "c"), result)
    }

    @Test
    fun fromString_emptyJsonList_returnsEmptyList() {
        val result = converters.fromString("[]")
        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun fromList_returnJsonString() {
        val list = listOf("oral", "injectable")
        val json = converters.fromList(list)
        assertTrue(json.contains("oral"))
        assertTrue(json.contains("injectable"))
    }

    @Test
    fun list_roundTrip() {
        val original = listOf("oral", "topique", "injectable")
        val json = converters.fromList(original)
        val result = converters.fromString(json)
        assertEquals(original, result)
    }
}

package iut.butinfo3.app_mobile.structural

import iut.butinfo3.app_mobile.model.entity.RecurrenceUnit
import iut.butinfo3.app_mobile.model.entity.SexEnum
import iut.butinfo3.app_mobile.utils.Converters
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Timestamp
import java.time.LocalDate
import java.util.Date

/**
 * Tests structurels pour Converters - couverture de toutes les branches (null / non-null).
 */
class ConvertersStructuralTest {

    private lateinit var converters: Converters

    @BeforeEach
    fun setup() {
        converters = Converters()
    }

    // --- Null paths for each converter ---

    @Test
    fun fromTimestamp_null_returnsNull() {
        assertNull(converters.fromTimestamp(null))
    }

    @Test
    fun timestampToLong_null_returnsNull() {
        assertNull(converters.timestampToLong(null))
    }

    @Test
    fun fromDate_null_returnsNull() {
        assertNull(converters.fromDate(null))
    }

    @Test
    fun dateToLong_null_returnsNull() {
        assertNull(converters.dateToLong(null))
    }

    @Test
    fun fromLocalDate_null_returnsNull() {
        assertNull(converters.fromLocalDate(null))
    }

    @Test
    fun localDateToLong_null_returnsNull() {
        assertNull(converters.LocalDateToLong(null))
    }

    @Test
    fun fromString_sexEnum_null_returnsNull() {
        val input: String? = null
        assertNull(converters.fromString(input))
    }

    @Test
    fun fromSexEnum_null_returnsNull() {
        assertNull(converters.fromSexEnum(null))
    }

    @Test
    fun fromRecurrenceUnitString_null_returnsDay() {
        assertEquals(RecurrenceUnit.DAY, converters.fromRecurrenceUnitString(null))
    }

    @Test
    fun fromRecurrenceUnit_null_returnsDayName() {
        assertEquals("DAY", converters.fromRecurrenceUnit(null))
    }

    // --- Non-null paths for each converter ---

    @Test
    fun fromTimestamp_nonNull_returnsTimestamp() {
        val result = converters.fromTimestamp(1000L)
        assertNotNull(result)
        assertEquals(1000L, result!!.time)
    }

    @Test
    fun timestampToLong_nonNull_returnsLong() {
        assertEquals(1000L, converters.timestampToLong(Timestamp(1000L)))
    }

    @Test
    fun fromDate_nonNull_returnsDate() {
        val result = converters.fromDate(1000L)
        assertNotNull(result)
        assertEquals(1000L, result!!.time)
    }

    @Test
    fun dateToLong_nonNull_returnsLong() {
        assertEquals(1000L, converters.dateToLong(Date(1000L)))
    }

    @Test
    fun fromLocalDate_nonNull_returnsLocalDate() {
        val result = converters.fromLocalDate(19000L)
        assertNotNull(result)
        assertEquals(LocalDate.ofEpochDay(19000L), result)
    }

    @Test
    fun localDateToLong_nonNull_returnsEpochDay() {
        val ld = LocalDate.of(2024, 1, 1)
        assertEquals(ld.toEpochDay(), converters.LocalDateToLong(ld))
    }

    @Test
    fun fromString_sexEnum_F() {
        val input: String? = "F"
        assertEquals(SexEnum.F, converters.fromString(input))
    }

    @Test
    fun fromString_sexEnum_M() {
        val input: String? = "M"
        assertEquals(SexEnum.M, converters.fromString(input))
    }

    @Test
    fun fromString_sexEnum_X() {
        val input: String? = "X"
        assertEquals(SexEnum.X, converters.fromString(input))
    }

    @Test
    fun fromSexEnum_nonNull() {
        assertEquals("F", converters.fromSexEnum(SexEnum.F))
        assertEquals("M", converters.fromSexEnum(SexEnum.M))
        assertEquals("X", converters.fromSexEnum(SexEnum.X))
    }

    @Test
    fun fromRecurrenceUnitString_allValid() {
        for (unit in RecurrenceUnit.entries) {
            assertEquals(unit, converters.fromRecurrenceUnitString(unit.name))
        }
    }

    @Test
    fun fromRecurrenceUnitString_invalid_returnsDay() {
        assertEquals(RecurrenceUnit.DAY, converters.fromRecurrenceUnitString("INVALIDE"))
    }

    @Test
    fun fromRecurrenceUnit_allNonNull() {
        for (unit in RecurrenceUnit.entries) {
            assertEquals(unit.name, converters.fromRecurrenceUnit(unit))
        }
    }

    // --- Round-trip for all types ---

    @Test
    fun timestamp_roundTrip() {
        val original = 1700000000000L
        val decoded = converters.fromTimestamp(original)
        assertEquals(original, converters.timestampToLong(decoded))
    }

    @Test
    fun date_roundTrip() {
        val original = 1700000000000L
        val decoded = converters.fromDate(original)
        assertEquals(original, converters.dateToLong(decoded))
    }

    @Test
    fun localDate_roundTrip() {
        val ld = LocalDate.of(2024, 6, 15)
        val asLong = converters.LocalDateToLong(ld)
        assertEquals(ld, converters.fromLocalDate(asLong))
    }

    @Test
    fun sexEnum_roundTrip() {
        for (sex in SexEnum.entries) {
            val asString: String? = converters.fromSexEnum(sex)
            assertEquals(sex, converters.fromString(asString))
        }
    }

    @Test
    fun recurrenceUnit_roundTrip() {
        for (unit in RecurrenceUnit.entries) {
            val asString = converters.fromRecurrenceUnit(unit)
            assertEquals(unit, converters.fromRecurrenceUnitString(asString))
        }
    }

    @Test
    fun stringList_roundTrip() {
        val original = listOf("oral", "topique")
        val json = converters.fromList(original)
        assertEquals(original, converters.fromString(json))
    }

    @Test
    fun stringList_empty_roundTrip() {
        val original = emptyList<String>()
        val json = converters.fromList(original)
        assertEquals(original, converters.fromString(json))
    }
}

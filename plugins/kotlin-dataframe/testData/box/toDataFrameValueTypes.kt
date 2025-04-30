import kotlinx.datetime.*
import java.math.BigDecimal
import java.math.BigInteger
import java.time.temporal.Temporal
import kotlin.time.Duration
import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*

fun box(): String {
    // Byte
    val byteDf = listOf<Byte>(1, 2, 3).toDataFrame()
    val byteCol: DataColumn<Byte> = byteDf.value
    byteCol.print()

    val byteDfNullable = listOf<Byte?>(1, 2, null).toDataFrame()
    val byteColNullable: DataColumn<Byte?> = byteDfNullable.value
    byteColNullable.print()

    // Short
    val shortDf = listOf<Short>(1, 2, 3).toDataFrame()
    val shortCol: DataColumn<Short> = shortDf.value
    shortCol.print()

    val shortDfNullable = listOf<Short?>(1, 2, null).toDataFrame()
    val shortColNullable: DataColumn<Short?> = shortDfNullable.value
    shortColNullable.print()

    // Int
    val intDf = listOf(1, 2, 3).toDataFrame()
    val intCol: DataColumn<Int> = intDf.value
    intCol.print()

    val intDfNullable = listOf<Int?>(1, 2, null).toDataFrame()
    val intColNullable: DataColumn<Int?> = intDfNullable.value
    intColNullable.print()

    // Long
    val longDf = listOf(1L, 2L, 3L).toDataFrame()
    val longCol: DataColumn<Long> = longDf.value
    longCol.print()

    val longDfNullable = listOf<Long?>(1L, 2L, null).toDataFrame()
    val longColNullable: DataColumn<Long?> = longDfNullable.value
    longColNullable.print()

    // String
    val stringDf = listOf("hello", "world", "test").toDataFrame()
    val stringCol: DataColumn<String> = stringDf.value
    stringCol.print()

    val stringDfNullable = listOf<String?>("hello", "world", null).toDataFrame()
    val stringColNullable: DataColumn<String?> = stringDfNullable.value
    stringColNullable.print()

    // Char
    val charDf = listOf('a', 'b', 'c').toDataFrame()
    val charCol: DataColumn<Char> = charDf.value
    charCol.print()

    val charDfNullable = listOf<Char?>('a', 'b', null).toDataFrame()
    val charColNullable: DataColumn<Char?> = charDfNullable.value
    charColNullable.print()

    // Boolean
    val booleanDf = listOf(true, false, true).toDataFrame()
    val booleanCol: DataColumn<Boolean> = booleanDf.value
    booleanCol.print()

    val booleanDfNullable = listOf<Boolean?>(true, false, null).toDataFrame()
    val booleanColNullable: DataColumn<Boolean?> = booleanDfNullable.value
    booleanColNullable.print()

    // Float
    val floatDf = listOf(1.1f, 2.2f, 3.3f).toDataFrame()
    val floatCol: DataColumn<Float> = floatDf.value
    floatCol.print()

    val floatDfNullable = listOf<Float?>(1.1f, 2.2f, null).toDataFrame()
    val floatColNullable: DataColumn<Float?> = floatDfNullable.value
    floatColNullable.print()

    // Double
    val doubleDf = listOf(1.1, 2.2, 3.3).toDataFrame()
    val doubleCol: DataColumn<Double> = doubleDf.value
    doubleCol.print()

    val doubleDfNullable = listOf<Double?>(1.1, 2.2, null).toDataFrame()
    val doubleColNullable: DataColumn<Double?> = doubleDfNullable.value
    doubleColNullable.print()

    // UByte
    val uByteDf = listOf<UByte>(1u, 2u, 3u).toDataFrame()
    val uByteCol: DataColumn<UByte> = uByteDf.value
    uByteCol.print()

    val uByteDfNullable = listOf<UByte?>(1u, 2u, null).toDataFrame()
    val uByteColNullable: DataColumn<UByte?> = uByteDfNullable.value
    uByteColNullable.print()

    // UShort
    val uShortDf = listOf<UShort>(1u, 2u, 3u).toDataFrame()
    val uShortCol: DataColumn<UShort> = uShortDf.value
    uShortCol.print()

    val uShortDfNullable = listOf<UShort?>(1u, 2u, null).toDataFrame()
    val uShortColNullable: DataColumn<UShort?> = uShortDfNullable.value
    uShortColNullable.print()

    // UInt
    val uIntDf = listOf<UInt>(1u, 2u, 3u).toDataFrame()
    val uIntCol: DataColumn<UInt> = uIntDf.value
    uIntCol.print()

    val uIntDfNullable = listOf<UInt?>(1u, 2u, null).toDataFrame()
    val uIntColNullable: DataColumn<UInt?> = uIntDfNullable.value
    uIntColNullable.print()

    // ULong
    val uLongDf = listOf<ULong>(1uL, 2uL, 3uL).toDataFrame()
    val uLongCol: DataColumn<ULong> = uLongDf.value
    uLongCol.print()

    val uLongDfNullable = listOf<ULong?>(1uL, 2uL, null).toDataFrame()
    val uLongColNullable: DataColumn<ULong?> = uLongDfNullable.value
    uLongColNullable.print()


    // BigDecimal
    val bigDecimalDf = listOf(BigDecimal("10.5"), BigDecimal("20.3"), BigDecimal("30.7")).toDataFrame()
    val bigDecimalCol: DataColumn<BigDecimal> = bigDecimalDf.value
    bigDecimalCol.print()

    val bigDecimalDfNullable = listOf<BigDecimal?>(BigDecimal("10.5"), BigDecimal("20.3"), null).toDataFrame()
    val bigDecimalColNullable: DataColumn<BigDecimal?> = bigDecimalDfNullable.value
    bigDecimalColNullable.print()

    // BigInteger
    val bigIntegerDf = listOf(BigInteger("100"), BigInteger("200"), BigInteger("300")).toDataFrame()
    val bigIntegerCol: DataColumn<BigInteger> = bigIntegerDf.value
    bigIntegerCol.print()

    val bigIntegerDfNullable = listOf<BigInteger?>(BigInteger("100"), BigInteger("200"), null).toDataFrame()
    val bigIntegerColNullable: DataColumn<BigInteger?> = bigIntegerDfNullable.value
    bigIntegerColNullable.print()

    // LocalDate
    val localDateDf = listOf(LocalDate(2024, 2, 28), LocalDate(2025, 3, 1)).toDataFrame()
    val localDateCol: DataColumn<LocalDate> = localDateDf.value
    localDateCol.print()

    val localDateDfNullable = listOf<LocalDate?>(LocalDate(2024, 2, 28), null).toDataFrame()
    val localDateColNullable: DataColumn<LocalDate?> = localDateDfNullable.value
    localDateColNullable.print()

    // java.time.LocalDate
    val javaLocalDateDf = listOf<java.time.LocalDate>(java.time.LocalDate.of(2024, 2, 28)).toDataFrame()
    val javaLocalDateCol: DataColumn<java.time.LocalDate> = javaLocalDateDf.value
    javaLocalDateCol.print()

    val javaLocalDateDfNullable = listOf<java.time.LocalDate?>(java.time.LocalDate.of(2024, 2, 28), null).toDataFrame()
    val javaLocalDateColNullable: DataColumn<java.time.LocalDate?> = javaLocalDateDfNullable.value
    javaLocalDateColNullable.print()

    // TimeZone
    val timeZoneDf = listOf(TimeZone.UTC, TimeZone.of("Europe/Berlin")).toDataFrame()
    val timeZoneCol: DataColumn<TimeZone> = timeZoneDf.value
    timeZoneCol.print()

    val timeZoneDfNullable = listOf<TimeZone?>(TimeZone.UTC, null).toDataFrame()
    val timeZoneColNullable: DataColumn<TimeZone?> = timeZoneDfNullable.value
    timeZoneColNullable.print()

    // Month
    val monthDf = listOf(Month.FEBRUARY, Month.AUGUST).toDataFrame()
    val monthCol: DataColumn<Month> = monthDf.value
    monthCol.print()

    val monthDfNullable = listOf<Month?>(Month.FEBRUARY, null).toDataFrame()
    val monthColNullable: DataColumn<Month?> = monthDfNullable.value
    monthColNullable.print()

    // DayOfWeek
    val dayOfWeekDf = listOf(DayOfWeek.WEDNESDAY, DayOfWeek.MONDAY).toDataFrame()
    val dayOfWeekCol: DataColumn<DayOfWeek> = dayOfWeekDf.value
    dayOfWeekCol.print()

    val dayOfWeekDfNullable = listOf<DayOfWeek?>(DayOfWeek.WEDNESDAY, null).toDataFrame()
    val dayOfWeekColNullable: DataColumn<DayOfWeek?> = dayOfWeekDfNullable.value
    dayOfWeekColNullable.print()

    // Enum
    val enumDf = listOf(EnumExample.VALUE1, EnumExample.VALUE2).toDataFrame()
    val enumCol: DataColumn<EnumExample> = enumDf.value
    enumCol.print()

    val enumDfNullable = listOf<EnumExample?>(EnumExample.VALUE1, null).toDataFrame()
    val enumColNullable: DataColumn<EnumExample?> = enumDfNullable.value
    enumColNullable.print()

    // Non-JSON Map

    val mapsDfNullable = listOf(mapOf(1 to null, 2 to "val"), mapOf(3 to 1, 4 to true), null).toDataFrame()
    val mapsColNullable: DataColumn<Map<*, *>?> = mapsDfNullable.value
    mapsColNullable.print()

    return "OK"
}

enum class EnumExample {
    VALUE1, VALUE2
}

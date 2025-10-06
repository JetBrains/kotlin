import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    // multiple columns
    val personsDfNullable = dataFrameOf(
        "name", "age", "city", "weight", "height", "yearsToRetirement", "workExperienceYears", "dependentsCount", "annualIncome",
    )(
        "Alice", 15, "London", 99.5, "1.85", 50, 0.toShort(), 0.toByte(), 0L,
        null, null, null, null, null, null, null, null, null,
        "Bob", 20, "Paris", 140.0, "1.35", 45, 2.toShort(), 0.toByte(), 12000L,
        "Charlie", 100, "Dubai", 75.0, "1.95", 0, 70.toShort(), 0.toByte(), 0L,
        "Rose", 1, "Moscow", 45.33, "0.79", 64, 0.toShort(), 2.toByte(), 0L,
        "Dylan", 35, "London", 23.4, "1.83", 30, 15.toShort(), 1.toByte(), 90000L,
        "Eve", 40, "Paris", 56.72, "1.85", 25, 18.toShort(), 3.toByte(), 125000L,
        "Frank", 55, "Dubai", 78.9, "1.35", 10, 35.toShort(), 2.toByte(), 145000L,
        "Grace", 29, "Moscow", 67.8, "1.65", 36, 5.toShort(), 1.toByte(), 70000L,
        "Hank", 60, "Paris", 80.22, "1.75", 5, 40.toShort(), 4.toByte(), 200000L,
        "Isla", 22, "London", 75.1, "1.85", 43, 1.toShort(), 0.toByte(), 30000L,
    ).group { name and age }.into("data")

    val personsDf = personsDfNullable.dropNulls { colsAtAnyDepth() }

    // scenario #0: all numerical columns
    val res0n = personsDfNullable.cumSum()
    res0n.compareSchemas()

    val cumSum01n: DataColumn<Int?> = res0n.data.age
    val cumSum02n: DataColumn<Double> = res0n.weight
    val cumSum03n: DataColumn<Int?> = res0n.yearsToRetirement
    val cumSum04n: DataColumn<Int?> = res0n.workExperienceYears
    val cumSum05n: DataColumn<Int?> = res0n.dependentsCount
    val cumSum06n: DataColumn<Long?> = res0n.annualIncome
    val cumSum07n: DataColumn<String?> = res0n.data.name
    val cumSum08n: DataColumn<String?> = res0n.city
    val cumSum09n: DataColumn<String?> = res0n.height

    val res0 = personsDf.cumSum()
    res0.compareSchemas()

    val cumSum01: DataColumn<Int> = res0.data.age
    val cumSum02: DataColumn<Double> = res0.weight
    val cumSum03: DataColumn<Int> = res0.yearsToRetirement
    val cumSum04: DataColumn<Int> = res0.workExperienceYears
    val cumSum05: DataColumn<Int> = res0.dependentsCount
    val cumSum06: DataColumn<Long> = res0.annualIncome
    val cumSum07: DataColumn<String> = res0.data.name
    val cumSum08: DataColumn<String> = res0.city
    val cumSum09: DataColumn<String> = res0.height

    // scenario #1: particular column
    val res1n = personsDfNullable.cumSum { data.age }
    res1n.compareSchemas()

    val max11n: DataColumn<Int?> = res1n.data.age

    val res1 = personsDf.cumSum { data.age }
    res1.compareSchemas()

    val max11: DataColumn<Int> = res1.data.age

    // scenario #1.1: particular column with converted type
    val res11n = personsDfNullable.cumSum { dependentsCount }
    res11n.compareSchemas()

    val max111n: DataColumn<Int?> = res11n.dependentsCount

    val res11 = personsDf.cumSum { dependentsCount }
    res11.compareSchemas()

    val max111: DataColumn<Int> = res11.dependentsCount

    // scenario #1.2: particular column with null -> NaN
    val res12n = personsDfNullable.cumSum { weight }
    res12n.compareSchemas()

    val max121n: DataColumn<Double> = res12n.weight

    val res12 = personsDf.cumSum { weight }
    res12.compareSchemas()

    val max121: DataColumn<Double> = res12.weight

    // scenario #2: cumSum of values per columns separately
    val res3n = personsDfNullable.cumSum { weight and workExperienceYears and dependentsCount and annualIncome }
    res3n.compareSchemas()

    val max32n: DataColumn<Double> = res3n.weight
    val max33n: DataColumn<Int?> = res3n.workExperienceYears
    val max34n: DataColumn<Int?> = res3n.dependentsCount
    val max35n: DataColumn<Long?> = res3n.annualIncome

    val res3 = personsDf.cumSum { weight and workExperienceYears and dependentsCount and annualIncome }
    res3.compareSchemas()

    val max32: DataColumn<Double> = res3.weight
    val max33: DataColumn<Int> = res3.workExperienceYears
    val max34: DataColumn<Int> = res3.dependentsCount
    val max35: DataColumn<Long> = res3.annualIncome

    return "OK"
}

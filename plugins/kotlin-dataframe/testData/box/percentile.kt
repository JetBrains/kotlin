import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    // multiple columns
    val personsDf = dataFrameOf(
        "name",
        "age",
        "city",
        "weight",
        "height",
        "yearsToRetirement",
        "workExperienceYears",
        "dependentsCount",
        "annualIncome"
    )(
        "Alice", 15, "London", 99.5, "1.85", 50, 0.toShort(), 0.toByte(), 0L,
        "Bob", 20, "Paris", 140.0, "1.35", 45, 2.toShort(), 0.toByte(), 12000L,
        "Charlie", 100, "Dubai", 75.0, "1.95", 0, 70.toShort(), 0.toByte(), 0L,
        "Rose", 1, "Moscow", 45.33, "0.79", 64, 0.toShort(), 2.toByte(), 0L,
        "Dylan", 35, "London", 23.4, "1.83", 30, 15.toShort(), 1.toByte(), 90000L,
        "Eve", 40, "Paris", 56.72, "1.85", 25, 18.toShort(), 3.toByte(), 125000L,
        "Frank", 55, "Dubai", 78.9, "1.35", 10, 35.toShort(), 2.toByte(), 145000L,
        "Grace", 29, "Moscow", 67.8, "1.65", 36, 5.toShort(), 1.toByte(), 70000L,
        "Hank", 60, "Paris", 80.22, "1.75", 5, 40.toShort(), 4.toByte(), 200000L,
        "Isla", 22, "London", 75.1, "1.85", 43, 1.toShort(), 0.toByte(), 30000L,
    )

    // scenario #0: all numerical columns
    val res0 = personsDf.percentile(percentile = 30.0)
    res0.df().compareSchemas()

    val percentile01: Double? = res0.age
    val percentile02: Double? = res0.weight
    val percentile03: Double? = res0.yearsToRetirement
    val percentile04: Double? = res0.workExperienceYears
    val percentile05: Double? = res0.dependentsCount
    val percentile06: Double? = res0.annualIncome
    val percentile07: String? = res0.name
    val percentile08: String? = res0.city
    val percentile09: String? = res0.height

    // scenario #1: particular column
    val res1 = personsDf.percentileFor(percentile = 30.0) { age }
    res1.df().compareSchemas()

    val percentile11: Double? = res1.age

    // scenario #1.1: particular column with converted type
    val res11 = personsDf.percentileFor(percentile = 30.0) { dependentsCount }
    res11.df().compareSchemas()

    val percentile111: Double? = res11.dependentsCount

    // scenario #2: percentile of values per columns separately
    val res3 = personsDf.percentileFor(percentile = 30.0){ name and weight and workExperienceYears and dependentsCount and annualIncome }
    res3.df().compareSchemas()

    val percentile31: String? = res3.name
    val percentile32: Double? = res3.weight
    val percentile33: Double? = res3.workExperienceYears
    val percentile34: Double? = res3.dependentsCount
    val percentile35: Double? = res3.annualIncome

    return "OK"
}

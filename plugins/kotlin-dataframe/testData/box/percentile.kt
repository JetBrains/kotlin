import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*
import java.math.BigInteger

fun box(): String {
    // multiple columns
    val personsDf = dataFrameOf(
        "name", "age", "city", "weight", "height", "yearsToRetirement", "workExperienceYears", "dependentsCount", "annualIncome", "bigNumber",
    )(
        "Alice", 15, "London", 99.5, "1.85", 50f, 0.toShort(), 0.toByte(), 0L, BigInteger.valueOf(23L),
        "Bob", 20, "Paris", 140.0, "1.35", 45f, 2.toShort(), 0.toByte(), 12000L, BigInteger.valueOf(12L),
        "Charlie", 100, "Dubai", 75.0, "1.95", 0f, 70.toShort(), 0.toByte(), 0L, BigInteger.valueOf(68798L),
        "Rose", 1, "Moscow", 45.33, "0.79", 64f, 0.toShort(), 2.toByte(), 0L, BigInteger.valueOf(46556L),
        "Dylan", 35, "London", 23.4, "1.83", 30f, 15.toShort(), 1.toByte(), 90000L, BigInteger.valueOf(235L),
        "Eve", 40, "Paris", 56.72, "1.85", 25f, 18.toShort(), 3.toByte(), 125000L, BigInteger.valueOf(-23534L),
        "Frank", 55, "Dubai", 78.9, "1.35", 10f, 35.toShort(), 2.toByte(), 145000L, BigInteger.valueOf(235L),
        "Grace", 29, "Moscow", 67.8, "1.65", 36f, 5.toShort(), 1.toByte(), 70000L, BigInteger.valueOf(0L),
        "Hank", 60, "Paris", 80.22, "1.75", 5f, 40.toShort(), 4.toByte(), 200000L, BigInteger.valueOf(-4L),
        "Isla", 22, "London", 75.1, "1.85", 43f, 1.toShort(), 0.toByte(), 30000L, BigInteger.valueOf(2345L),
    )

    // scenario #0: all numerical columns
    personsDf.percentile(percentile = 30.0).let { row ->
        row.df().compareSchemas()

        val percentile01: Double? = row.age
        val percentile02: Double? = row.weight
        val percentile03: Double? = row.yearsToRetirement
        val percentile04: Double? = row.workExperienceYears
        val percentile05: Double? = row.dependentsCount
        val percentile06: Double? = row.annualIncome
        val percentile07: String? = row.name
        val percentile08: String? = row.city
        val percentile09: String? = row.height
    }

    // scenario #1: particular column
    personsDf.percentileFor(percentile = 30.0) { age }.let { row ->
        row.df().compareSchemas()

        val percentile11: Double? = row.age
    }

    // scenario #1.1: particular column with converted type
    personsDf.percentileFor(percentile = 30.0) { dependentsCount }.let { row ->
        row.df().compareSchemas()

        val percentile111: Double? = row.dependentsCount
    }

    // scenario #2: percentile of values per columns separately
    personsDf.percentileFor<_, String>(percentile = 30.0) { name and weight and workExperienceYears and dependentsCount and annualIncome }.let { row ->
        row.df().compareSchemas()

        val percentile31: String? = row.name
        val percentile32: Double? = row.weight
        val percentile33: Double? = row.workExperienceYears
        val percentile34: Double? = row.dependentsCount
        val percentile35: Double? = row.annualIncome
    }

    return "OK"
}

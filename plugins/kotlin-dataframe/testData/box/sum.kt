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
    personsDf.sum().let { row ->
        row.df().compareSchemas()

        val sum01: Int = row.age
        val sum02: Double = row.weight
        val sum03: Float = row.yearsToRetirement
        val sum04: Int = row.workExperienceYears
        val sum05: Int = row.dependentsCount
        val sum06: Long = row.annualIncome
    }

    // scenario #1: particular column
    personsDf.sumFor { age }.let { row ->
        row.df().compareSchemas()

        val sum11: Int = row.age
    }

    // scenario #1.1: particular column with converted type
    personsDf.sumFor { dependentsCount }.let { row ->
        row.df().compareSchemas()

        val sum111: Int = row.dependentsCount
    }

    // scenario #2: sum of values per columns separately
    personsDf.sumFor { age and weight and workExperienceYears and dependentsCount and annualIncome }.let { row ->
        row.df().compareSchemas()

        val sum31: Int = row.age
        val sum32: Double = row.weight
        val sum33: Int = row.workExperienceYears
        val sum34: Int = row.dependentsCount
        val sum35: Long = row.annualIncome
    }

    return "OK"
}

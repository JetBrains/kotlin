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
        "Alice", 15, "London", 99.5, "1.85", 50, 0.toShort(), 0.toByte(), 0L, BigInteger.valueOf(23),
        "Bob", 20, "Paris", 140.0, "1.35", 45, 2.toShort(), 0.toByte(), 12000L, BigInteger.valueOf(12),
        "Charlie", 100, "Dubai", 75.0, "1.95", 0, 70.toShort(), 0.toByte(), 0L, BigInteger.valueOf(68798),
        "Rose", 1, "Moscow", 45.33, "0.79", 64, 0.toShort(), 2.toByte(), 0L, BigInteger.valueOf(46556),
        "Dylan", 35, "London", 23.4, "1.83", 30, 15.toShort(), 1.toByte(), 90000L, BigInteger.valueOf(235),
        "Eve", 40, "Paris", 56.72, "1.85", 25, 18.toShort(), 3.toByte(), 125000L, BigInteger.valueOf(-23534),
        "Frank", 55, "Dubai", 78.9, "1.35", 10, 35.toShort(), 2.toByte(), 145000L, BigInteger.valueOf(235),
        "Grace", 29, "Moscow", 67.8, "1.65", 36, 5.toShort(), 1.toByte(), 70000L, BigInteger.valueOf(0),
        "Hank", 60, "Paris", 80.22, "1.75", 5, 40.toShort(), 4.toByte(), 200000L, BigInteger.valueOf(-4),
        "Isla", 22, "London", 75.1, "1.85", 43, 1.toShort(), 0.toByte(), 30000L, BigInteger.valueOf(2345),
    )

    // scenario #0: all numerical columns
    personsDf.max().let { row ->
        row.df().compareSchemas()

        val max01: Int? = row.age
        val max02: Double? = row.weight
        val max03: Int? = row.yearsToRetirement
        val max04: Short? = row.workExperienceYears
        val max05: Byte? = row.dependentsCount
        val max06: Long? = row.annualIncome
        val max07: String? = row.name
        val max08: String? = row.city
        val max09: String? = row.height
    }

    // scenario #1: particular column
    personsDf.maxFor { age }.let { row ->
        row.df().compareSchemas()

        val max11: Int? = row.age
    }

    // scenario #1.1: particular column with converted type
    personsDf.maxFor { dependentsCount }.let { row ->
        row.df().compareSchemas()

        val max111: Byte? = row.dependentsCount
    }

    // scenario #2: max of values per columns separately
    personsDf.maxFor<_, String> { name and weight and workExperienceYears and dependentsCount and annualIncome }.let { row ->
        row.df().compareSchemas()

        val max31: String? = row.name
        val max32: Double? = row.weight
        val max33: Short? = row.workExperienceYears
        val max34: Byte? = row.dependentsCount
        val max35: Long? = row.annualIncome
    }

    return "OK"
}

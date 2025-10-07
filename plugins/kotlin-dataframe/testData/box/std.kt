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
        "Alice", 15, "London", 99.5, "1.85", 50, 0.toShort(), 0.toByte(), 0L, BigInteger.ZERO,
        "Bob", 20, "Paris", 140.0, "1.35", 45, 2.toShort(), 0.toByte(), 12000L, BigInteger.TEN,
        "Charlie", 100, "Dubai", 75.0, "1.95", 0, 70.toShort(), 0.toByte(), 0L, BigInteger.valueOf(2),
        "Rose", 1, "Moscow", 45.33, "0.79", 64, 0.toShort(), 2.toByte(), 0L, BigInteger.valueOf(3),
        "Dylan", 35, "London", 23.4, "1.83", 30, 15.toShort(), 1.toByte(), 90000L, BigInteger.valueOf(4),
        "Eve", 40, "Paris", 56.72, "1.85", 25, 18.toShort(), 3.toByte(), 125000L, BigInteger.valueOf(5),
        "Frank", 55, "Dubai", 78.9, "1.35", 10, 35.toShort(), 2.toByte(), 145000L, BigInteger.valueOf(6),
        "Grace", 29, "Moscow", 67.8, "1.65", 36, 5.toShort(), 1.toByte(), 70000L, BigInteger.valueOf(7),
        "Hank", 60, "Paris", 80.22, "1.75", 5, 40.toShort(), 4.toByte(), 200000L, BigInteger.valueOf(8),
        "Isla", 22, "London", 75.1, "1.85", 43, 1.toShort(), 0.toByte(), 30000L, BigInteger.valueOf(9),
    )

    // scenario #0: all numerical columns
    personsDf.std().let { row ->
        row.df().compareSchemas()

        val std01: Double? = row.age
        val std02: Double? = row.weight
        val std03: Double? = row.yearsToRetirement
        val std04: Double? = row.workExperienceYears
        val std05: Double? = row.dependentsCount
        val std06: Double? = row.annualIncome
    }

    // scenario #1: particular column
    personsDf.stdFor { age }.let { row ->
        row.df().compareSchemas()

        val std11: Double? = row.age
    }

    // scenario #1.1: particular column with converted type
    personsDf.stdFor { dependentsCount }.let { row ->
        row.df().compareSchemas()

        val std111: Double? = row.dependentsCount
    }

    // scenario #2: std of values per columns separately
    personsDf.stdFor { age and weight and workExperienceYears and dependentsCount and annualIncome and bigNumber }.let { row ->
        row.df().compareSchemas()

        val std31: Double? = row.age
        val std32: Double? = row.weight
        val std33: Double? = row.workExperienceYears
        val std34: Double? = row.dependentsCount
        val std35: Double? = row.annualIncome
        val std36: Double? = row.bigNumber
    }

    return "OK"
}

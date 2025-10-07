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
    personsDf.min().let { row ->
        row.df().compareSchemas()

        val min01: Int? = row.age
        val min02: Double? = row.weight
        val min03: Int? = row.yearsToRetirement
        val min04: Short? = row.workExperienceYears
        val min05: Byte? = row.dependentsCount
        val min06: Long? = row.annualIncome
        val min07: String? = row.name
        val min08: String? = row.city
        val min09: String? = row.height
    }

    // scenario #1: particular column
    personsDf.minFor { age }.let { row ->
        row.df().compareSchemas()

        val min11: Int? = row.age
    }

    // scenario #1.1: particular column with converted type
    personsDf.minFor { dependentsCount }.let { row ->
        row.df().compareSchemas()

        val min111: Byte? = row.dependentsCount
    }

    // scenario #2: min of values per columns separately
    personsDf.minFor { name and weight and workExperienceYears and dependentsCount and annualIncome }.let { row ->
        row.df().compareSchemas()

        val min31: String? = row.name
        val min32: Double? = row.weight
        val min33: Short? = row.workExperienceYears
        val min34: Byte? = row.dependentsCount
        val min35: Long? = row.annualIncome
    }

    return "OK"
}

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*
import java.math.BigInteger

fun box(): String {
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

    // scenario #0: all intraComparable columns
    personsDf.groupBy { city }.median().let { df ->
        df.compareSchemas()

        val median01: Double? = df.age[0]
        val median02: Double? = df.weight[0]
        val median03: String? = df.height[0]
    }

    // scenario #1: particular column
    personsDf.groupBy { city }.medianFor { age }.let { df ->
        df.compareSchemas()

        val median11: Double? = df.age[0]
    }

    // scenario #1.1: particular column via median
    personsDf.groupBy { city }.median { age }.let { df ->
        df.compareSchemas()

        val median111: Double? = df.age[0]
    }

    // scenario #1.2: multiple columns via median
    personsDf.groupBy { city }.median { age and age }.let { df ->
        df.compareSchemas()

        val median111: Double? = df.median[0]
    }

    // scenario #2: particular column with new name - schema changes
    // TODO: not supported scenario
    // val res2 = personsDf.groupBy { city }.median("age", name = "newAge")
    // val median21: Int? = res2.newAge[0]

    // scenario #2.1: particular column with new name - schema changes but via columnSelector
    personsDf.groupBy { city }.median("newAge") { age }.let { df ->
        df.compareSchemas()

        val median211: Double? = df.newAge[0]
    }

    // scenario #2.2: two columns with new name - schema changes but via columnSelector
    personsDf.groupBy { city }.median("newAge") { age and age }.let { df ->
        df.compareSchemas()

        val median221: Double? = df.newAge[0]
    }

    // scenario #3: create new column via expression
    personsDf.groupBy { city }.medianOf("newAgeExpr") { age * 10 }.let { df ->
        df.compareSchemas()

        val median3: Double? = df.newAgeExpr[0]
    }

    return "OK"
}

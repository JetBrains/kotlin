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
    personsDf.groupBy { city }.std().let { df ->
        val std01: Double = df.age[0]
        val std02: Double = df.weight[0]
        df.compareSchemas()
    }

    // scenario #1: particular column
    personsDf.groupBy { city }.stdFor { age }.let { df ->
        val std11: Double = df.age[0]
        df.compareSchemas()
    }

    // scenario #1.1: particular column via std
    personsDf.groupBy { city }.std { age }.let { df ->
        val std111: Double = df.age[0]
        df.compareSchemas()
    }

    // scenario #1.2: multiple columns via std
    personsDf.groupBy { city }.std { age and yearsToRetirement }.let { df ->
        val std111: Double = df.std[0]
        df.compareSchemas()
    }

    // scenario #2: particular column with new name - schema changes
    // TODO: not supported scenario
    // val res2 = personsDf.groupBy { city }.std("age", name = "newAge")
    // val std21: Double = res2.newAge[0]

    // scenario #2.1: particular column with new name - schema changes but via columnSelector
    personsDf.groupBy { city }.std("newAge") { age }.let { df ->
        val std211: Double = df.newAge[0]
        df.compareSchemas()
    }

    // scenario #2.2: two columns with new name - schema changes but via columnSelector
    personsDf.groupBy { city }.std("newAge") { age and yearsToRetirement }.let { df ->
        val std221: Double = df.newAge[0]
        df.compareSchemas()
    }

    // scenario #3: create new column via expression
    personsDf.groupBy { city }.stdOf("newAge") { age * 10 }.let { df ->
        val std3: Double = df.newAge[0]
        df.compareSchemas()
    }

    return "OK"
}


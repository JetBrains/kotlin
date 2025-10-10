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

    // scenario #0: all numerical columns
    personsDf.groupBy { city }.sum().let { df ->
        df.compareSchemas()

        val sum01: Int = df.age[0]
        val sum02: Double = df.weight[0]
        val sum03: Float = df.yearsToRetirement[0]
        val sum04: Int = df.workExperienceYears[0]
        val sum05: Int = df.dependentsCount[0]
        val sum06: Long = df.annualIncome[0]
    }

    // scenario #1: particular column
    personsDf.groupBy { city }.sumFor { annualIncome }.let { df ->
        val sum11: Long = df.annualIncome[0]
        df.compareSchemas()
    }

    // scenario #1.1: particular column via sum
    personsDf.groupBy { city }.sum { weight }.let { df ->
        val sum111: Double = df.weight[0]
        df.compareSchemas()
    }

    // scenario #1.2: multiple columns via sum
    personsDf.groupBy { city }.sum { age and age }.let { df ->
        val sum121: Int = df.sum[0]
        df.compareSchemas()
    }

    // scenario #1.3: multiple columns via sum with number unification, Int + Float => Double
    personsDf.groupBy { city }.sum { age and yearsToRetirement }.let { df ->
        val sum121: Double = df.sum[0]
        df.compareSchemas()
    }

    // scenario #2: particular column with new name - schema changes
    // TODO: not supported scenario for String API
    // val res2 = personsDf.groupBy { city }.sum("age", name = "newAge")
    // val sum21: Int = res2.newAge[0]

    // scenario #2.1: particular column with new name - schema changes but via columnSelector
    personsDf.groupBy { city }.sum("newAnnualIncome") { annualIncome }.let { df ->
        val sum211: Long = df.newAnnualIncome[0]
        df.compareSchemas()
    }

    // scenario #2.2: two columns with new name via sum with number unification, Int + Float => Double
    personsDf.groupBy { city }.sum("newSum") { age and yearsToRetirement }.let { df ->
        val sum221: Double = df.newSum[0]
        df.compareSchemas()
    }

    // scenario #3: create new column via expression
    personsDf.groupBy { city }.sumOf("newAge") { age * 10 }.let { df ->
        val sum3: Int = df.newAge[0]
    }

    // scenario #3.1: create new column via expression on Double column
    personsDf.groupBy { city }.sumOf("newAge") { weight * 10 }.let { df ->
        val sum31: Double = df.newAge[0]
        df.compareSchemas()
    }

    val df = dataFrameOf("a")(1, 2, 3)
    df.groupBy { a named "b" }.sum { a }.let { df ->
        df.compareSchemas()

        val sum41: Int = df.a[0]
    }

    return "OK"
}

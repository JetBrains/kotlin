import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*
import java.math.BigInteger

fun box(): String {
    // multiple columns
    val personsDf = dataFrameOf(
        "name", "age", "city", "weight", "height", "yearsToRetirement", "bigNumber",
    )(
        "Alice", 15, "London", 99.5, "1.85", 50f, BigInteger.valueOf(23L),
        "Bob", 20, "Paris", 140.0, "1.35", 45f, BigInteger.valueOf(12L),
        "Charlie", 100, "Dubai", 75.0, "1.95", 0f, BigInteger.valueOf(68798L),
        "Rose", 1, "Moscow", 45.33, "0.79", 64f, BigInteger.valueOf(46556L),
        "Dylan", 35, "London", 23.4, "1.83", 30f, BigInteger.valueOf(235L),
        "Eve", 40, "Paris", 56.72, "1.85", 25f, BigInteger.valueOf(-23534L),
        "Frank", 55, "Dubai", 78.9, "1.35", 10f, BigInteger.valueOf(235L),
        "Grace", 29, "Moscow", 67.8, "1.65", 36f, BigInteger.valueOf(0L),
        "Hank", 60, "Paris", 80.22, "1.75", 5f, BigInteger.valueOf(-4L),
        "Isla", 22, "London", 75.1, "1.85", 43f, BigInteger.valueOf(2345L),
    )

    // scenario #0: all numerical columns
    personsDf.groupBy { city }.max().let { df ->
        val max01: Int? = df.age[0]
        val max02: Double? = df.weight[0]
        df.compareSchemas()
    }

    // scenario #1: particular column
    personsDf.groupBy { city }.maxFor { age }.let { df ->
        val max11: Int? = df.age[0]
        df.compareSchemas()
    }

    // scenario #1.1: particular column via max
    personsDf.groupBy { city }.max { age }.let { df ->
        val max111: Int? = df.age[0]
        df.compareSchemas()
    }

    // scenario #1.2: multiple columns via max
    personsDf.groupBy { city }.max { age and age }.let { df ->
        val max111: Int? = df.max[0]
        df.compareSchemas()
    }

    // scenario #2: particular column with new name - schema changes
    // TODO: not supported scenario
    // val res2 = personsDf.groupBy { city }.max("age", name = "newAge")
    // val max21: Int? = res2.newAge[0]

    // scenario #2.1: particular column with new name - schema changes but via columnSelector
    personsDf.groupBy { city }.max("newAge") { age }.let { df ->
        val max211: Int? = df.newAge[0]
        df.compareSchemas()
    }

    // scenario #2.2: two columns with new name - schema changes but via columnSelector
    personsDf.groupBy { city }.max("newAge") { age and age }.let { df ->
        val max221: Int? = df.newAge[0]
        df.compareSchemas()
    }

    // scenario #3: create new column via expression
    personsDf.groupBy { city }.maxOf("newAge") { age / 10 }.let { df ->
        val max3: Int? = df.newAge[0]
        df.compareSchemas()
    }

    return "OK"
}


import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
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
    personsDf.groupBy { city }.min().let { df ->
        df.compareSchemas()

        val min01: Int? = df.age[0]
        val min02: Double? = df.weight[0]
        val min03: Int? = df.yearsToRetirement[0]
        val min04: Short? = df.workExperienceYears[0]
        val min05: Byte? = df.dependentsCount[0]
        val min06: Long? = df.annualIncome[0]
        val min07: String? = df.name[0]
        val min08: String? = df.city[0]
        val min09: String? = df.height[0]
    }

    // scenario #1: particular column
    val res1 = personsDf.groupBy { city }.minFor { age }
    val min11: Int? = res1.age[0]
    res1.compareSchemas()
    personsDf.groupBy { city }.minFor { age }.let { df ->
        val min11: Int? = df.age[0]
        df.compareSchemas()
    }

    // scenario #1.1: particular column via min
    val res11 = personsDf.groupBy { city }.min { dependentsCount }
    val min111: Byte? = res11.dependentsCount[0]
    res11.compareSchemas()
    personsDf.groupBy { city }.min { dependentsCount }.let { df ->
        val min111: Byte? = df.dependentsCount[0]
        df.compareSchemas()
    }

    // scenario #2: particular column with new name - schema changes
    // TODO: not supported scenario
    // val res2 = personsDf.groupBy { city }.min("age", name = "newAge")
    // val min21: Int? = res2.newAge[0]

    // scenario #2.1: particular column with new name - schema changes but via columnSelector
    personsDf.groupBy { city }.min("newName") { name }.let { df ->
        val min211: String? = df.newName[0]
        df.compareSchemas()
    }

    // scenario #2.2: two columns with new name - schema changes but via columnSelector
    // TODO: handle multiple columns https://github.com/Kotlin/dataframe/issues/1090
    personsDf.groupBy { city }.min("newAge") { age and yearsToRetirement }.let { df ->
        val min221: Int? = df.newAge[0]
        df.compareSchemas()
    }

    // scenario #3: create new column via expression
    personsDf.groupBy { city }.minOf("newAnnualIncome") { annualIncome / 2 }.let { df ->
        val min3: Long? = df.newAnnualIncome[0]
        df.compareSchemas()
    }

    return "OK"
}


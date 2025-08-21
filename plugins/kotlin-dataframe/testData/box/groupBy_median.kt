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

    // scenario #0: all intraComparable columns
    val res0 = personsDf.groupBy { city }.median()
    res0.compareSchemas()

    val median01: Double? = res0.age[0]
    val median02: Double? = res0.weight[0]
    val median03: String? = res0.height[0]

    // scenario #1: particular column
    val res1 = personsDf.groupBy { city }.medianFor { age }
    res1.compareSchemas()

    val median11: Double? = res1.age[0]

    // scenario #1.1: particular column via median
    val res11 = personsDf.groupBy { city }.median { age }
    res11.compareSchemas()

    val median111: Double? = res11.age[0]

    // scenario #2: particular column with new name - schema changes
    // TODO: not supported scenario
    // val res2 = personsDf.groupBy { city }.median("age", name = "newAge")
    // val median21: Int? = res2.newAge[0]

    // scenario #2.1: particular column with new name - schema changes but via columnSelector
    val res21 = personsDf.groupBy { city }.median("newAge") { age }
    res21.compareSchemas()

    val median211: Double? = res21.newAge[0]

    // scenario #2.2: two columns with new name - schema changes but via columnSelector
    // TODO: handle multiple columns https://github.com/Kotlin/dataframe/issues/1090
    val res22 = personsDf.groupBy { city }.median("newAge") { age and yearsToRetirement }
    res22.compareSchemas()

    val median221: Double? = res22.newAge[0]

    // scenario #3: create new column via expression
    val res3 = personsDf.groupBy { city }.medianOf("newAgeExpr") { age * 10 }
    res3.compareSchemas()

    val median3: Double? = res3.newAgeExpr[0]

    return "OK"
}

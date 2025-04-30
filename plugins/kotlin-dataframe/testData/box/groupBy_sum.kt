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
    val res0 = personsDf.groupBy { city }.sum()
    res0.compareSchemas()

    val sum01: Int? = res0.age[0]
    val sum02: Double? = res0.weight[0]
    val sum03: Int? = res0.yearsToRetirement[0]
    val sum04: Int? = res0.workExperienceYears[0]
    val sum05: Int? = res0.dependentsCount[0]
    val sum06: Long? = res0.annualIncome[0]

    // scenario #1: particular column
    val res1 = personsDf.groupBy { city }.sumFor { annualIncome }
    val sum11: Long? = res1.annualIncome[0]
    res1.compareSchemas()

    // scenario #1.1: particular column via sum
    val res11 = personsDf.groupBy { city }.sum { weight }
    val sum111: Double? = res11.weight[0]
    res11.compareSchemas()

    // scenario #2: particular column with new name - schema changes
    // TODO: not supported scenario for String API
    // val res2 = personsDf.groupBy { city }.sum("age", name = "newAge")
    // val sum21: Int? = res2.newAge[0]

    // scenario #2.1: particular column with new name - schema changes but via columnSelector
    val res21 =  personsDf.groupBy { city }.sum("newAnnualIncome") { annualIncome }
    val sum211: Long? = res21.newAnnualIncome[0]
    res21.compareSchemas()

    // scenario #2.2: two columns with new name - schema changes but via columnSelector
    // TODO: handle multiple columns https://github.com/Kotlin/dataframe/issues/1090
    val res22 = personsDf.groupBy { city }.sum("newAge") { age and yearsToRetirement }
    val sum221: Int? = res22.newAge[0]
    res22.compareSchemas()

    // scenario #3: create new column via expression
    val res3 = personsDf.groupBy { city }.sumOf("newAge") { age * 10 }
    val sum3: Int? = res3.newAge[0]

    // scenario #3.1: create new column via expression on Double column
    val res31 = personsDf.groupBy { city }.sumOf("newAge") { weight * 10 }
    val sum31: Double? = res31.newAge[0]
    res31.compareSchemas()

    val df = dataFrameOf("a")(1, 2, 3)
    val res41 = df.groupBy { a named "b" }.sum { a }
    res41.compareSchemas()

    val sum41: Int = res41.a[0]

    return "OK"
}

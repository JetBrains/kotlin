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
    val res0 = personsDf.sum()
    res0.df().compareSchemas()

    val sum01: Int? = res0.age
    val sum02: Double? = res0.weight
    val sum03: Int? = res0.yearsToRetirement
    val sum04: Int? = res0.workExperienceYears
    val sum05: Int? = res0.dependentsCount
    val sum06: Long? = res0.annualIncome

    // scenario #1: particular column
    val res1 = personsDf.sumFor { age }
    res1.df().compareSchemas()

    val sum11: Int? = res1.age

    // scenario #1.1: particular column with converted type
    val res11 = personsDf.sumFor { dependentsCount }
    res11.df().compareSchemas()

    val sum111: Int? = res11.dependentsCount

    // scenario #2: sum of values per columns separately
    val res3 = personsDf.sumFor { age and weight and workExperienceYears and dependentsCount and annualIncome }
    res3.df().compareSchemas()

    val sum31: Int? = res3.age
    val sum32: Double? = res3.weight
    val sum33: Int? = res3.workExperienceYears
    val sum34: Int? = res3.dependentsCount
    val sum35: Long? = res3.annualIncome

    return "OK"
}

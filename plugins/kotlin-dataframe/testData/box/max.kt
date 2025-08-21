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
    val res0 = personsDf.max()
    res0.df().compareSchemas()

    val max01: Int? = res0.age
    val max02: Double? = res0.weight
    val max03: Int? = res0.yearsToRetirement
    val max04: Short? = res0.workExperienceYears
    val max05: Byte? = res0.dependentsCount
    val max06: Long? = res0.annualIncome
    val max07: String? = res0.name
    val max08: String? = res0.city
    val max09: String? = res0.height

    // scenario #1: particular column
    val res1 = personsDf.maxFor { age }
    res1.df().compareSchemas()

    val max11: Int? = res1.age

    // scenario #1.1: particular column with converted type
    val res11 = personsDf.maxFor { dependentsCount }
    res11.df().compareSchemas()

    val max111: Byte? = res11.dependentsCount

    // scenario #2: max of values per columns separately
    val res3 = personsDf.maxFor<_, String> { name and weight and workExperienceYears and dependentsCount and annualIncome }
    res3.df().compareSchemas()

    val max31: String? = res3.name
    val max32: Double? = res3.weight
    val max33: Short? = res3.workExperienceYears
    val max34: Byte? = res3.dependentsCount
    val max35: Long? = res3.annualIncome

    return "OK"
}

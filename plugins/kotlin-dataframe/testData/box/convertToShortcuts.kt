import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*
import kotlinx.datetime.*

fun box(): String {
    val daysToStandardMillis = 24 * 60 * 60 * 1000L * 366
    val df = dataFrameOf("a")(60L * 1000L + daysToStandardMillis).convert { a }.toLocalDateTime(TimeZone.UTC)
    val localDateTime: LocalDateTime = df.a[0]

    val df1 = dataFrameOf("a")(60L * 1000L + daysToStandardMillis, null).convert { a }.toLocalDateTime(TimeZone.UTC)
    val localDateTime1: LocalDateTime? = df1.a[0]

    val df2 = dataFrameOf("a")(123).convert { a }.toStr()
    val str: String = df2.a[0]

    val df3 = dataFrameOf("a")(123, null).convert { a }.toStr()
    df3.compareSchemas(strict = true)
    return "OK"
}

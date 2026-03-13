import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf("age" to columnOf(1))

    df.insert("year of birth") { 2021 - age }.under { pathOf("new", "group") }.let { df1 ->
        val v1: Int = df1[0].age
        val v2: Int = df1[0].new.group.`year of birth`
        df1.compareSchemas(strict = true)
    }
    return "OK"
}

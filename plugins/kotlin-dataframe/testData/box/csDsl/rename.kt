import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df1 = df.select { expr { age } into "age2" }
    val i1: Int = df1.age2[0]

    val df2 = dataFrameOf("lists")(listOf(1, 2), listOf(3)).explode { lists into "int" }
    val i2: Int = df2.int[0]

    df.select { age named "age2" }.compareSchemas(strict = true)

    return "OK"
}

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf(
        "group" to columnOf("b" to columnOf(1)),
    )

    val res = df.gather { group }
        .mapValues { 1 }
        .valuesInto("v")

    val value: Int = res[0].v
    res.compareSchemas(strict = true)
    return "OK"
}

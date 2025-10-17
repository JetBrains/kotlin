import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf(
        "a" to columnOf(42, 1, 2, 1, 2),
    ).addId().groupBy { a }.aggregate {
        this into "frameCol"
    }
    val i: Int = df.frameCol[0].a[0]
    df.compareSchemas(strict = true)
    return "OK"
}

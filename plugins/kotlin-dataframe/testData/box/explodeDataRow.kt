
import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf("a", "b")(listOf(100, 113, 140), listOf(400, 410)).first().explode { a }
    val i: Int = df.a[0]
    val l: List<Int> = df.b[0]
    df.compareSchemas(strict = true)
    return "OK"
}

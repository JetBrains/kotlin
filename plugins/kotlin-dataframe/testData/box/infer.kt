import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf("a")(123)
        .add("b", infer = Infer.Nulls) { 123 }
        .convert { b }.with(infer = Infer.None) { it.toString() }
        .select { a and b and expr(infer = Infer.Nulls) { 42 } }

    val b: String = df.b[0]
    val untitled: Int = df.untitled[0]
    return "OK"
}

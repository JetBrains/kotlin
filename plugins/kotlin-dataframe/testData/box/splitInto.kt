import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df1 = dataFrameOf("a" to listOf(1, 2)).split { a }.by { List(it) { "123" } }.into("a", "b")
    val col1: DataColumn<String?> = df1.a
    df1.compareSchemas()
    return "OK"
}

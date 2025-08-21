import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df2 = dataFrameOf("a" to listOf(1)).split { a }.by { listOf(1, 2) }.inward("a", "b")
    val col2: DataColumn<Int?> = df2.a.b
    df2.compareSchemas()
    return "OK"
}

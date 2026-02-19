import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df4 = dataFrameOf("a" to listOf("abc")).split { a }.by { it.asIterable() }.inplace()
    val listCol: DataColumn<List<Char>> = df4.a
    return "OK"
}

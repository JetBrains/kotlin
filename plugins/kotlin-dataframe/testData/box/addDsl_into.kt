import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df1 = dataFrameOf("myCol" to columnOf("str"))
    val df = dataFrameOf("a" to columnOf(42)).add {
        expr { a - 1 } into "test"
        expr { df1 } into "frameCol"
        expr { df1.single() } into "columnGroup"
    }
    val res: DataColumn<Int> = df.test
    val res1: DataColumn<String> = df.frameCol[0].myCol
    val res2: DataColumn<String> = df.columnGroup.myCol
    df.compareSchemas(strict = true)
    return "OK"
}

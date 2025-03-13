import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf("a")(1).addId()
    val i: DataColumn<Int> = df.id
    val i1: DataColumn<Int> = dataFrameOf("a")(1).addId("i").i
    return "OK"
}

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val res = dataFrameOf("a")(1, 2, 3).convert { a }.asColumn { it.convertToString() }
    val str: DataColumn<String> = res.a

    val res1 = dataFrameOf("a")(1).convert { a }.asColumn { dataFrameOf("b", "c")(2, 3.0).asColumnGroup() }
    val i: DataColumn<Int> = res1.a.b
    val d: DataColumn<Double> = res1.a.c
    return "OK"
}

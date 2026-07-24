import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val inputDf = dataFrameOf("a" to columnOf(1, 1, 2), "b" to columnOf(2, 2, 1), "c" to columnOf(1, 2, 1))

    val df = inputDf.groupBy { a }.countDistinct()
    val i: Int = df.countDistinct[0]

    val df1 = inputDf.groupBy { a }.countDistinct { b }
    val i1: Int = df1.countDistinct[0]

    val df2 = inputDf.groupBy { a }.countDistinct("myCol") { b }
    val i2: Int = df2.myCol[0]

    return "OK"
}

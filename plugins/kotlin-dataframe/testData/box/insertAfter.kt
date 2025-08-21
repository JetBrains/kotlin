import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*

fun box(): String {
    val df = dataFrameOf("a", "b", "c")(1, 2, 3, 4, 5, 6)

    val dfWithD = df.insert("d") { b * c }.after { a }

    val dCol: DataColumn<Int> = dfWithD.d

    return "OK"
}

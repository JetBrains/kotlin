import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*

fun box(): String {
    val df = dataFrameOf("a", "b", "c")(1, 2, 3, 4, 5, 6)
    val grouped = df.group { a and b and c }.into("g")

    val dfWithD = grouped.insert("d") { g.b  * g.c }.before { g.b }
    dfWithD.checkCompileTimeSchemaEqualsRuntime()

    val dCol: DataColumn<Int> = dfWithD.g.d

    return "OK"
}

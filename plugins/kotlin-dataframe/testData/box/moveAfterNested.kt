import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*

fun box(): String {
    val df = dataFrameOf("a", "b", "c")(1, 2, 3, 4, 5, 6)
    val grouped = df.group { a and b }.into("g")

    val dfMovedAfterNested = grouped.move { c }.after { g.a }
    val cNested: DataColumn<Int> = dfMovedAfterNested.g.c

    return "OK"
}

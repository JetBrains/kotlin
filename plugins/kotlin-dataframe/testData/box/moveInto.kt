import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf("a", "b", "c")(1, 2, 3)
        .group { a and b }.into("d")
        .move { d.a }.into("renamedA")
    df.renamedA
    df.compareSchemas(strict = true)
    return "OK"
}

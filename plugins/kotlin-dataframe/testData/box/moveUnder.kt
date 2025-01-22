import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf("a", "b", "c")(1, 2, 3)
        .group { a and b }.into("d")
        .move { c }.under { d }
    df.compareSchemas(strict = true)

    // alias for group into
    dataFrameOf("a", "b", "c")(1, 2, 3)
        .move { a and b }.under("d")
        .compareSchemas(strict = true)
    return "OK"
}

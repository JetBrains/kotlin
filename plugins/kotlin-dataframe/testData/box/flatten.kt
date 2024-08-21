import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf("a", "b", "c", "d")(1, 2, 3, 4)
    val grouped = df
        .group { a and b }.into("e")
        .group { e and c }.into("f")

    grouped.flatten().compareSchemas(strict = true)
    val flattened = grouped.flatten { f.e }
    flattened.compareSchemas(strict = true)
    flattened.ungroup { f }.compareSchemas(strict = true)

    grouped.flatten { f.e and f }.compareSchemas(strict = true)
    return "OK"
}





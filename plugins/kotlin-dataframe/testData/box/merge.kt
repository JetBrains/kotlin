import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val merge = dataFrameOf("a", "b")(1, null).merge { a and b }
    merge.into("b").compareSchemas(strict = true)
    merge.by { it }.into("b").compareSchemas(strict = true)
    merge.notNull().into("b").compareSchemas(strict = true)
    merge.notNull().by { it }.into("b").compareSchemas(strict = true)
    return "OK"
}

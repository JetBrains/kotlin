import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf("a", "b", "c")(1, 2, 3)
        .group { a and b }.into("d")
        .move { c }.under { d }
    df.checkCompileTimeSchemaEqualsRuntime()

    // alias for group into
    dataFrameOf("a", "b", "c")(1, 2, 3)
        .move { a and b }.under("d")
        .checkCompileTimeSchemaEqualsRuntime()

    // new group by path
    dataFrameOf("a", "b", "c")(1, 2, 3)
        .move { a and b }.under { pathOf("new", "group") }
        .let { df ->
            df.new.group.a
            df.new.group.b
            df.checkCompileTimeSchemaEqualsRuntime()
        }

    return "OK"
}

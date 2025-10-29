import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf("a", "b", "c")(1, 2, 3, 4, 5, 6)
    df.move { c and a }.toStart().let {
        it.checkCompileTimeSchemaEqualsRuntime()
    }
    df.moveToStart { c and a }.let {
        it.checkCompileTimeSchemaEqualsRuntime()
    }

    val dfGrouped = df.group { a and b }.into("d")

    dfGrouped.move { d.b }.toStart(true).let {
        it.checkCompileTimeSchemaEqualsRuntime()
        it.c
        it.d
        it.d.b
        it.d.a
    }
    dfGrouped.moveToStart(true) { d.b }.let {
        it.checkCompileTimeSchemaEqualsRuntime()
        it.c
        it.d
        it.d.b
        it.d.a
    }

    dfGrouped.move { d.b }.toStart().let {
        it.checkCompileTimeSchemaEqualsRuntime()
        it.c
        it.d
        it.b
        it.d.a
    }
    dfGrouped.moveToStart { d.b }.let {
        it.checkCompileTimeSchemaEqualsRuntime()
        it.c
        it.d
        it.b
        it.d.a
    }

    return "OK"
}

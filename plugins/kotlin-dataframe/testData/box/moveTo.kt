import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf("a", "b", "c")(1, 2, 3, 4, 5, 6)
    df.move { c and a }.to(3).let {
        it.checkCompileTimeSchemaEqualsRuntime()
    }
    df.moveTo(3) { c and a }.let {
        it.checkCompileTimeSchemaEqualsRuntime()
    }

    val dfGrouped = df.group { a and b }.into("d")

    dfGrouped.move { d.b }.to(0, true).let {
        it.checkCompileTimeSchemaEqualsRuntime()
        it.c
        it.d
        it.d.b
        it.d.a
    }
    dfGrouped.moveTo(0, true) { d.b }.let {
        it.checkCompileTimeSchemaEqualsRuntime()
        it.c
        it.d
        it.d.b
        it.d.a
    }

    dfGrouped.move { d.b }.to(0).let {
        it.checkCompileTimeSchemaEqualsRuntime()
        it.c
        it.d
        it.b
        it.d.a
    }
    dfGrouped.moveTo(0) { d.b }.let {
        it.checkCompileTimeSchemaEqualsRuntime()
        it.c
        it.d
        it.b
        it.d.a
    }

    return "OK"
}

// CHECK_TYPE_WITH_EXACT

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.columns.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val nested = columnOf(
        dataFrameOf("group" to columnOf("x" to columnOf(1), "y" to columnOf("a"))),
        dataFrameOf("group" to columnOf("x" to columnOf(2))),
    )
    checkExactType<Int>(nested[0].group.x[0])
    dataFrameOf("frames" to nested).checkCompileTimeSchemaEqualsRuntime()

    val df = dataFrameOf("id" to columnOf(1, 2), "frames" to col)
    checkExactType<Int>(df.frames[0].a[0])
    df.checkCompileTimeSchemaEqualsRuntime()
    return "OK"
}

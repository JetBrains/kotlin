// CHECK_TYPE_WITH_EXACT

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.columns.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val col = columnOf(
        dataFrameOf("a" to columnOf(1), "b" to columnOf("x")),
        dataFrameOf("a" to columnOf(2)),
    )
    checkExactType<Int>(col[0].a[0])
    checkExactType<Int>(col[1].a[0])
    dataFrameOf("frames" to col).checkCompileTimeSchemaEqualsRuntime()

    val df = dataFrameOf("id" to columnOf(1, 2), "frames" to col)
    checkExactType<Int>(df.frames[0].a[0])
    df.checkCompileTimeSchemaEqualsRuntime()
    return "OK"
}

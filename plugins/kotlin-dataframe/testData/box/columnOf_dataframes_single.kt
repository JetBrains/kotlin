// CHECK_TYPE_WITH_EXACT

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.columns.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val single = columnOf(dataFrameOf("a" to columnOf(1)))
    checkExactType<Int>(single[0].a[0])
    dataFrameOf("frames" to single).checkCompileTimeSchemaEqualsRuntime()
    return "OK"
}

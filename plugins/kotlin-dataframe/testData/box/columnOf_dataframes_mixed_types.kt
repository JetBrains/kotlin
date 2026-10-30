// CHECK_TYPE_WITH_EXACT

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.columns.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val commonType = columnOf(
        dataFrameOf("value" to columnOf(1)),
        dataFrameOf("value" to columnOf(2.0)),
    )
    checkExactType<Number>(commonType[0].value[0])
    dataFrameOf("frames" to commonType).checkCompileTimeSchemaEqualsRuntime()
    return "OK"
}

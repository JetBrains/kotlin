// CHECK_TYPE_WITH_EXACT

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.columns.*
import org.jetbrains.kotlinx.dataframe.io.*

@DataSchema
data class A(val i: Int)

fun box(): String {
     val df = dataFrameOf(
        "frame" to columnOf(
            dataFrameOf(A(123)),
            dataFrameOf(A(321)),
        )
    )
    df.frame.map { it.i }
    checkExactType<Int>(df[0].frame[0].i)
    dataFrameOf("frames" to single).checkCompileTimeSchemaEqualsRuntime()
    return "OK"
}

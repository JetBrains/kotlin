// CHECK_TYPE_WITH_EXACT

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val schema = dataFrameOf("b" to columnOf(42))
    val pair = 1 to schema.first()
    val pair1 = 2 to null
    val df = dataFrameOf("pairs" to columnOf(pair, pair1))

    df.unfold { pairs }.let { res ->
        checkExactType<Int?>(res[0].pairs.second.b)
    }
    return "OK"
}

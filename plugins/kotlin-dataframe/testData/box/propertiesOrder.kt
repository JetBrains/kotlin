// FIR_DUMP

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.io.read
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.annotations.*

fun box(): String {
    val df = dataFrameOf("full_name", "a", "b")(1, 2, 3)
    df.full_name
    return "OK"
}

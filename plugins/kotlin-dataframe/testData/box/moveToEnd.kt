// FIR_DUMP
import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf("s")("str").addId().move { id }.toEnd()
    val df1 = dataFrameOf("s")("str").addId().moveToEnd { id }
    return "OK"
}

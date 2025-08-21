// FIR_DUMP
import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf("s")("str").add("l") { s.length }.move { l }.toStart()
    val df1 = dataFrameOf("s")("str").add("l") { s.length }.moveToStart { l }
    return "OK"
}

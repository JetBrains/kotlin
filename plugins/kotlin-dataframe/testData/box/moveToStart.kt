import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf("s")("str").add("l") { s.length }.move { l }.toStart()
    df.checkCompileTimeSchemaEqualsRuntime()
    val df1 = dataFrameOf("s")("str").add("l") { s.length }.moveToStart { l }
    df1.checkCompileTimeSchemaEqualsRuntime()
    return "OK"
}

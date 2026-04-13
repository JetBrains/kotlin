import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

data class Container<T>(val value: T)

fun box(): String {
    val df = listOf(Container(1)).toDataFrame()
    val i: Int = df[0].value
    return "OK"
}

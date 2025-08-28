import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

private inline fun <reified T> convert(l: List<T>) = l.<!DATAFRAME_PLUGIN_NOT_YET_SUPPORTED_IN_INLINE!>toDataFrame<!>()

fun box(): String {
    convert(listOf(1, 2, 3))
    return "OK"
}

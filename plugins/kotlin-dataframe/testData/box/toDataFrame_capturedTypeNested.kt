import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

class Holder<T>(val items: List<T>)
class Wrapper(val h: Holder<*>)

fun box(): String {
    val df = listOf(Wrapper(Holder(listOf(1)))).toDataFrame(maxDepth = 2)
    df.assert()
    return "OK"
}

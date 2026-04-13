import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val schema = dataFrameOf("b" to columnOf(42))
    val df = dataFrameOf("pairs" to columnOf(schema to "str"))

    df.unfold { pairs }.let { res ->
        val b1: Int = res[0].pairs.first[0].b
        val str: String = res[0].pairs.second
    }
    return "OK"
}

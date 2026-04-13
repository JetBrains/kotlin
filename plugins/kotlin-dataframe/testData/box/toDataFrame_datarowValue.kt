import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val schema = dataFrameOf("b" to columnOf(42))
    val df = dataFrameOf("pairs" to columnOf("abc" to schema.first()))

    df.unfold { pairs }.let { res ->
        val str: String = res[0].pairs.first
        val b2: Int = res[0].pairs.second.b
    }
    return "OK"
}

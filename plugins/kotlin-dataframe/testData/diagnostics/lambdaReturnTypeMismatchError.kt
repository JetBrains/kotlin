import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun test() {
    val df = dataFrameOf("a" to columnOf(1, null))
    df.select { <!RETURN_TYPE_MISMATCH!>"a"<!> }
}

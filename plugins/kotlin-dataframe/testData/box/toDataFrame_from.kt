import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

class S(
    val str: String,
)

fun box(): String {
    val res = listOf(S("123")).toDataFrame {
        properties(maxDepth = 1)
        "col" from { it.str }
    }
    val s: String = res[0].col
    val s1: String = res[0].str
    res.compareSchemas(strict = true)
    return "OK"
}

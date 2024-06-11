import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*

class S(
    val str: String,
)

fun box(): String {
    val res = listOf(S("123")).toDataFrame {
        properties(maxDepth = 1)
        "col" from { it.str }
    }
    res.col.print()
    res.str.print()
    return "OK"
}

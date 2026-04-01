import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

class KotlinRecord {
    fun getI(): Int {
        return 1
    }
}

fun box(): String {
    val df = listOf(KotlinRecord()).toDataFrame(maxDepth = 2)
    val i: Int = df.i[0]
    df.compareSchemas(strict = true)
    return "OK"
}

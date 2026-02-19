import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val rawDf = DataFrame.readJsonStr("""[{"a":1,"b":2,"c":3,"group":{"x":10}}]""")

    rawDf.select { col<Int>(0) named "aNew" and (col(1) named "bNew") }.let {
        val v1: Int = it[0].aNew
        val v2: Any? = it[0].bNew
        it.compareSchemas()
    }

    return "OK"
}
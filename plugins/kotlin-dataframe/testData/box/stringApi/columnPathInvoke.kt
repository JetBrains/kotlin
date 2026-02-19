import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val rawDf = DataFrame.readJsonStr("""[{"parent":{"child":1,"nested":{"deep":42}},"a":10,"b":20}]""")
    rawDf.select { "parent"["child"]<Int>() and "parent"["nested"]["deep"]<Int>() }.let {
        val v1: Int = it[0].child
        val v2: Int = it[0].deep
        it.compareSchemas(strict = true)
    }

    rawDf.convert { "parent"["nested"]["deep"]<Int>() }.with { it.toString () }.let {
        val v: String = it[0].parent.nested.deep
        it.compareSchemas()
    }

    return "OK"
}
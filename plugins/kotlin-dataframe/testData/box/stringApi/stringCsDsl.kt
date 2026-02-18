import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val rawDf = DataFrame.readJsonStr("""[{"a":1,"b":2,"c":3,"group":{"x":10,"y":20}}]""")

    // ColByString
    rawDf.select { col<Int>("a") }.let {
        val v: Int = it[0].a
        it.compareSchemas()
    }

    // ColByStringUntyped
    rawDf.select { col("b") }.let {
        val v: Any? = it[0].b
        it.compareSchemas()
    }

    // StringNestedColUntyped
    rawDf.select { "group".col("x") }.let {
        val v: Any? = it[0].x
        it.compareSchemas()
    }

    // StringNestedCol
    rawDf.select { "group".col<Int>("x") }.let {
        val v: Int = it[0].x
        it.compareSchemas()
    }

    // ColumnPathColUntyped
    rawDf.select { pathOf("group").col("x") }.let {
        val v: Any? = it[0].x
        it.compareSchemas()
    }

    // ColumnPathCol
    rawDf.select { pathOf("group").col<Int>("x") }.let {
        val v: Int = it[0].x
        it.compareSchemas()
    }

    // StringSelect
    rawDf.select { "group".select { col<Int>("x") } }.let {
        val v: Int = it[0].x
        it.compareSchemas()
    }

    // ColumnPathSelect
    rawDf.select { pathOf("group").select { col<Int>("x") and col<Int>("y") } }.let {
        val v1: Int = it[0].x
        val v2: Int = it[0].y
        it.compareSchemas()
    }

    // PathOf
    DataFrame.readJsonStr("""[{"parent":{"child":1,"nested":{"deep":42}},"a":10,"b":20}]""")
        .select { pathOf("parent", "nested", "deep") }.let {
            val v: Any? = it[0].deep
            it.compareSchemas()
        }

    return "OK"
}
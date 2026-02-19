import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

data class Nested(val i: Double?)

data class Record(val a: String?, val b: Int?, val nested: Nested)

fun box(): String {
    val df = listOf(Record("112", 42, Nested(3.0))).toDataFrame(maxDepth = 1)
    df.dropNulls { a and b }.let { res ->
        val col1: DataColumn<String> = res.a
        val col2: DataColumn<Int> = res.b
    }

    // dropNulls whereAllNull = true sometimes doesn't have any effect on nullability of columns

    val df1 = dataFrameOf("a" to columnOf(null, 1), "b" to columnOf("str", null))
    df1.dropNulls(whereAllNull = true) { a and b }.let { res ->
        val schema = res.compileTimeSchema()
        assert(schema.column("a").nullable)
        assert(schema.column("b").nullable)

        res.compareSchemas(strict = true)
    }

    df1.dropNulls(whereAllNull = true).let { res ->
        val schema = res.compileTimeSchema()
        assert(schema.column("a").nullable)
        assert(schema.column("b").nullable)

        res.compareSchemas(strict = true)
    }

    return "OK"
}

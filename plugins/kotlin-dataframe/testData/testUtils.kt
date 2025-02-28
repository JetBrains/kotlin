import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*
import org.jetbrains.kotlinx.dataframe.schema.DataFrameSchema

inline fun <reified T> DataFrame<T>.compareSchemas(strict: Boolean = false) {
    val schema = schema()
    val compileTimeSchema = compileTimeSchema()
    val compare = compileTimeSchema.compare(schema)
    require(if (strict) compare.isEqual() else compare.isSuperOrEqual()) {
        buildString {
            appendLine("Comparison result: $compare")
            appendLine("Runtime:")
            appendLine(schema.toString())
            appendLine("Compile:")
            appendLine(compileTimeSchema.toString())
        }
    }
}

inline fun <reified T1, reified T2> compareSchemas(df1: DataFrame<T1>, df2: DataFrame<T2>, strict: Boolean = true) {
    val runtime = df1.schema()
    val schema1 = df1.compileTimeSchema()
    val schema2 = df2.compileTimeSchema()
    compare(runtime, listOf(schema1, schema2), strict)
}

inline fun <reified T1, reified T2, reified T3> compareSchemas(df1: DataFrame<T1>, df2: DataFrame<T2>, df3: DataFrame<T3>, strict: Boolean = true) {
    val runtime = df1.schema()
    val schema1 = df1.compileTimeSchema()
    val schema2 = df2.compileTimeSchema()
    val schema3 = df3.compileTimeSchema()
    compare(runtime, listOf(schema1, schema2, schema3), strict)
}

inline fun <reified T1, reified T2, reified T3, reified T4> compareSchemas(df1: DataFrame<T1>, df2: DataFrame<T2>, df3: DataFrame<T3>, df4: DataFrame<T4>, strict: Boolean = true) {
    val runtime = df1.schema()
    val schema1 = df1.compileTimeSchema()
    val schema2 = df2.compileTimeSchema()
    val schema3 = df3.compileTimeSchema()
    val schema4 = df4.compileTimeSchema()
    compare(runtime, listOf(schema1, schema2, schema3, schema4), strict)
}

fun compare(runtime: DataFrameSchema, schemas: List<DataFrameSchema>, strict: Boolean) {
    val schema = schemas.first()
    val compare = runtime.compare(schema)
    require(schemas.zipWithNext().all { (a, b) -> a.compare(b).isEqual() } && if (strict) compare.isEqual() else compare.isSuperOrEqual()) {
        buildString {
            appendLine("Comparison result: $compare")
            appendLine("Runtime:")
            appendLine(runtime.toString())
            schemas.forEachIndexed { i, schema ->
                appendLine("Compile $i")
                appendLine(schema.toString())
            }
        }
    }
}


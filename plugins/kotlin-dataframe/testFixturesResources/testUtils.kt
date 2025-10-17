import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.columns.*
import org.jetbrains.kotlinx.dataframe.io.*
import org.jetbrains.kotlinx.dataframe.schema.*
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf

inline fun <reified T> DataFrame<T>.compareSchemas(strict: Boolean = false) {
    val schema = schema()
    val compileTimeSchema = compileTimeSchema()
    val compare = compileTimeSchema.compare(schema)
    require(if (strict) compare.matches() else compare.isSuperOrMatches()) {
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
    require(
        schemas.zipWithNext().all { (a, b) -> a.compare(b).matches() } &&
                if (strict) compare.matches() else compare.isSuperOrMatches(),
    ) {
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

// Usual DataFrameSchema.compare is either strict comparison where both set of columns and their type must be the same
// or subtype relation where subset of columns can vary.
// This checks that schemas have same set of columns, but compile time columns can be nullable where runtime is narrowed to non-nullable

sealed interface Mismatch
data class AcceptableNullabilityMismatch(val path: ColumnPath, val compile: KType, val runtime: KType) : Mismatch
data class ErrorMismatch(val message: String) : Mismatch

inline fun <reified T> DataFrame<T>.assert(print: Boolean = false): List<Mismatch> {
    val mismatches = mutableListOf<Mismatch>()
    equals(compileTimeSchema(), schema(), mismatches, pathOf())
    if (print) {
        println(mismatches.joinToString("\n"))
    } else if (mismatches.any { it is ErrorMismatch }) {
        error(mismatches.joinToString("\n"))
    }
    return mismatches
}

fun equals(compile: DataFrameSchema, runtime: DataFrameSchema, mismatches: MutableList<Mismatch>, path: ColumnPath) {
    runtime.columns.forEach { name, runtimeColumnSchema ->
        val compileColumnSchema = compile.columns[name]
        if (compileColumnSchema == null) error("No column ${name} found in: ${compile.columns.keys.map { path + it }}")

        when (runtimeColumnSchema) {
            is ColumnSchema.Value -> {
                if (!runtimeColumnSchema.type.isSubtypeOf(compileColumnSchema.type)) {
                    mismatches += ErrorMismatch("$name: ${runtimeColumnSchema.type} is not subtype of ${compileColumnSchema.type}")
                } else if (runtimeColumnSchema.type != compileColumnSchema.type) {
                    mismatches += AcceptableNullabilityMismatch(path + name, compile = compileColumnSchema.type, runtime = runtimeColumnSchema.type)
                }
            }
            is ColumnSchema.Group -> {
                if (compileColumnSchema !is ColumnSchema.Group) {
                    mismatches += ErrorMismatch("$name of ${compileColumnSchema.kind} but Group was expected")
                } else {
                    equals(compileColumnSchema.schema, runtimeColumnSchema.schema, mismatches, path + name)
                }
            }
            is ColumnSchema.Frame -> {
                if (compileColumnSchema !is ColumnSchema.Frame) {
                    mismatches += ErrorMismatch("$name of ${compileColumnSchema.kind} but Frame was expected")
                } else {
                    equals(compileColumnSchema.schema, runtimeColumnSchema.schema, mismatches, path + name)
                }
            }
        }
    }
}

fun DataFrameSchema.column(name: String): ColumnSchema {
    return columns[name] ?: error("Column ${name} not found in schema:\n${this}")
}


import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

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

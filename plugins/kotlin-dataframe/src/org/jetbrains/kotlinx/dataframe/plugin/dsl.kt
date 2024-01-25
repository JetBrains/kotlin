package org.jetbrains.kotlinx.dataframe.plugin

import org.jetbrains.dataframe.impl.codeGen.CodeGenerator
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.KotlinTypeFacade
import org.jetbrains.kotlinx.dataframe.api.schema
import org.jetbrains.kotlinx.dataframe.schema.ColumnSchema

public val KotlinTypeFacade.pluginSchema: DataFrame<*>.() -> PluginDataFrameSchema get() = { schema().toPluginDataFrameSchema() }

public fun DataFrame<*>.generateSchemaDeclaration(
    capitalizedName: String,
    generator: CodeGenerator = CodeGenerator.create(useFqNames = false)
): String = generator.generate(schema(), name = capitalizedName, fields = true, extensionProperties = true, isOpen = true)
    .code.declarations
    .replace(Regex("@JvmName\\(.*\"\\)"), "")

public fun Map<String, ColumnSchema>.accept(
    s: String,
): List<String> {
    var i = 0
    val expressions = mutableListOf<String>()
    fun acceptInt(
        s: String,
        columns: Map<String, ColumnSchema>,
        expressions: MutableList<String>,
    ) {
        columns.forEach { (t, u) ->
            when (u) {
                is ColumnSchema.Frame -> {
                    acceptInt("${s}.$t[0]", u.schema.columns, expressions)
                }

                is ColumnSchema.Value -> {
                    val funName = "col${i}"
                    expressions.add("fun $funName(v: ${u.type}) {}")
                    i++
                    expressions.add("${funName}(${s}.$t[0])")
                }

                is ColumnSchema.Group -> {
                    acceptInt("${s}.$t", u.schema.columns, expressions)
                }
            }
        }
    }
    acceptInt(s, this, expressions)
    return expressions.toList()
}

public fun DataFrame<*>.generateTestCode(): String {
    val expressions = schema().columns.accept("df")
    return expressions.joinToString("\n")
}

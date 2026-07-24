/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin.extensions

import org.jetbrains.kotlin.diagnostics.rendering.Renderer
import org.jetbrains.kotlin.fir.types.renderReadable
import org.jetbrains.kotlinx.dataframe.codeGen.ValidFieldName
import org.jetbrains.kotlinx.dataframe.plugin.extensions.impl.MaterializedSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleCol
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleColumnGroup
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleDataColumn
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleFrameColumn

object DataFrameDiagnosticsRenderers {
    val TO_DATA_SCHEMA_DECLARATION = Renderer { element: MaterializedSchema ->
        element.schema.renderAsKotlin(element.name, element.asDataClass)
    }
}

fun PluginDataFrameSchema.renderAsKotlin(
    rootName: String,
    asDataClass: Boolean = true,
): String = buildString {
    appendLine()
    renderMarker(rootName, columns(), indent = "", asDataClass)
}

private data class Nested(val markerName: String, val cols: List<SimpleCol>)

private fun StringBuilder.renderMarker(
    name: String,
    cols: List<SimpleCol>,
    indent: String,
    asDataClass: Boolean,
) {
    val inner = "$indent    "
    val nested = mutableListOf<Nested>()
    val fieldNames = cols.map {
        ValidFieldName.of(it.name)
    }
    val usedNames = fieldNames.mapTo(mutableSetOf()) {
        it.unquoted
    }
    val fields = cols.map { col ->
        val valid = ValidFieldName.of(col.name)
        val fieldName = valid.quotedIfNeeded
        val columnName = col.name

        val annotation = if (columnName != fieldName) {
            "$inner@ColumnName(\"${escapeStringLiteral(columnName)}\")\n"
        } else ""

        val type = when (col) {
            is SimpleDataColumn -> {
                col.type.coneType.renderReadable()
            }
            is SimpleColumnGroup -> {
                val child = nestedName(col.name, usedNames)
                nested += Nested(child, col.columns())
                child
            }
            is SimpleFrameColumn -> {
                val child = nestedName(col.name, usedNames)
                nested += Nested(child, col.columns())
                "List<$child>"
            }
        }
        annotation to "val $fieldName: $type"
    }

    append(indent).appendLine("@DataSchema")

    if (asDataClass) {
        append(indent).append("data class $name(")
        if (fields.isNotEmpty()) {
            appendLine()
            for ([ann, decl] in fields) {
                append(ann)
                append(inner).append(decl).appendLine(",")
            }
            append(indent)
        }
        append(")")
        if (nested.isNotEmpty()) {
            appendLine(" {")
            nested.forEachIndexed { i, n ->
                renderMarker(n.markerName, n.cols, inner, asDataClass = true)
                appendLine()
                if (i < nested.size - 1) appendLine()
            }
            append(indent).append("}")
        }
    } else {
        append(indent).append("interface $name")
        if (fields.isEmpty() && nested.isEmpty()) {
            append(" { }")
        } else {
            appendLine(" {")
            for ([ann, decl] in fields) {
                append(ann)
                append(inner).appendLine(decl)
            }
            if (fields.isNotEmpty() && nested.isNotEmpty()) appendLine()
            nested.forEachIndexed { i, n ->
                renderMarker(n.markerName, n.cols, inner, asDataClass = false)
                appendLine()
                if (i < nested.size - 1) appendLine()
            }
            append(indent).append("}")
        }
    }
}

private fun escapeStringLiteral(s: String): String =
    s.replace("\\", "\\\\")
        .replace("$", "\\$")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")

private fun nestedName(columnName: String, usedNames: MutableSet<String>): String {
    fun isReserved(name: String) = usedNames.contains(name)
    val prefix = columnName.toDataSchemaName()
    if (!isReserved(prefix)) return prefix
    var id = 1
    while (isReserved("$prefix$id")) id++
    return "$prefix$id"
}

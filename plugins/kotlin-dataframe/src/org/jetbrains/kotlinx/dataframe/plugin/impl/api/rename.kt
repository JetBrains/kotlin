package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.api.rename
import org.jetbrains.kotlinx.dataframe.api.renameToCamelCase
import org.jetbrains.kotlinx.dataframe.api.toCamelCase
import org.jetbrains.kotlinx.dataframe.columns.toColumnSet
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractSchemaModificationInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.Arguments
import org.jetbrains.kotlinx.dataframe.plugin.impl.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleCol
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleColumnGroup
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleDataColumn
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleFrameColumn
import org.jetbrains.kotlinx.dataframe.plugin.impl.asDataFrame
import org.jetbrains.kotlinx.dataframe.plugin.impl.dataFrame
import org.jetbrains.kotlinx.dataframe.plugin.impl.toPluginDataFrameSchema

class Rename : AbstractInterpreter<RenameClauseApproximation>() {
    private val Arguments.receiver by dataFrame()
    private val Arguments.columns: ColumnsResolver by arg()

    override fun Arguments.interpret(): RenameClauseApproximation = RenameClauseApproximation(receiver, columns)
}

class RenameClauseApproximation(val schema: PluginDataFrameSchema, val columns: ColumnsResolver)

class RenameInto : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: RenameClauseApproximation by arg()
    val Arguments.newNames: List<String> by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val columns = receiver.columns.resolve(receiver.schema)
        require(columns.size == newNames.size)
        var i = 0
        return receiver.schema.map(
            selected = columns.mapTo(mutableSetOf()) { it.path.path },
            nextName = { newNames[i].also { i += 1 } },
        )
    }
}

internal fun PluginDataFrameSchema.map(selected: ColumnsSet, nextName: () -> String): PluginDataFrameSchema =
    PluginDataFrameSchema(
        f(columns(), nextName, selected, emptyList()),
    )

internal fun f(
    columns: List<SimpleCol>,
    nextName: () -> String,
    selected: ColumnsSet,
    path: List<String>,
): List<SimpleCol> =
    columns.map {
        val fullPath = path + listOf(it.name)
        when (it) {
            is SimpleColumnGroup -> {
                val group = if (fullPath in selected) {
                    it.rename(nextName())
                } else {
                    it
                }
                group.map(selected, fullPath, nextName)
            }

            is SimpleFrameColumn -> {
                val frame = if (fullPath in selected) {
                    it.rename(nextName())
                } else {
                    it
                }
                frame.map(selected, fullPath, nextName)
            }

            is SimpleDataColumn -> if (fullPath in selected) {
                it.rename(nextName())
            } else {
                it
            }
        }
    }

internal fun SimpleColumnGroup.map(
    selected: ColumnsSet,
    path: List<String>,
    nextName: () -> String,
): SimpleColumnGroup =
    SimpleColumnGroup(
        name = name,
        columns = f(columns(), nextName, selected, path),
    )

internal fun SimpleFrameColumn.map(
    selected: ColumnsSet,
    path: List<String>,
    nextName: () -> String,
): SimpleFrameColumn =
    SimpleFrameColumn(
        name = name,
        columns = f(columns(), nextName, selected, path),
    )

class RenameToCamelCase : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver by dataFrame()

    override fun Arguments.interpret(): PluginDataFrameSchema =
        receiver.asDataFrame().renameToCamelCase().toPluginDataFrameSchema()
}

class RenameToCamelCaseClause : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: RenameClauseApproximation by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val selectedPaths = receiver.columns.resolve(receiver.schema).map { it.path }
        return receiver.schema.asDataFrame()
            .rename { selectedPaths.toColumnSet() }.toCamelCase()
            .toPluginDataFrameSchema()
    }
}

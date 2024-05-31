package org.jetbrains.kotlin.fir.dataframe.api

import org.jetbrains.kotlinx.dataframe.annotations.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.annotations.AbstractSchemaModificationInterpreter
import org.jetbrains.kotlinx.dataframe.annotations.Arguments
import org.jetbrains.kotlinx.dataframe.plugin.ColumnWithPathApproximation
import org.jetbrains.kotlinx.dataframe.plugin.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.SimpleCol
import org.jetbrains.kotlinx.dataframe.plugin.SimpleColumnGroup
import org.jetbrains.kotlinx.dataframe.plugin.SimpleFrameColumn
import org.jetbrains.kotlinx.dataframe.plugin.dataFrame
import org.jetbrains.kotlinx.dataframe.plugin.varargString

class Rename : AbstractInterpreter<RenameClauseApproximation>() {
    private val Arguments.receiver by dataFrame()
    private val Arguments.columns: List<ColumnWithPathApproximation> by arg()
    override fun Arguments.interpret(): RenameClauseApproximation {
        return RenameClauseApproximation(receiver, columns)
    }
}

class RenameClauseApproximation(val schema: PluginDataFrameSchema, val columns: List<ColumnWithPathApproximation>)

class RenameInto : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: RenameClauseApproximation by arg()
    val Arguments.newNames: List<String> by varargString()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        require(receiver.columns.size == newNames.size)
        var i = 0
        return receiver.schema.map(receiver.columns.mapTo(mutableSetOf()) { it.path.path }, nextName = { newNames[i].also { i += 1 } })
    }
}

internal fun PluginDataFrameSchema.map(selected: ColumnsSet, nextName: () -> String): PluginDataFrameSchema {
    return PluginDataFrameSchema(
        f(columns(), nextName, selected, emptyList())
    )
}

internal typealias ColumnsSet = Set<List<String>>

internal fun f(columns: List<SimpleCol>, nextName: () -> String, selected: ColumnsSet, path: List<String>): List<SimpleCol> {
    return columns.map {
        val fullPath = path + listOf(it.name)
        when (it) {
            is SimpleColumnGroup -> {
                val group = if (fullPath in selected) {
                    it.rename(nextName())
                }  else {
                    it
                }
                group.map(selected, fullPath, nextName)
            }
            is SimpleFrameColumn -> {
                val frame = if (fullPath in selected) {
                    it.rename(nextName())
                }  else {
                    it
                }
                frame.map(selected, fullPath, nextName)
            }
            else -> if (fullPath in selected) {
                it.rename(nextName())
            } else {
                it
            }
        }
    }
}

internal fun SimpleColumnGroup.map(selected: ColumnsSet, path: List<String>, nextName: () -> String): SimpleColumnGroup {
    return SimpleColumnGroup(
        name,
        f(columns(), nextName, selected, path),
        type
    )
}

internal fun SimpleFrameColumn.map(selected: ColumnsSet, path: List<String>, nextName: () -> String): SimpleFrameColumn {
    return SimpleFrameColumn(
        name,
        f(columns(), nextName, selected, path),
        anyFrameType
    )
}

package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractSchemaModificationInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.Arguments
import org.jetbrains.kotlinx.dataframe.plugin.impl.data.ColumnWithPathApproximation
import org.jetbrains.kotlinx.dataframe.plugin.impl.dataFrame
import org.jetbrains.kotlinx.dataframe.plugin.impl.varargString

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

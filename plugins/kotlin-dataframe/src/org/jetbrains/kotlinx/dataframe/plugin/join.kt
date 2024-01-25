package org.jetbrains.kotlinx.dataframe.plugin

import org.jetbrains.kotlinx.dataframe.annotations.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.annotations.Arguments
import org.jetbrains.kotlinx.dataframe.impl.ColumnNameGenerator

internal class Join0 : AbstractInterpreter<PluginDataFrameSchema>() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.other: PluginDataFrameSchema by dataFrame()
    val Arguments.selector: ColumnMatchApproximation by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val nameGenerator = ColumnNameGenerator()

        fun MutableList<SimpleCol>.addColumns(schema: PluginDataFrameSchema, selector: ColumnWithPathApproximation) {
            for (column in schema.columns()) {
                if (column.name == selector.path.path.first()) continue
                val uniqueName = nameGenerator.addUnique(column.name)
                add(column.rename(uniqueName))
            }
        }

        val capacity = (receiver.columns().size + other.columns().size - 2).coerceAtLeast(0)
        val columns = buildList(capacity) {
            addColumns(receiver, selector.left)
            addColumns(other, selector.right)
        }
        return PluginDataFrameSchema(columns)
    }
}

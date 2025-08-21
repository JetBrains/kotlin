package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.api.ValueCount
import org.jetbrains.kotlinx.dataframe.impl.ColumnNameGenerator
import org.jetbrains.kotlinx.dataframe.plugin.extensions.wrap
import org.jetbrains.kotlinx.dataframe.plugin.impl.*

class ValueCounts : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver by dataFrame()
    val Arguments.dropNA by ignore()
    val Arguments.ascending by ignore()
    val Arguments.sort by ignore()
    val Arguments.resultColumn: String by arg(defaultValue = Present(ValueCount::count.name))
    val Arguments.columns: ColumnsResolver? by arg(defaultValue = Present(null))

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val res = columns?.resolve(receiver)?.map { it.column } ?: receiver.columns()
        val generator = ColumnNameGenerator(res.map { it.name })
        val count = SimpleDataColumn(generator.addUnique(resultColumn), session.builtinTypes.intType.coneType.wrap())
        return PluginDataFrameSchema(res + count)
    }
}

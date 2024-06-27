package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.plugin.extensions.KotlinTypeFacade
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractSchemaModificationInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.Arguments
import org.jetbrains.kotlinx.dataframe.plugin.impl.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleCol
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleColumnGroup
import org.jetbrains.kotlinx.dataframe.plugin.impl.data.ColumnWithPathApproximation
import org.jetbrains.kotlinx.dataframe.plugin.impl.dataFrame

class Group0 : AbstractInterpreter<GroupClauseApproximation>() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.columns: ColumnsResolver by arg()

    override fun Arguments.interpret(): GroupClauseApproximation {
        return GroupClauseApproximation(receiver, columns)
    }
}

class GroupClauseApproximation(val df: PluginDataFrameSchema, val columns: ColumnsResolver)

class Into0 : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: GroupClauseApproximation by arg()
    val Arguments.column: String by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val grouped = groupImpl(receiver.df.columns(), receiver.columns.resolve(receiver.df).mapTo(mutableSetOf()) { it.path.path }, column)
        return grouped
    }
}

fun KotlinTypeFacade.groupImpl(schema: List<SimpleCol>, paths: Set<List<String>>, into: String): PluginDataFrameSchema {
    val removeResult = removeImpl(schema, paths)
    val grouped = removeResult.updatedColumns + SimpleColumnGroup(into, removeResult.removedColumns)
    return PluginDataFrameSchema(grouped)
}

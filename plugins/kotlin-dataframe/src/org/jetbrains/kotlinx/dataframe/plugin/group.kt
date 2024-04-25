package org.jetbrains.kotlinx.dataframe.plugin

import org.jetbrains.kotlinx.dataframe.KotlinTypeFacade
import org.jetbrains.kotlinx.dataframe.annotations.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.annotations.AbstractSchemaModificationInterpreter
import org.jetbrains.kotlinx.dataframe.annotations.Arguments

class Group0 : AbstractInterpreter<GroupClauseApproximation>() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.columns: List<ColumnWithPathApproximation> by arg()

    override fun Arguments.interpret(): GroupClauseApproximation {
        return GroupClauseApproximation(receiver, columns)
    }
}

class GroupClauseApproximation(val df: PluginDataFrameSchema, val columns: List<ColumnWithPathApproximation>)

class Into0 : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: GroupClauseApproximation by arg()
    val Arguments.column: String by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val grouped = groupImpl(receiver.df.columns(), receiver.columns.mapTo(mutableSetOf()) { it.path.path }, column)
        return grouped
    }
}

fun KotlinTypeFacade.groupImpl(schema: List<SimpleCol>, paths: Set<List<String>>, into: String): PluginDataFrameSchema {
    val removeResult = removeImpl(schema, paths)
    val grouped = removeResult.updatedColumns + SimpleColumnGroup(into, removeResult.removedColumns, anyRow)
    return PluginDataFrameSchema(grouped)
}

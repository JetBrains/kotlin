package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlinx.dataframe.api.getColumnsWithPaths
import org.jetbrains.kotlinx.dataframe.api.isColumnGroup
import org.jetbrains.kotlinx.dataframe.api.remove
import org.jetbrains.kotlinx.dataframe.api.toPath
import org.jetbrains.kotlinx.dataframe.columns.toColumnSet
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractSchemaModificationInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.Arguments
import org.jetbrains.kotlinx.dataframe.plugin.impl.Interpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.Present
import org.jetbrains.kotlinx.dataframe.plugin.impl.asDataFrame
import org.jetbrains.kotlinx.dataframe.plugin.impl.dataFrame
import org.jetbrains.kotlinx.dataframe.plugin.impl.groupBy
import org.jetbrains.kotlinx.dataframe.plugin.impl.toPluginDataFrameSchema

class DataFrameXs : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.keyValues: FirExpression by arg(lens = Interpreter.Id)
    val Arguments.keyColumns: ColumnsResolver? by arg(defaultValue = Present(null))

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val keyColumns = keyColumns?.let { it.resolve(receiver).map { it.path.toPath() } }
        val n = (keyValues as? FirVarargArgumentsExpression)?.arguments?.size ?: return PluginDataFrameSchema.EMPTY
        return receiver
            .asDataFrame()
            .remove { keyColumns?.toColumnSet() ?: colsAtAnyDepth { !it.isColumnGroup() }.take(n) }
            .toPluginDataFrameSchema()
    }
}

class GroupByXs : AbstractInterpreter<GroupBy>() {
    val Arguments.receiver by groupBy()
    val Arguments.keyValues: FirExpression by arg(lens = Interpreter.Id)
    val Arguments.keyColumns: ColumnsResolver? by arg(defaultValue = Present(null))

    override fun Arguments.interpret(): GroupBy {
        val keyColumns = keyColumns?.let { it.resolve(receiver.keys).map { it.path.toPath() } }
        val n = (keyValues as? FirVarargArgumentsExpression)?.arguments?.size ?: return GroupBy.EMPTY

        val toRemove = receiver.keys.asDataFrame()
            .getColumnsWithPaths { keyColumns?.toColumnSet() ?: colsAtAnyDepth { !it.isColumnGroup() }.take(n) }
            .toColumnSet()
        val updatedKeys = receiver.keys.asDataFrame().remove { toRemove }.toPluginDataFrameSchema()
        val updatedGroups = receiver.groups.asDataFrame().remove { toRemove }.toPluginDataFrameSchema()
        return GroupBy(updatedKeys, updatedGroups)
    }
}

package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.api.group
import org.jetbrains.kotlinx.dataframe.api.into
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractSchemaModificationInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.Arguments
import org.jetbrains.kotlinx.dataframe.plugin.impl.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.asDataFrame
import org.jetbrains.kotlinx.dataframe.plugin.impl.dataFrame
import org.jetbrains.kotlinx.dataframe.plugin.impl.toPluginDataFrameSchema

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
        return receiver.df.asDataFrame().group { receiver.columns }.into(column).toPluginDataFrameSchema()
    }
}

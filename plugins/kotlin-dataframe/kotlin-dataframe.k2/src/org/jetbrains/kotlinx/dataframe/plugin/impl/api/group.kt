package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.api.group
import org.jetbrains.kotlinx.dataframe.api.into
import org.jetbrains.kotlinx.dataframe.plugin.impl.*

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

class IntoStringLambda : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: GroupClauseApproximation by arg()
    val Arguments.column: String by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return receiver.df.asDataFrame().group { receiver.columns }.into(column).toPluginDataFrameSchema()
    }
}
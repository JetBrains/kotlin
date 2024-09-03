package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.api.move
import org.jetbrains.kotlinx.dataframe.api.pathOf
import org.jetbrains.kotlinx.dataframe.api.toTop
import org.jetbrains.kotlinx.dataframe.columns.toColumnSet
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractSchemaModificationInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.Arguments
import org.jetbrains.kotlinx.dataframe.plugin.impl.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.asDataFrame
import org.jetbrains.kotlinx.dataframe.plugin.impl.dataFrame
import org.jetbrains.kotlinx.dataframe.plugin.impl.toPluginDataFrameSchema

class Move0 : AbstractInterpreter<MoveClauseApproximation>() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.columns: ColumnsResolver by arg()

    override fun Arguments.interpret(): MoveClauseApproximation {
        return MoveClauseApproximation(receiver, columns)
    }
}

class ToTop : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: MoveClauseApproximation by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val columns = receiver.columns.resolve(receiver.df).map { pathOf(*it.path.path.toTypedArray()) }
        return receiver.df.asDataFrame().move { columns.toColumnSet() }.toTop().toPluginDataFrameSchema()
    }
}

class MoveClauseApproximation(val df: PluginDataFrameSchema, val columns: ColumnsResolver)

package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.api.after
import org.jetbrains.kotlinx.dataframe.api.into
import org.jetbrains.kotlinx.dataframe.api.move
import org.jetbrains.kotlinx.dataframe.api.moveToStart
import org.jetbrains.kotlinx.dataframe.api.toStart
import org.jetbrains.kotlinx.dataframe.api.toEnd
import org.jetbrains.kotlinx.dataframe.api.toTop
import org.jetbrains.kotlinx.dataframe.api.under
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
        val columns = receiver.columns.resolve(receiver.df).map { it.path }
        return receiver.df.asDataFrame().move { columns.toColumnSet() }.toTop().toPluginDataFrameSchema()
    }
}

class MoveUnder0 : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: MoveClauseApproximation by arg()
    val Arguments.column: String by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val columns = receiver.columns.resolve(receiver.df).map { it.path }
        return receiver.df.asDataFrame().move { columns.toColumnSet() }.under(column).toPluginDataFrameSchema()
    }
}

class MoveUnder1 : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: MoveClauseApproximation by arg()
    val Arguments.column: SingleColumnApproximation by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val columns = receiver.columns.resolve(receiver.df).map { it.path }
        return receiver.df.asDataFrame().move { columns.toColumnSet() }.under { column.col.path }.toPluginDataFrameSchema()
    }
}

class MoveInto0 : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: MoveClauseApproximation by arg()
    val Arguments.column: String by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val columns = receiver.columns.resolve(receiver.df).map { it.path }
        return receiver.df.asDataFrame().move { columns.toColumnSet() }.into(column).toPluginDataFrameSchema()
    }
}

class MoveToStart0 : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: MoveClauseApproximation by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val columns = receiver.columns.resolve(receiver.df).map { it.path }
        return receiver.df.asDataFrame().move { columns.toColumnSet() }.toStart().toPluginDataFrameSchema()
    }
}

class MoveToStart1 : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.columns: ColumnsResolver by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val columns = columns.resolve(receiver).map { it.path }
        return receiver.asDataFrame().moveToStart { columns.toColumnSet() }.toPluginDataFrameSchema()
    }
}


class MoveToEnd0 : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: MoveClauseApproximation by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val columns = receiver.columns.resolve(receiver.df).map { it.path }
        return receiver.df.asDataFrame().move { columns.toColumnSet() }.toEnd().toPluginDataFrameSchema()
    }
}

class MoveAfter0 : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: MoveClauseApproximation by arg()
    val Arguments.column: SingleColumnApproximation by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val columns = receiver.columns.resolve(receiver.df).map { it.path }
        return receiver.df.asDataFrame().move { columns.toColumnSet() }.after { column.col.path }.toPluginDataFrameSchema()
    }
}


class MoveClauseApproximation(val df: PluginDataFrameSchema, val columns: ColumnsResolver)

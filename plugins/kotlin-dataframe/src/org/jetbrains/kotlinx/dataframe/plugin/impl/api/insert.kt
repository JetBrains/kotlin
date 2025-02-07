package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.Infer
import org.jetbrains.kotlinx.dataframe.api.insert
import org.jetbrains.kotlinx.dataframe.api.pathOf
import org.jetbrains.kotlinx.dataframe.api.under
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.Arguments
import org.jetbrains.kotlinx.dataframe.plugin.impl.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.Present
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleCol
import org.jetbrains.kotlinx.dataframe.plugin.impl.asDataColumn
import org.jetbrains.kotlinx.dataframe.plugin.impl.asDataFrame
import org.jetbrains.kotlinx.dataframe.plugin.impl.data.ColumnAccessorApproximation
import org.jetbrains.kotlinx.dataframe.plugin.impl.data.ColumnPathApproximation
import org.jetbrains.kotlinx.dataframe.plugin.impl.data.InsertClauseApproximation
import org.jetbrains.kotlinx.dataframe.plugin.impl.data.KPropertyApproximation
import org.jetbrains.kotlinx.dataframe.plugin.impl.dataFrame
import org.jetbrains.kotlinx.dataframe.plugin.impl.enum
import org.jetbrains.kotlinx.dataframe.plugin.impl.simpleColumnOf
import org.jetbrains.kotlinx.dataframe.plugin.impl.toPluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.type

/**
 * @see DataFrame.insert
 */
internal class Insert0 : AbstractInterpreter<InsertClauseApproximation>() {
    val Arguments.column: SimpleCol by arg()
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()

    override fun Arguments.interpret(): InsertClauseApproximation {
        return InsertClauseApproximation(receiver, column)
    }
}

internal class Insert1 : AbstractInterpreter<InsertClauseApproximation>() {
    val Arguments.name: String by arg()
    val Arguments.infer: Infer by enum(defaultValue = Present(Infer.Nulls))
    val Arguments.expression: TypeApproximation by type()
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()

    override fun Arguments.interpret(): InsertClauseApproximation {
        return InsertClauseApproximation(receiver, simpleColumnOf(name, expression.type))
    }
}

internal class Insert2 : AbstractInterpreter<InsertClauseApproximation>() {
    val Arguments.column: ColumnAccessorApproximation by arg()
    val Arguments.infer: Infer by enum(defaultValue = Present(Infer.Nulls))
    val Arguments.expression: TypeApproximation by type()
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()

    override fun Arguments.interpret(): InsertClauseApproximation {
        return InsertClauseApproximation(receiver, simpleColumnOf(column.name, expression.type))
    }
}

internal class Insert3 : AbstractInterpreter<InsertClauseApproximation>() {
    val Arguments.column: KPropertyApproximation by arg()
    val Arguments.infer: Infer by enum(defaultValue = Present(Infer.Nulls))
    val Arguments.expression: TypeApproximation by type()
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()

    override fun Arguments.interpret(): InsertClauseApproximation {
        return InsertClauseApproximation(receiver, simpleColumnOf(column.name, expression.type))
    }
}

internal class Under0 : AbstractInterpreter<PluginDataFrameSchema>() {
    val Arguments.column: SingleColumnApproximation by arg()
    val Arguments.receiver: InsertClauseApproximation by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val path = column.resolve(receiver.df).single().path
        return receiver.df.asDataFrame()
            .insert(receiver.column.asDataColumn()).under(path)
            .toPluginDataFrameSchema()
    }
}

internal class Under1 : AbstractInterpreter<PluginDataFrameSchema>() {
    val Arguments.columnPath: ColumnPathApproximation by arg()
    val Arguments.receiver: InsertClauseApproximation by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return receiver.df.asDataFrame()
            .insert(receiver.column.asDataColumn()).under(columnPath)
            .toPluginDataFrameSchema()
    }
}

internal class Under2 : AbstractInterpreter<PluginDataFrameSchema>() {
    val Arguments.column: ColumnAccessorApproximation by arg()
    val Arguments.receiver: InsertClauseApproximation by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return receiver.df.asDataFrame()
            .insert(receiver.column.asDataColumn()).under(pathOf(column.name))
            .toPluginDataFrameSchema()
    }
}

internal class Under3 : AbstractInterpreter<PluginDataFrameSchema>() {
    val Arguments.column: KPropertyApproximation by arg()
    val Arguments.receiver: InsertClauseApproximation by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return receiver.df.asDataFrame()
            .insert(receiver.column.asDataColumn()).under(pathOf(column.name))
            .toPluginDataFrameSchema()
    }
}

internal class Under4 : AbstractInterpreter<PluginDataFrameSchema>() {
    val Arguments.column: String by arg()
    val Arguments.receiver: InsertClauseApproximation by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return receiver.df.asDataFrame()
            .insert(receiver.column.asDataColumn()).under(pathOf(column))
            .toPluginDataFrameSchema()
    }
}

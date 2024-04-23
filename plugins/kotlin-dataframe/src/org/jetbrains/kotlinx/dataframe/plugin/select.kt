package org.jetbrains.kotlinx.dataframe.plugin

import org.jetbrains.kotlinx.dataframe.annotations.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.annotations.Arguments
import org.jetbrains.kotlinx.dataframe.annotations.Present
import org.jetbrains.kotlinx.dataframe.annotations.TypeApproximation
import org.jetbrains.kotlinx.dataframe.api.Infer

internal class Select0 : AbstractInterpreter<PluginDataFrameSchema>() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.columns: List<ColumnWithPathApproximation> by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return PluginDataFrameSchema(columns.map { it.column })
    }
}

internal class Expr0 : AbstractInterpreter<List<ColumnWithPathApproximation>>() {
    val Arguments.name: String by arg(defaultValue = Present("untitled"))
    val Arguments.infer: Infer by enum(defaultValue = Present(Infer.Nulls))
    val Arguments.expression: TypeApproximation by type()

    override fun Arguments.interpret(): List<ColumnWithPathApproximation> {
        return listOf(ColumnWithPathApproximation(ColumnPathApproximation(listOf(name)), SimpleCol(name, expression)))
    }
}

internal class And0 : AbstractInterpreter<List<ColumnWithPathApproximation>>() {
    val Arguments.receiver: List<ColumnWithPathApproximation> by arg()
    val Arguments.other: List<ColumnWithPathApproximation> by arg()

    override fun Arguments.interpret(): List<ColumnWithPathApproximation> {
        return receiver + other
    }
}

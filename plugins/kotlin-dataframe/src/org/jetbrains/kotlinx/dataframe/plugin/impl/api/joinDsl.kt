package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.Arguments

internal data class ColumnMatchApproximation(val left: ColumnsResolver, val right: ColumnsResolver)

internal class Match0 : AbstractInterpreter<ColumnMatchApproximation>() {
    val Arguments.receiver: ColumnsResolver by arg()
    val Arguments.other: ColumnsResolver by arg()

    override fun Arguments.interpret(): ColumnMatchApproximation {
        return ColumnMatchApproximation(receiver, other)
    }
}

package org.jetbrains.kotlinx.dataframe.plugin

import org.jetbrains.kotlinx.dataframe.annotations.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.annotations.Arguments

internal data class ColumnMatchApproximation(val left: ColumnWithPathApproximation, val right: ColumnWithPathApproximation)

internal class Match0 : AbstractInterpreter<ColumnMatchApproximation>() {
    val Arguments.receiver: List<ColumnWithPathApproximation> by arg()
    val Arguments.other: List<ColumnWithPathApproximation> by arg()

    override fun Arguments.interpret(): ColumnMatchApproximation {
        return ColumnMatchApproximation(receiver.single(), other.single())
    }
}

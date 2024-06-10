package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.Arguments
import org.jetbrains.kotlinx.dataframe.plugin.impl.data.ColumnWithPathApproximation

internal data class ColumnMatchApproximation(val left: ColumnWithPathApproximation, val right: ColumnWithPathApproximation)

internal class Match0 : AbstractInterpreter<ColumnMatchApproximation>() {
    val Arguments.receiver: List<ColumnWithPathApproximation> by arg()
    val Arguments.other: List<ColumnWithPathApproximation> by arg()

    override fun Arguments.interpret(): ColumnMatchApproximation {
        return ColumnMatchApproximation(receiver.single(), other.single())
    }
}

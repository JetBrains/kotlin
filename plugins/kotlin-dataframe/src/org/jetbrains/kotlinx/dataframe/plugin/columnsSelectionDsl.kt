package org.jetbrains.kotlinx.dataframe.plugin

import org.jetbrains.kotlinx.dataframe.annotations.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.annotations.Arguments

internal class And10 : AbstractInterpreter<List<ColumnWithPathApproximation>>() {
    val Arguments.other: List<ColumnWithPathApproximation> by arg()
    val Arguments.receiver: List<ColumnWithPathApproximation> by arg()

    override fun Arguments.interpret(): List<ColumnWithPathApproximation> {
        return receiver + other
    }
}

package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.Arguments
import org.jetbrains.kotlinx.dataframe.plugin.impl.data.ColumnWithPathApproximation

internal class And10 : AbstractInterpreter<List<ColumnWithPathApproximation>>() {
    val Arguments.other: List<ColumnWithPathApproximation> by arg()
    val Arguments.receiver: List<ColumnWithPathApproximation> by arg()

    override fun Arguments.interpret(): List<ColumnWithPathApproximation> {
        return receiver + other
    }
}

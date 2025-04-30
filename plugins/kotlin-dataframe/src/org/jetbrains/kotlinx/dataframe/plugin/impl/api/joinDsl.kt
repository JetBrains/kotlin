package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.api.ColumnMatch
import org.jetbrains.kotlinx.dataframe.columns.ColumnResolutionContext
import org.jetbrains.kotlinx.dataframe.columns.ColumnWithPath
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.Arguments
import org.jetbrains.kotlinx.dataframe.plugin.impl.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.data.ColumnWithPathApproximation

internal data class ColumnMatchApproximation(
    override val left: SingleColumnApproximation,
    override val right: SingleColumnApproximation
): ColumnMatch<Any?>, ColumnsResolver {
    override fun resolve(df: PluginDataFrameSchema): List<ColumnWithPathApproximation> {
        throw UnsupportedOperationException()
    }

    override fun resolve(context: ColumnResolutionContext): List<ColumnWithPath<Any?>> {
        throw UnsupportedOperationException()
    }
}

internal class Match0 : AbstractInterpreter<ColumnMatchApproximation>() {
    val Arguments.receiver: SingleColumnApproximation by arg()
    val Arguments.other: SingleColumnApproximation by arg()

    override fun Arguments.interpret(): ColumnMatchApproximation {
        return ColumnMatchApproximation(receiver, other)
    }
}

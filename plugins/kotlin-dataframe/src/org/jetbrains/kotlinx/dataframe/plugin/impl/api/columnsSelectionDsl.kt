package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.Arguments
import org.jetbrains.kotlinx.dataframe.plugin.impl.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleCol
import org.jetbrains.kotlinx.dataframe.plugin.impl.data.ColumnWithPathApproximation

internal class And10 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.other: ColumnsResolver by arg()
    val Arguments.receiver: ColumnsResolver by arg()

    override fun Arguments.interpret(): ColumnsResolver {
        return object : ColumnsResolver {
            override fun resolve(df: PluginDataFrameSchema): List<ColumnWithPathApproximation> {
                return receiver.resolve(df) + other.resolve(df)
            }
        }
    }
}

class SingleColumnApproximation(val col: ColumnWithPathApproximation) : ColumnsResolver {
    override fun resolve(df: PluginDataFrameSchema): List<ColumnWithPathApproximation> {
        return listOf(col)
    }
}

interface ColumnsResolver {
    fun resolve(df: PluginDataFrameSchema): List<ColumnWithPathApproximation>
}

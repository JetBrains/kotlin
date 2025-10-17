package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.plugin.impl.*

class FillNulls0 : AbstractInterpreter<FillNullsApproximation>() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.columns: ColumnsResolver by arg()

    override fun Arguments.interpret(): FillNullsApproximation {
        return FillNullsApproximation(receiver, columns)
    }
}

data class FillNullsApproximation(val schema: PluginDataFrameSchema, val columns: ColumnsResolver, val where: Boolean = false) : UpdateApproximation {
    override fun withWhere(): UpdateApproximation {
        return copy(where = true)
    }
}
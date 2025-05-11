package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.Arguments
import org.jetbrains.kotlinx.dataframe.plugin.impl.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.dataFrame

class Update0 : AbstractInterpreter<UpdateApproximationImpl>() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.columns: ColumnsResolver by arg()

    override fun Arguments.interpret(): UpdateApproximationImpl {
        return UpdateApproximationImpl(receiver, columns)
    }
}

sealed interface UpdateApproximation

class UpdateApproximationImpl(val schema: PluginDataFrameSchema, val columns: ColumnsResolver) : UpdateApproximation

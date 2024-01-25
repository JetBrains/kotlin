package org.jetbrains.kotlinx.dataframe.plugin

import org.jetbrains.kotlinx.dataframe.annotations.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.annotations.Arguments
import org.jetbrains.kotlinx.dataframe.annotations.TypeApproximation
import org.jetbrains.kotlinx.dataframe.api.Infer

internal class Add0 : AbstractInterpreter<PluginDataFrameSchema>() {
    val Arguments.name: String by string()
    val Arguments.infer: Infer by enum()
    val Arguments.expression: TypeApproximation by type()
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return TODO()
    }
}

package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.api.reorderColumnsByName
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractSchemaModificationInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.Arguments
import org.jetbrains.kotlinx.dataframe.plugin.impl.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.Present
import org.jetbrains.kotlinx.dataframe.plugin.impl.asDataFrame
import org.jetbrains.kotlinx.dataframe.plugin.impl.dataFrame
import org.jetbrains.kotlinx.dataframe.plugin.impl.toPluginDataFrameSchema

class ReorderColumnsByName : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver by dataFrame()
    val Arguments.atAnyDepth: Boolean by arg(defaultValue = Present(true))
    val Arguments.desc: Boolean by arg(defaultValue = Present(false))

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return receiver.asDataFrame().reorderColumnsByName(atAnyDepth, desc).toPluginDataFrameSchema()
    }
}

package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.api.byName
import org.jetbrains.kotlinx.dataframe.api.reorder
import org.jetbrains.kotlinx.dataframe.api.reorderColumnsByName
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractInterpreter
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

class Reorder : AbstractInterpreter<ReorderApproximation>() {
    val Arguments.receiver by dataFrame()
    val Arguments.selector: ColumnsResolver by arg()

    override fun Arguments.interpret(): ReorderApproximation {
        return ReorderApproximation(receiver, selector)
    }
}

class ReorderApproximation(val df: PluginDataFrameSchema, val selector: ColumnsResolver)

class ByName : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: ReorderApproximation by arg()
    val Arguments.desc: Boolean by arg(defaultValue = Present(false))

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return receiver.df.asDataFrame().reorder { receiver.selector }.byName(desc).toPluginDataFrameSchema()
    }
}

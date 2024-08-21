package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.api.flatten
import org.jetbrains.kotlinx.dataframe.api.pathOf
import org.jetbrains.kotlinx.dataframe.columns.toColumnSet
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractSchemaModificationInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.Arguments
import org.jetbrains.kotlinx.dataframe.plugin.impl.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.Present
import org.jetbrains.kotlinx.dataframe.plugin.impl.asDataFrame
import org.jetbrains.kotlinx.dataframe.plugin.impl.dataFrame
import org.jetbrains.kotlinx.dataframe.plugin.impl.toPluginDataFrameSchema

class FlattenDefault : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver by dataFrame()
    val Arguments.keepParentNameForColumns: Boolean by arg(defaultValue = Present(false))
    val Arguments.separator: String by arg(defaultValue = Present("."))

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return receiver.asDataFrame().flatten(keepParentNameForColumns, separator).toPluginDataFrameSchema()
    }
}

class Flatten0 : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver by dataFrame()
    val Arguments.keepParentNameForColumns: Boolean by arg(defaultValue = Present(false))
    val Arguments.separator: String by arg(defaultValue = Present("."))
    val Arguments.columns: ColumnsResolver by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val columns = columns.resolve(receiver).map { pathOf(*it.path.path.toTypedArray()) }
        return receiver
            .asDataFrame()
            .flatten(keepParentNameForColumns, separator) { columns.toColumnSet() }
            .toPluginDataFrameSchema()
    }
}


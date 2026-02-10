package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.api.flatten
import org.jetbrains.kotlinx.dataframe.columns.toColumnSet
import org.jetbrains.kotlinx.dataframe.plugin.impl.*

class FlattenDefault : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver by dataFrame()
    val Arguments.keepParentNameForColumns: Boolean by arg(defaultValue = Present(false))
    val Arguments.separator: String by arg(defaultValue = Present("_"))

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return receiver
            .asDataFrame(/*operation has no selector*/)
            .flatten(keepParentNameForColumns, separator)
            .toPluginDataFrameSchema()
    }
}

class Flatten0 : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver by dataFrame()
    val Arguments.keepParentNameForColumns: Boolean by arg(defaultValue = Present(false))
    val Arguments.separator: String by arg(defaultValue = Present("_"))
    val Arguments.columns: ColumnsResolver by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val columns = columns.resolve(receiver).map { it.path }
        return receiver
            .asDataFrame(/*selected column group is immediately flattened/removed*/)
            .flatten(keepParentNameForColumns, separator) { columns.toColumnSet() }
            .toPluginDataFrameSchema()
    }
}


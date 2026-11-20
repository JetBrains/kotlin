package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlin.fir.StandardTypes
import org.jetbrains.kotlinx.dataframe.plugin.impl.*

class AddId : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.columnName: String by arg(defaultValue = Present("id"))

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val columns = buildList {
            add(simpleColumnOf(columnName, StandardTypes.Int))
            addAll(receiver.columns())
        }
        return PluginDataFrameSchema(columns)
    }
}

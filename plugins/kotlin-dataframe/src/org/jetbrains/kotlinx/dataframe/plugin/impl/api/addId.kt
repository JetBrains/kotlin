package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractSchemaModificationInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.Arguments
import org.jetbrains.kotlinx.dataframe.plugin.impl.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.Present
import org.jetbrains.kotlinx.dataframe.plugin.impl.dataFrame
import org.jetbrains.kotlinx.dataframe.plugin.impl.simpleColumnOf

class AddId : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.columnName: String by arg(defaultValue = Present("id"))

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val columns = buildList {
            add(simpleColumnOf(columnName, session.builtinTypes.intType.type))
            addAll(receiver.columns())
        }
        return PluginDataFrameSchema(columns)
    }
}

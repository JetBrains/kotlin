package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.plugin.impl.*

class MapToFrame : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.body by dsl()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val addDsl = AddDslApproximation(mutableListOf())
        body(addDsl, emptyMap())
        return PluginDataFrameSchema(addDsl.columns)
    }
}

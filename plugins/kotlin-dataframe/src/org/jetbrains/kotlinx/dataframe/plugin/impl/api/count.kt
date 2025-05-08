package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.plugin.impl.*

class GroupByCount0 : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver by groupBy()
    val Arguments.resultName: String by arg(defaultValue = Present("count"))
    val Arguments.predicate by ignore()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return receiver.keys.add(resultName, session.builtinTypes.intType.coneType, context = this)
    }
}

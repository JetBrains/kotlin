package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractSchemaModificationInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.Arguments
import org.jetbrains.kotlinx.dataframe.plugin.impl.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.Present
import org.jetbrains.kotlinx.dataframe.plugin.impl.add
import org.jetbrains.kotlinx.dataframe.plugin.impl.groupBy
import org.jetbrains.kotlinx.dataframe.plugin.impl.ignore

class GroupByCount0 : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver by groupBy()
    val Arguments.resultName: String by arg(defaultValue = Present("count"))
    val Arguments.predicate by ignore()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return receiver.keys.add(resultName, session.builtinTypes.intType.type, context = this)
    }
}

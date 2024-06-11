package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.plugin.extensions.KotlinTypeFacade
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractSchemaModificationInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.Arguments
import org.jetbrains.kotlinx.dataframe.plugin.impl.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleCol
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleColumnGroup
import org.jetbrains.kotlinx.dataframe.plugin.impl.data.ColumnWithPathApproximation
import org.jetbrains.kotlinx.dataframe.plugin.impl.dataFrame

class Ungroup0 : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.columns: List<ColumnWithPathApproximation> by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val ungrouped = ungroupImpl(receiver.columns(), columns.mapTo(mutableSetOf()) { it.path.path }, emptyList())
        return PluginDataFrameSchema(ungrouped)
    }
}

fun KotlinTypeFacade.ungroupImpl(schema: List<SimpleCol>, path: Set<List<String>>, p: List<String>): List<SimpleCol> {
    return schema.flatMap {
        if (it !is SimpleColumnGroup) {
            listOf(it)
        } else {
            if (p + it.name in path) {
                it.columns()
            } else {
                listOf(SimpleColumnGroup(it.name, ungroupImpl(it.columns(), path, p + it.name)))
            }
        }
    }
}

package org.jetbrains.kotlinx.dataframe.plugin

import org.jetbrains.kotlinx.dataframe.KotlinTypeFacade
import org.jetbrains.kotlinx.dataframe.annotations.AbstractSchemaModificationInterpreter
import org.jetbrains.kotlinx.dataframe.annotations.Arguments

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
                listOf(SimpleColumnGroup(it.name, ungroupImpl(it.columns(), path, p + it.name), anyRow))
            }
        }
    }
}

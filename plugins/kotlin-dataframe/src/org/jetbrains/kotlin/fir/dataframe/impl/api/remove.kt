package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.plugin.extensions.KotlinTypeFacade
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractSchemaModificationInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.Arguments
import org.jetbrains.kotlinx.dataframe.plugin.impl.data.ColumnWithPathApproximation
import org.jetbrains.kotlinx.dataframe.plugin.impl.dataFrame

class Remove0 : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.columns: List<ColumnWithPathApproximation> by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val removeResult = removeImpl(receiver.columns(), columns.mapTo(mutableSetOf()) { it.path.path })
        return PluginDataFrameSchema(removeResult.updatedColumns)
    }
}

class RemoveResult(val updatedColumns: List<SimpleCol>, val removedColumns: List<SimpleCol>)

fun KotlinTypeFacade.removeImpl(schema: List<SimpleCol>, paths: Set<List<String>>): RemoveResult {
    val removed = mutableListOf<SimpleCol>()
    fun remove(schema: List<SimpleCol>, p: List<String>): List<SimpleCol> {
        return schema.flatMap {
            if (p + it.name() in paths) {
                removed.add(it)
                emptyList()
            } else {
                if (it is SimpleColumnGroup) {
                    val columns = remove(it.columns(), p + it.name())
                    if (columns.isEmpty()) emptyList() else listOf(SimpleColumnGroup(it.name(), columns, anyRow))
                } else {
                    listOf(it)
                }
            }
        }
    }
    val updatedColumns = remove(schema, emptyList())
    return RemoveResult(updatedColumns, removed)
}

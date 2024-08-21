package org.jetbrains.kotlinx.dataframe.plugin.impl

import org.jetbrains.kotlinx.dataframe.AnyCol
import org.jetbrains.kotlinx.dataframe.DataColumn
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.cast
import org.jetbrains.kotlinx.dataframe.api.dataFrameOf
import org.jetbrains.kotlinx.dataframe.columns.ColumnGroup
import org.jetbrains.kotlinx.dataframe.columns.FrameColumn
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.TypeApproximation

fun PluginDataFrameSchema.asDataFrame(): DataFrame<ConeTypesAdapter> = columns().map()

fun DataFrame<ConeTypesAdapter>.toPluginDataFrameSchema() = PluginDataFrameSchema(columns().mapBack())

interface ConeTypesAdapter

private fun List<SimpleCol>.map(): DataFrame<ConeTypesAdapter> = map {
    when (it) {
        is SimpleDataColumn -> DataColumn.create(it.name, listOf(it.type))
        is SimpleColumnGroup -> DataColumn.createColumnGroup(it.name, it.columns().map())
        is SimpleFrameColumn -> DataColumn.createFrameColumn(it.name, listOf(it.columns().map()))
    }
}.let { dataFrameOf(it).cast() }

private fun List<AnyCol>.mapBack(): List<SimpleCol> = map {
    when (it) {
        is ColumnGroup<*> -> {
            SimpleColumnGroup(it.name(), it.columns().mapBack())
        }
        is FrameColumn<*> -> {
            SimpleFrameColumn(it.name(), it[0].columns().mapBack())
        }
        else -> {
            SimpleDataColumn(it.name(), it[0] as TypeApproximation)
        }
    }
}

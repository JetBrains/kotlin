package org.jetbrains.kotlinx.dataframe.plugin.impl

import org.jetbrains.kotlinx.dataframe.AnyCol
import org.jetbrains.kotlinx.dataframe.DataColumn
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.asDataColumn
import org.jetbrains.kotlinx.dataframe.api.cast
import org.jetbrains.kotlinx.dataframe.api.dataFrameOf
import org.jetbrains.kotlinx.dataframe.columns.ColumnGroup
import org.jetbrains.kotlinx.dataframe.columns.FrameColumn
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.TypeApproximation

fun PluginDataFrameSchema.asDataFrame(): DataFrame<ConeTypesAdapter> {
    val df = columns().map()
    return df
}

fun DataFrame<ConeTypesAdapter>.toPluginDataFrameSchema() = PluginDataFrameSchema(columns().mapBack())

interface ConeTypesAdapter

private fun List<SimpleCol>.map(): DataFrame<ConeTypesAdapter> {
    val columns = map {
        it.asDataColumn()
    }
    return dataFrameOf(columns).cast()
}

fun SimpleCol.asDataColumn(): DataColumn<*> {
    val column = when (this) {
        is SimpleDataColumn -> DataColumn.createByType(this.name, listOf(this.type))
        is SimpleColumnGroup -> DataColumn.createColumnGroup(this.name, this.columns().map()).asDataColumn()
        is SimpleFrameColumn -> DataColumn.createFrameColumn(this.name, listOf(this.columns().map()))
    }
    return column
}

private fun List<AnyCol>.mapBack(): List<SimpleCol> =
    map {
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

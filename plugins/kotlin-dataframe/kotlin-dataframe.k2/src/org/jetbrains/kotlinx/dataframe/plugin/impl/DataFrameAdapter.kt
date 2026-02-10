package org.jetbrains.kotlinx.dataframe.plugin.impl

import org.jetbrains.kotlinx.dataframe.AnyCol
import org.jetbrains.kotlinx.dataframe.DataColumn
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.asColumn
import org.jetbrains.kotlinx.dataframe.api.asDataColumn
import org.jetbrains.kotlinx.dataframe.api.cast
import org.jetbrains.kotlinx.dataframe.api.convert
import org.jetbrains.kotlinx.dataframe.api.dataFrameOf
import org.jetbrains.kotlinx.dataframe.api.perRowCol
import org.jetbrains.kotlinx.dataframe.columns.ColumnGroup
import org.jetbrains.kotlinx.dataframe.columns.FrameColumn
import org.jetbrains.kotlinx.dataframe.plugin.extensions.ColumnType
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.ColumnsResolver

fun PluginDataFrameSchema.asDataFrame(impliedColumnsResolver: ColumnsResolver): DataFrame<ConeTypesAdapter> {
    val df = columns(impliedColumnsResolver).map()
    return df
}

// with 2 separate functions, it's easier to find usages.
// ideally, argument-less function should have a reason to be used, because proper usage indicated String API support
fun PluginDataFrameSchema.asDataFrame(): DataFrame<ConeTypesAdapter> {
    val df = columns().map()
    return df
}

fun DataFrame<ConeTypesAdapter>.toPluginDataFrameSchema() = PluginDataFrameSchema(columns().mapBack())

interface ConeTypesAdapter

fun PluginDataFrameSchema.convert(columns: ColumnsResolver, converter: (SimpleCol) -> ColumnType): PluginDataFrameSchema {
    return asDataFrame(impliedColumnsResolver = columns)
        .convert { columns }.perRowCol { _, col -> converter(col.asSimpleColumn()) }
        .toPluginDataFrameSchema()
}

fun PluginDataFrameSchema.convertAsColumn(columns: ColumnsResolver, converter: (SimpleCol) -> SimpleCol): PluginDataFrameSchema {
    return asDataFrame(impliedColumnsResolver = columns)
        .convert { columns }.asColumn { converter(it.asSimpleColumn()).asDataColumn() }
        .toPluginDataFrameSchema()
}

private fun List<SimpleCol>.map(): DataFrame<ConeTypesAdapter> {
    val columns = map {
        it.asDataColumn()
    }
    // avoiding UnequalColumnSize exception in dataFrameOf for an empty column group
    if (columns.isEmpty()) return DataFrame.empty(nrow = 1).cast()
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

private fun List<AnyCol>.mapBack(): List<SimpleCol> = map { it.asSimpleColumn() }

fun AnyCol.asSimpleColumn(): SimpleCol {
    return when (this) {
        is ColumnGroup<*> -> {
            SimpleColumnGroup(name(), columns().mapBack())
        }

        is FrameColumn<*> -> {
            SimpleFrameColumn(name(), this[0].columns().mapBack())
        }

        else -> {
            SimpleDataColumn(name(), this[0] as ColumnType)
        }
    }
}

package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.plugin.impl.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleCol
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleColumnGroup
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleFrameColumn
import org.jetbrains.kotlinx.dataframe.plugin.impl.data.ColumnPathApproximation
import org.jetbrains.kotlinx.dataframe.plugin.impl.data.ColumnWithPathApproximation

fun PluginDataFrameSchema.flatten(): List<ColumnWithPathApproximation> {
    if (columns().isEmpty()) return emptyList()
    val columns = mutableListOf<ColumnWithPathApproximation>()
    flattenImpl(columns(), emptyList(), columns)
    return columns
}

fun flattenImpl(columns: List<SimpleCol>, path: List<String>, flatList: MutableList<ColumnWithPathApproximation>) {
    columns.forEach { column ->
        val fullPath = path + listOf(column.name)
        when (column) {
            is SimpleColumnGroup -> {
                flatList.add(ColumnWithPathApproximation(ColumnPathApproximation(fullPath), column))
                flattenImpl(column.columns(), fullPath, flatList)
            }
            is SimpleFrameColumn -> {
                flatList.add(ColumnWithPathApproximation(ColumnPathApproximation(fullPath), column))
                flattenImpl(column.columns(), fullPath, flatList)
            }
            else -> {
                flatList.add(ColumnWithPathApproximation(ColumnPathApproximation(fullPath), column))
            }
        }
    }
}

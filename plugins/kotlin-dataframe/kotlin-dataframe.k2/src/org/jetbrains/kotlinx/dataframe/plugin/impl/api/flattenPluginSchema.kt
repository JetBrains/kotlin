package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.columns.ColumnPath
import org.jetbrains.kotlinx.dataframe.plugin.impl.*
import org.jetbrains.kotlinx.dataframe.plugin.impl.data.ColumnWithPathApproximation

fun PluginDataFrameSchema.flatten(includeFrames: Boolean): List<ColumnWithPathApproximation> {
    if (columns().isEmpty()) return emptyList()
    val columns = mutableListOf<ColumnWithPathApproximation>()
    flattenImpl(columns(), emptyList(), columns, includeFrames)
    return columns
}

fun flattenImpl(columns: List<SimpleCol>, path: List<String>, flatList: MutableList<ColumnWithPathApproximation>, includeFrames: Boolean) {
    columns.forEach { column ->
        val fullPath = path + listOf(column.name)
        when (column) {
            is SimpleColumnGroup -> {
                flatList.add(ColumnWithPathApproximation(ColumnPath(fullPath), column))
                flattenImpl(column.columns(), fullPath, flatList, includeFrames)
            }
            is SimpleFrameColumn -> {
                flatList.add(ColumnWithPathApproximation(ColumnPath(fullPath), column))
                flattenImpl(column.columns(), fullPath, flatList, includeFrames)
            }
            is SimpleDataColumn -> {
                flatList.add(ColumnWithPathApproximation(ColumnPath(fullPath), column))
            }
        }
    }
}

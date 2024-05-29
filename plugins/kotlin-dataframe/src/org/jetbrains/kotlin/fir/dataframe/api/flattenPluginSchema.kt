package org.jetbrains.kotlin.fir.dataframe.api

import org.jetbrains.kotlinx.dataframe.plugin.ColumnPathApproximation
import org.jetbrains.kotlinx.dataframe.plugin.ColumnWithPathApproximation
import org.jetbrains.kotlinx.dataframe.plugin.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.SimpleCol
import org.jetbrains.kotlinx.dataframe.plugin.SimpleColumnGroup
import org.jetbrains.kotlinx.dataframe.plugin.SimpleFrameColumn

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

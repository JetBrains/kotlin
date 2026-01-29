package org.jetbrains.kotlinx.dataframe.plugin.impl.data

import org.jetbrains.kotlinx.dataframe.columns.ColumnWithPath
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleCol

/**
 * @see ColumnWithPath
 */
data class ColumnWithPathApproximation(
    val path: ColumnPathApproximation,
    val column: SimpleCol,
    val isImpliedColumn: Boolean = false,
)

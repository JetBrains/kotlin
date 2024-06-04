package org.jetbrains.kotlinx.dataframe.plugin

import org.jetbrains.kotlinx.dataframe.columns.ColumnWithPath

/**
 * @see ColumnWithPath
 */
data class ColumnWithPathApproximation(val path: ColumnPathApproximation, val column: SimpleCol)

package org.jetbrains.kotlinx.dataframe.plugin.impl.data

import org.jetbrains.kotlinx.dataframe.plugin.impl.api.SimpleCol
import org.jetbrains.kotlinx.dataframe.columns.ColumnWithPath

/**
 * @see ColumnWithPath
 */
data class ColumnWithPathApproximation(val path: ColumnPathApproximation, val column: SimpleCol)

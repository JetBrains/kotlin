package org.jetbrains.kotlinx.dataframe.plugin

import org.jetbrains.kotlinx.dataframe.columns.ColumnWithPath

/**
 * @see ColumnWithPath
 */
public data class ColumnWithPathApproximation(public val path: ColumnPathApproximation, public val column: SimpleCol)

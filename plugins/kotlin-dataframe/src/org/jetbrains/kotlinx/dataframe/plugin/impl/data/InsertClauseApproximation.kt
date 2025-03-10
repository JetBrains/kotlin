package org.jetbrains.kotlinx.dataframe.plugin.impl.data

import org.jetbrains.kotlinx.dataframe.api.InsertClause
import org.jetbrains.kotlinx.dataframe.plugin.impl.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleCol

/**
 * @see InsertClause
 */
data class InsertClauseApproximation(val df: PluginDataFrameSchema, val column: SimpleCol)

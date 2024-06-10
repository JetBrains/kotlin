package org.jetbrains.kotlinx.dataframe.plugin.impl.data

import org.jetbrains.kotlinx.dataframe.plugin.impl.api.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.SimpleCol
import org.jetbrains.kotlinx.dataframe.api.InsertClause

/**
 * @see InsertClause
*/
data class InsertClauseApproximation(val df: PluginDataFrameSchema, val column: SimpleCol)

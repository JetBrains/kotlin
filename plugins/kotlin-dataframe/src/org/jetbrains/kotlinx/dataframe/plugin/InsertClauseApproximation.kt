package org.jetbrains.kotlinx.dataframe.plugin

import org.jetbrains.kotlinx.dataframe.api.InsertClause

/**
 * @see InsertClause
*/
public data class InsertClauseApproximation(public val df: PluginDataFrameSchema, public val column: SimpleCol)

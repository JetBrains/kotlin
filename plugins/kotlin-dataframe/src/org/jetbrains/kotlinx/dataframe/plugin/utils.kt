package org.jetbrains.kotlinx.dataframe.plugin

import org.jetbrains.kotlinx.dataframe.ColumnsSelector
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.getColumnsWithPaths

@Suppress("UnusedReceiverParameter")
fun <T, C> DataFrame<T>.makeSelector(selector: ColumnsSelector<T, C>): ColumnsSelector<T, C> = selector

fun <T, C> ColumnsSelector<T, C>.toColumnPath(df: DataFrame<T>): List<ColumnPathApproximation> = df
    .getColumnsWithPaths(this)
    .map { ColumnPathApproximation(it.path.path) }

fun PluginDataFrameSchema.print(): Unit = println(this)

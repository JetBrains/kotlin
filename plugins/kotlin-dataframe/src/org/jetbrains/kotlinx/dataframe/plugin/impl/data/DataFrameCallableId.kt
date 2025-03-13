package org.jetbrains.kotlinx.dataframe.plugin.impl.data

data class DataFrameCallableId(
    val packageName: String,
    val className: String,
    val callableName: String
)

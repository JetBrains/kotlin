package org.jetbrains.kotlinx.dataframe.plugin

public data class DataFrameCallableId(
    val packageName: String,
    val className: String,
    val callableName: String
)

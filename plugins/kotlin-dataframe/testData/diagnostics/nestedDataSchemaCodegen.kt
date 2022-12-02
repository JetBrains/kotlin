package org.jetbrains.kotlinx.dataframe

import org.jetbrains.kotlinx.dataframe.annotations.DataSchema
import org.jetbrains.kotlinx.dataframe.api.*

@DataSchema
class Function(
    val receiverType: String,
    val function: String,
    val functionReturnType: Type,
    val parameters: List<Parameter>
) : DataRowSchema {
}

@DataSchema
data class Type(val name: String, val vararg: Boolean)

@DataSchema
class Parameter(
    val name: String,
    val returnType: Type,
    val defaultValue: String?,
) : DataRowSchema

internal fun take(df: DataFrame<Function>) {
    df.functionReturnType.vararg
    df.parameters.first().returnType.name
}

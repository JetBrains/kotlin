package org.jetbrains.kotlinx.dataframe

import org.jetbrains.kotlinx.dataframe.annotations.DataSchema
import org.jetbrains.kotlinx.dataframe.api.*

@DataSchema
class Bridge(val type: Type,
             val approximation: String,
             val converter: String,
             val lens: String,
             val supported: Boolean = false) : DataRowSchema

@DataSchema
data class Type(val name: String, val vararg: Boolean)

@DataSchema
class Parameter(
    val name: String,
    val returnType: Type,
    val defaultValue: String?,
) : DataRowSchema

@DataSchema
class Function(
    val receiverType: String,
    val function: String,
    val functionReturnType: Type,
    val parameters: List<Parameter>
) : DataRowSchema {
}


@DataSchema
class RefinedFunction(
    val receiverType: String,
    val function: String,
    val functionReturnType: Type,
    val parameters: List<Parameter>,
    val startingSchema: Parameter
) : DataRowSchema

fun DataFrame<Function>.refine(bridges: DataFrame<Bridge>) {
    val functions = this
    val df1 = functions.joinDefault(bridges) {
        functions.functionReturnType.name.match(right.type.name)
    }
    val col: DataColumn<DataFrame<*>> = df1.parameters
}

fun box(): String = "OK"

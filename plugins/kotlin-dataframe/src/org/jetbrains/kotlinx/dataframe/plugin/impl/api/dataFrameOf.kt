@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.types.commonSuperTypeOrNull
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractSchemaModificationInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.Arguments
import org.jetbrains.kotlinx.dataframe.plugin.impl.Interpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.simpleColumnOf
import org.jetbrains.kotlinx.dataframe.plugin.impl.varargString
import org.jetbrains.kotlinx.dataframe.impl.api.withValuesImpl

class DataFrameOf0 : AbstractInterpreter<DataFrameBuilderApproximation>() {
    val Arguments.header: List<String> by varargString()

    override fun Arguments.interpret(): DataFrameBuilderApproximation {
        return DataFrameBuilderApproximation(header)
    }
}

class DataFrameBuilderApproximation(val header: List<String>)

class DataFrameBuilderInvoke0 : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: DataFrameBuilderApproximation by arg()
    val Arguments.values: FirVarargArgumentsExpression by arg(lens = Interpreter.Id)

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val columns = withValuesImpl(receiver.header, values.arguments).map { (name, values) ->
            val type = session.typeContext.commonSuperTypeOrNull(values.map { it.resolvedType }) ?: error("$name $values")
            simpleColumnOf(name, type)
        }
        return PluginDataFrameSchema(columns)
    }
}

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.types.commonSuperTypeOrNull
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.type
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractSchemaModificationInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.Arguments
import org.jetbrains.kotlinx.dataframe.plugin.impl.Interpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.simpleColumnOf
import org.jetbrains.kotlinx.dataframe.impl.api.withValuesImpl

class DataFrameOf0 : AbstractInterpreter<DataFrameBuilderApproximation>() {
    val Arguments.header: List<String> by arg()

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

class DataFrameOf3 : AbstractSchemaModificationInterpreter() {
    val Arguments.columns: List<Interpreter.Success<Pair<*, *>>> by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val res = columns.map {
            val it = it.value
            val name = (it.first as? FirLiteralExpression)?.value as? String
            val type = (it.second as? FirExpression)?.resolvedType?.typeArguments?.getOrNull(0)?.type
            if (name == null || type == null) return PluginDataFrameSchema(emptyList())
            simpleColumnOf(name, type)
        }
        return PluginDataFrameSchema(res)
    }
}

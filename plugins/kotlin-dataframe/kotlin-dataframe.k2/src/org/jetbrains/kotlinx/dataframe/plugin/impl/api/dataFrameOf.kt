package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.plugin.createConeType
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlinx.dataframe.impl.api.withValuesImpl
import org.jetbrains.kotlinx.dataframe.plugin.impl.*
import org.jetbrains.kotlinx.dataframe.plugin.utils.Names

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
        val columns = (receiver.header to values.arguments).withValuesImpl().map { (name, values) ->
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

abstract class SchemaConstructor : AbstractSchemaModificationInterpreter() {
    val Arguments.columns: List<Interpreter.Success<Pair<*, *>>> by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val res = columns.map {
            val it = it.value
            val name = (it.first as? FirLiteralExpression)?.value as? String
            val resolvedType = (it.second as? FirExpression)?.resolvedType
            val type: ConeKotlinType? = when (resolvedType?.classId) {
                Names.COLUM_GROUP_CLASS_ID -> Names.DATA_ROW_CLASS_ID.createConeType(session, arrayOf(resolvedType.typeArguments[0]))
                Names.FRAME_COLUMN_CLASS_ID -> Names.DF_CLASS_ID.createConeType(session, arrayOf(resolvedType.typeArguments[0]))
                Names.DATA_COLUMN_CLASS_ID -> resolvedType.typeArguments[0] as? ConeKotlinType
                Names.BASE_COLUMN_CLASS_ID -> resolvedType.typeArguments[0] as? ConeKotlinType
                else -> null
            }
            if (name == null || type == null) return PluginDataFrameSchema(emptyList())
            simpleColumnOf(name, type)
        }
        return PluginDataFrameSchema(res)
    }
}

class DataFrameOfPairs : SchemaConstructor()

class ColumnOfPairs : SchemaConstructor()


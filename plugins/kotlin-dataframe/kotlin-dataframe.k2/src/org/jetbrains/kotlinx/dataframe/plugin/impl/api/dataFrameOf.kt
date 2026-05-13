package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.plugin.createConeType
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlinx.dataframe.impl.api.withValuesImpl
import org.jetbrains.kotlinx.dataframe.plugin.extensions.ColumnType
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

context(_: Arguments)
private fun fixedTypeSchema(
    receiver: DataFrameBuilderApproximation,
    type: ConeKotlinType,
): PluginDataFrameSchema = PluginDataFrameSchema(
    receiver.header.map { simpleColumnOf(it, type) }
)

class DataFrameBuilderRandomInt : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: DataFrameBuilderApproximation by arg()
    val Arguments.nrow by ignore()

    override fun Arguments.interpret(): PluginDataFrameSchema =
        fixedTypeSchema(receiver, session.builtinTypes.intType.coneType)
}

class DataFrameBuilderRandomIntRange : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: DataFrameBuilderApproximation by arg()
    val Arguments.nrow by ignore()
    val Arguments.range by ignore()

    override fun Arguments.interpret(): PluginDataFrameSchema =
        fixedTypeSchema(receiver, session.builtinTypes.intType.coneType)
}

class DataFrameBuilderRandomDouble : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: DataFrameBuilderApproximation by arg()
    val Arguments.nrow by ignore()

    override fun Arguments.interpret(): PluginDataFrameSchema =
        fixedTypeSchema(receiver, session.builtinTypes.doubleType.coneType)
}

class DataFrameBuilderRandomDoubleRange : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: DataFrameBuilderApproximation by arg()
    val Arguments.nrow by ignore()
    val Arguments.range by ignore()

    override fun Arguments.interpret(): PluginDataFrameSchema =
        fixedTypeSchema(receiver, session.builtinTypes.doubleType.coneType)
}

class DataFrameBuilderRandomFloat : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: DataFrameBuilderApproximation by arg()
    val Arguments.nrow by ignore()

    override fun Arguments.interpret(): PluginDataFrameSchema =
        fixedTypeSchema(receiver, session.builtinTypes.floatType.coneType)
}

class DataFrameBuilderRandomLong : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: DataFrameBuilderApproximation by arg()
    val Arguments.nrow by ignore()

    override fun Arguments.interpret(): PluginDataFrameSchema =
        fixedTypeSchema(receiver, session.builtinTypes.longType.coneType)
}

class DataFrameBuilderRandomLongRange : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: DataFrameBuilderApproximation by arg()
    val Arguments.nrow by ignore()
    val Arguments.range by ignore()

    override fun Arguments.interpret(): PluginDataFrameSchema =
        fixedTypeSchema(receiver, session.builtinTypes.longType.coneType)
}

class DataFrameBuilderRandomBoolean : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: DataFrameBuilderApproximation by arg()
    val Arguments.nrow by ignore()

    override fun Arguments.interpret(): PluginDataFrameSchema =
        fixedTypeSchema(receiver, session.builtinTypes.booleanType.coneType)
}

class DataFrameBuilderNulls : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: DataFrameBuilderApproximation by arg()
    val Arguments.typeArg0: ColumnType by type()
    val Arguments.nrow by ignore()

    override fun Arguments.interpret(): PluginDataFrameSchema =
        fixedTypeSchema(receiver, typeArg0.coneType.withNullability(true, session.typeContext))
}

class DataFrameBuilderFillIndexed : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: DataFrameBuilderApproximation by arg()
    val Arguments.typeArg0: ColumnType by type()
    val Arguments.nrow by ignore()
    val Arguments.init by ignore()

    override fun Arguments.interpret(): PluginDataFrameSchema =
        fixedTypeSchema(receiver, typeArg0.coneType)
}

class DataFrameBuilderFill : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: DataFrameBuilderApproximation by arg()
    val Arguments.typeArg0: ColumnType by type()
    val Arguments.nrow by ignore()
    val Arguments.init by ignore()

    override fun Arguments.interpret(): PluginDataFrameSchema =
        fixedTypeSchema(receiver, typeArg0.coneType)
}

class DataFrameBuilderFillValue : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: DataFrameBuilderApproximation by arg()
    val Arguments.typeArg0: ColumnType by type()
    val Arguments.nrow by ignore()
    val Arguments.value by ignore()

    override fun Arguments.interpret(): PluginDataFrameSchema =
        fixedTypeSchema(receiver, typeArg0.coneType)
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
    val Arguments.columns: List<Interpreter.Success<Pair<*, *>>?> by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val res = columns.map {
            val it = it?.value
            val name = (it?.first as? FirLiteralExpression)?.value as? String
            val resolvedType = (it?.second as? FirExpression)?.resolvedType
            val type: ConeKotlinType? = extractBaseColumnValuesType(resolvedType)
            if (name == null || type == null) return PluginDataFrameSchema(emptyList())
            simpleColumnOf(name, type)
        }
        return PluginDataFrameSchema(res)
    }
}

internal fun Arguments.extractBaseColumnValuesType(resolvedType: ConeKotlinType?): ConeKotlinType? {
    val type: ConeKotlinType? = when (resolvedType?.classId) {
        Names.COLUM_GROUP_CLASS_ID -> Names.DATA_ROW_CLASS_ID.createConeType(session, arrayOf(resolvedType.typeArguments[0]))
        Names.FRAME_COLUMN_CLASS_ID -> Names.DF_CLASS_ID.createConeType(session, arrayOf(resolvedType.typeArguments[0]))
        Names.DATA_COLUMN_CLASS_ID -> resolvedType.typeArguments[0] as? ConeKotlinType
        Names.BASE_COLUMN_CLASS_ID -> resolvedType.typeArguments[0] as? ConeKotlinType
        Names.VALUE_COLUMN_CLASS_ID -> resolvedType.typeArguments[0] as? ConeKotlinType
        else -> null
    }
    return type
}

class DataFrameOfPairs : SchemaConstructor()

class ColumnOfPairs : SchemaConstructor()

class DataFrameGenerator : AbstractSchemaModificationInterpreter() {
    val Arguments.size by ignore()
    val Arguments.body by dsl()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val dsl = CreateDataFrameDslImplApproximation()
        body(dsl, mapOf("typeArg0" to Interpreter.Success(session.builtinTypes.intType.coneType)))
        return dsl.toPluginDataFrameSchema()
    }
}

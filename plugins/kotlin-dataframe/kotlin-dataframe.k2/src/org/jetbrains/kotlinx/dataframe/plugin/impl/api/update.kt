package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.isMarkedNullable
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.fir.types.withNullability
import org.jetbrains.kotlinx.dataframe.plugin.extensions.ColumnType
import org.jetbrains.kotlinx.dataframe.plugin.extensions.wrap
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractSchemaModificationInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.Arguments
import org.jetbrains.kotlinx.dataframe.plugin.impl.Interpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleDataColumn
import org.jetbrains.kotlinx.dataframe.plugin.impl.convertAsColumn
import org.jetbrains.kotlinx.dataframe.plugin.impl.dataFrame
import org.jetbrains.kotlinx.dataframe.plugin.impl.ignore
import org.jetbrains.kotlinx.dataframe.plugin.impl.simpleColumnOf
import org.jetbrains.kotlinx.dataframe.plugin.impl.type

class Update0 : AbstractInterpreter<UpdateApproximationImpl>() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.columns: ColumnsResolver by arg()

    override fun Arguments.interpret(): UpdateApproximationImpl {
        return UpdateApproximationImpl(receiver, columns)
    }
}

class UpdateWhere : AbstractInterpreter<UpdateApproximation>() {
    val Arguments.receiver: UpdateApproximation by arg()
    val Arguments.predicate by ignore()

    override fun Arguments.interpret(): UpdateApproximation {
        return receiver.withWhere()
    }
}

class UpdateAt : AbstractInterpreter<UpdateApproximation>() {
    val Arguments.receiver: UpdateApproximation by arg()
    val Arguments.rowIndices by ignore()
    val Arguments.rowRange by ignore()

    override fun Arguments.interpret(): UpdateApproximation {
        return receiver.withWhere()
    }
}

sealed interface UpdateApproximation {
    fun withWhere(): UpdateApproximation
}

data class UpdateApproximationImpl(
    val schema: PluginDataFrameSchema, val columns: ColumnsResolver, val where: Boolean = false
) : UpdateApproximation {
    override fun withWhere(): UpdateApproximation {
        return copy(where = true)
    }
}

abstract class UpdatePerCol(name: String) : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: UpdateApproximation by arg()
    val Arguments.typeArg1: ConeKotlinType by arg(lens = Interpreter.Id)
    val Arguments.param by ignore(ArgumentName.of(name))

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return updateWithImpl(
            receiver,
            // think here the library always expects "update values" to be the same type as original type
            // so no extra type information can be extracted for target.
            targetMarkedNullable = typeArg1.isMarkedNullable,
            originalType = typeArg1.wrap()
        )
    }
}

class UpdatePerColRow : UpdatePerCol("values")
class UpdatePerColMap : UpdatePerCol("values")
class UpdatePerColLambda : UpdatePerCol("valueSelector")
class UpdatePerRowCol : UpdatePerCol("expression")

class UpdateWith0 : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: UpdateApproximation by arg()
    val Arguments.typeArg1: ConeKotlinType by arg(lens = Interpreter.Id)
    val Arguments.target: ColumnType by type(ArgumentName.of("expression"))

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return updateWithImpl(receiver, target.coneType.isMarkedNullable, typeArg1.wrap())
    }
}

private fun Arguments.updateWithImpl(
    receiver: UpdateApproximation,
    targetMarkedNullable: Boolean,
    originalType: ColumnType,
): PluginDataFrameSchema {
    return when (val receiver = receiver) {
        is FillNullsApproximation -> receiver.schema.convertAsColumn(receiver.columns) { original ->
            val originalType = if (original is SimpleDataColumn) original.type else originalType
            val nullable = originalType.coneType.isMarkedNullable && (targetMarkedNullable || receiver.where)
            val updatedType = originalType.coneType.withNullability(
                nullable = nullable,
                session.typeContext
            )
            simpleColumnOf(original.name, updatedType)
        }
        is UpdateApproximationImpl -> receiver.schema.convertAsColumn(receiver.columns) { original ->
            val originalType = if (original is SimpleDataColumn) original.type else originalType
            val nullable = targetMarkedNullable || (receiver.where && originalType.coneType.isMarkedNullable)
            val updatedType = originalType.coneType.withNullability(
                nullable = nullable,
                session.typeContext
            )
            simpleColumnOf(original.name, updatedType)
        }
    }
}

class UpdateNotNullWith : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: UpdateApproximation by arg()
    val Arguments.target: ColumnType by type(ArgumentName.of("expression"))
    val Arguments.typeArg1: ConeKotlinType by arg(lens = Interpreter.Id)

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return updateWithImpl(receiver.withWhere(), target.coneType.isMarkedNullable, typeArg1.wrap())
    }
}

class UpdateNotNull : AbstractInterpreter<UpdateApproximation>() {
    val Arguments.receiver: UpdateApproximation by arg()
    override fun Arguments.interpret(): UpdateApproximation {
        return receiver.withWhere()
    }
}

class UpdateWithNull : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: UpdateApproximation by arg()
    val Arguments.typeArg1: ConeKotlinType by arg(lens = Interpreter.Id)
    override fun Arguments.interpret(): PluginDataFrameSchema {
        return updateWithImpl(receiver, targetMarkedNullable = true, typeArg1.wrap())
    }
}

class UpdateWithZero : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: UpdateApproximation by arg()
    val Arguments.typeArg1: ConeKotlinType by arg(lens = Interpreter.Id)
    override fun Arguments.interpret(): PluginDataFrameSchema {
        return updateWithImpl(receiver, targetMarkedNullable = false, typeArg1.wrap())
    }
}
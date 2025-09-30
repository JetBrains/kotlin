package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlin.fir.types.isMarkedNullable
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.fir.types.withNullability
import org.jetbrains.kotlinx.dataframe.plugin.extensions.wrap
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractSchemaModificationInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.Arguments
import org.jetbrains.kotlinx.dataframe.plugin.impl.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.convert
import org.jetbrains.kotlinx.dataframe.plugin.impl.dataFrame
import org.jetbrains.kotlinx.dataframe.plugin.impl.ignore
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
    val Arguments.typeArg1: TypeApproximation by type()
    val Arguments.param by ignore(ArgumentName.of(name))

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return updateWithImpl(receiver, targetMarkedNullable = typeArg1.type.isMarkedNullable)
    }
}

class UpdatePerColRow : UpdatePerCol("values")
class UpdatePerColMap : UpdatePerCol("values")
class UpdatePerColLambda : UpdatePerCol("valueSelector")
class UpdatePerRowCol : UpdatePerCol("expression")

class UpdateWith0 : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: UpdateApproximation by arg()
    val Arguments.target: TypeApproximation by type(ArgumentName.of("expression"))

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return updateWithImpl(receiver, target.type.isMarkedNullable)
    }
}

private fun Arguments.updateWithImpl(receiver: UpdateApproximation, targetMarkedNullable: Boolean): PluginDataFrameSchema {
    return when (val receiver = receiver) {
        is FillNullsApproximation -> receiver.schema.convert(receiver.columns) { original ->
            val nullable = original.type.isMarkedNullable && (targetMarkedNullable || receiver.where)
            original.type.withNullability(
                nullable = nullable,
                session.typeContext
            ).wrap()
        }
        is UpdateApproximationImpl -> receiver.schema.convert(receiver.columns) { original ->
            val nullable = targetMarkedNullable || (receiver.where && original.type.isMarkedNullable)
            original.type.withNullability(
                nullable = nullable,
                session.typeContext
            ).wrap()
        }
    }
}

class UpdateNotNullWith : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: UpdateApproximation by arg()
    val Arguments.target: TypeApproximation by type(ArgumentName.of("expression"))

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return updateWithImpl(receiver.withWhere(), target.type.isMarkedNullable)
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
    override fun Arguments.interpret(): PluginDataFrameSchema {
        return updateWithImpl(receiver, targetMarkedNullable = true)
    }
}

class UpdateWithZero : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: UpdateApproximation by arg()
    override fun Arguments.interpret(): PluginDataFrameSchema {
        return updateWithImpl(receiver, targetMarkedNullable = false)
    }
}
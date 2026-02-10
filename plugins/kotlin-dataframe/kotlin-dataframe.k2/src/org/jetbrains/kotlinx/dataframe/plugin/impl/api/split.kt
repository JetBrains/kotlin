/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlin.fir.plugin.createConeType
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.plugin.extensions.ColumnType
import org.jetbrains.kotlinx.dataframe.plugin.extensions.wrap
import org.jetbrains.kotlinx.dataframe.plugin.impl.*
import org.jetbrains.kotlinx.dataframe.plugin.pluginDataFrameSchema

data class SplitApproximation(
    val df: PluginDataFrameSchema,
    val columns: ColumnsResolver,
)

class Split0 : AbstractInterpreter<SplitApproximation>() {
    val Arguments.receiver by dataFrame()
    val Arguments.columns: ColumnsResolver by arg()

    override fun Arguments.interpret(): SplitApproximation {
        return SplitApproximation(receiver, columns)
    }
}

data class SplitWithTransformApproximation(
    val df: PluginDataFrameSchema,
    val columns: ColumnsResolver,
    val targetType: ColumnType,
    val default: ColumnType?,
)

class ByIterable : AbstractInterpreter<SplitWithTransformApproximation>() {
    val Arguments.receiver: SplitApproximation by arg()
    val Arguments.typeArg2 by type()
    val Arguments.splitter by ignore()

    override fun Arguments.interpret(): SplitWithTransformApproximation {
        return SplitWithTransformApproximation(receiver.df, receiver.columns, typeArg2, null)
    }
}

class SplitDefault : AbstractInterpreter<SplitWithTransformApproximation>() {
    val Arguments.receiver: SplitApproximation by arg()
    val Arguments.typeArg2 by type()
    val Arguments.value by type()

    override fun Arguments.interpret(): SplitWithTransformApproximation {
        return SplitWithTransformApproximation(receiver.df, receiver.columns, typeArg2, value)
    }
}

abstract class ByDelimiters : AbstractInterpreter<SplitWithTransformApproximation>() {
    val Arguments.receiver: SplitApproximation by arg()
    val Arguments.delimiters by ignore()
    val Arguments.trim by ignore()
    val Arguments.ignoreCase by ignore()
    val Arguments.limit by ignore()

    override fun Arguments.interpret(): SplitWithTransformApproximation {
        return SplitWithTransformApproximation(
            receiver.df,
            receiver.columns,
            targetType = session.builtinTypes.stringType.coneType.wrap(),
            default = null
        )
    }
}

class ByCharDelimiters : ByDelimiters()

class ByStringDelimiters : ByDelimiters()

class ByRegex : AbstractInterpreter<SplitWithTransformApproximation>() {
    val Arguments.receiver: SplitApproximation by arg()
    val Arguments.regex by ignore()
    val Arguments.trim by ignore()
    val Arguments.limit by ignore()

    override fun Arguments.interpret(): SplitWithTransformApproximation {
        return SplitWithTransformApproximation(
            receiver.df,
            receiver.columns,
            targetType = session.builtinTypes.stringType.coneType.wrap(),
            default = null
        )
    }
}

abstract class AbstractMatch : AbstractInterpreter<SplitWithTransformApproximation>() {
    val Arguments.receiver: SplitApproximation by arg()
    val Arguments.regex by ignore()

    override fun Arguments.interpret(): SplitWithTransformApproximation {
        return SplitWithTransformApproximation(
            receiver.df,
            receiver.columns,
            targetType = session.builtinTypes.stringType.coneType.wrap(),
            default = null
        )
    }
}

class MatchStringRegex : AbstractMatch()

class MatchRegex : AbstractMatch()

abstract class SplitWithTransformAbstractOperation : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: SplitWithTransformApproximation by arg()
    val Arguments.names: List<String> by arg()
    val Arguments.extraNamesGenerator by ignore()

    abstract fun SplitWithTransform<ConeTypesAdapter, Any, ColumnType>.operation(names: List<String>): DataFrame<ConeTypesAdapter>

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return receiver.df.split(receiver.columns) {
            val default = receiver.default
            val nullable = default == null || default.coneType.isMarkedNullable
            val type = receiver.targetType.coneType.withNullability(nullable = nullable, session.typeContext).wrap()
            List(names.size) { type }
        }
            .operation(names)
            .toPluginDataFrameSchema()
    }
}

fun PluginDataFrameSchema.split(
    columns: ColumnsResolver,
    targetType: () -> List<ColumnType>
): SplitWithTransform<ConeTypesAdapter, Any, ColumnType> {
    val resolvedColumns = columns.resolve(this)
    if (resolvedColumns.size != 1) error("Compiler plugin only supports split of 1 column, but was $resolvedColumns")

    return asDataFrame(impliedColumnsResolver = columns).split { columns }.by { targetType() }
}

class SplitWithTransformInto0 : SplitWithTransformAbstractOperation() {
    override fun SplitWithTransform<ConeTypesAdapter, Any, ColumnType>.operation(names: List<String>): DataFrame<ConeTypesAdapter> {
        return into(names)
    }
}

class SplitWithTransformInward0 : SplitWithTransformAbstractOperation() {
    override fun SplitWithTransform<ConeTypesAdapter, Any, ColumnType>.operation(names: List<String>): DataFrame<ConeTypesAdapter> {
        return inward(names)
    }
}

class SplitInplace : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: SplitApproximation by arg()
    val Arguments.typeArg2 by type()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return receiver.df
            .convertAsColumn(receiver.columns) {
                simpleColumnOf("", createListType(typeArg2.coneType))
            }
    }
}

class SplitWithTransformInplace : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: SplitWithTransformApproximation by arg()
    val Arguments.typeArg2 by type()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return receiver.df
            .convertAsColumn(receiver.columns) {
                simpleColumnOf("", createListType(typeArg2.coneType))
            }
    }
}

class SplitWithTransformDefault : AbstractInterpreter<SplitWithTransformApproximation>() {
    val Arguments.receiver: SplitWithTransformApproximation by arg()
    val Arguments.value by type()

    override fun Arguments.interpret(): SplitWithTransformApproximation {
        return receiver.copy(default = value)
    }
}

class SplitWithTransformIntoRows : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: SplitWithTransformApproximation by arg()
    val Arguments.dropEmpty: Boolean by arg(defaultValue = Present(true))

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return receiver.df.convertAsColumn(receiver.columns) {
            val targetProjection = arrayOf(receiver.targetType.coneType.toTypeProjection(Variance.INVARIANT))
            simpleColumnOf("", StandardClassIds.List.createConeType(session, targetProjection))
        }.explodeImpl(dropEmpty, receiver.columns)
    }
}

class SplitIntoRows : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: SplitApproximation by arg()
    val Arguments.dropEmpty: Boolean by arg(defaultValue = Present(true))

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return receiver.df.explodeImpl(dropEmpty, receiver.columns)
    }
}

class SplitAnyFrameRows : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: SplitApproximation by arg()
    val Arguments.dropEmpty: Boolean by arg(defaultValue = Present(true))

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return receiver.df.explodeImpl(dropEmpty, receiver.columns)
    }
}

abstract class SplitPair : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: SplitApproximation by arg()
    val Arguments.typeArg1 by type()
    val Arguments.typeArg2 by type()
    val Arguments.firstCol: String by arg()
    val Arguments.secondCol: String by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return receiver.df
            .split(receiver.columns) {
                listOf(typeArg1, typeArg2)
            }
            .operation()
            .toPluginDataFrameSchema()
    }

    context(arguments: Arguments)
    abstract fun SplitWithTransform<ConeTypesAdapter, Any, ColumnType>.operation(): DataFrame<ConeTypesAdapter>
}

class SplitPairInto : SplitPair() {
    context(arguments: Arguments)
    override fun SplitWithTransform<ConeTypesAdapter, Any, ColumnType>.operation(): DataFrame<ConeTypesAdapter> {
        return into(arguments.firstCol, arguments.secondCol)
    }
}

class SplitPairInward : SplitPair() {
    context(arguments: Arguments)
    override fun SplitWithTransform<ConeTypesAdapter, Any, ColumnType>.operation(): DataFrame<ConeTypesAdapter> {
        return inward(arguments.firstCol, arguments.secondCol)
    }
}

class SplitAnyFrameIntoColumns : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: SplitApproximation by arg()
    val Arguments.typeArg1 by type()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val schemaArgument = typeArg1.coneType.typeArguments.getOrNull(0) ?: return PluginDataFrameSchema.EMPTY
        val columns = pluginDataFrameSchema(schemaArgument)
            .columns()
            .map { implode(it) }

        return receiver.df.asDataFrame()
            .convert { receiver.columns.single() }
            .asColumn { SimpleColumnGroup(it.name(), columns).asDataColumn() }
            .toPluginDataFrameSchema()
    }
}

internal fun Arguments.createListType(type: ConeKotlinType): ConeClassLikeType = StandardClassIds.List.createConeType(
    session,
    arrayOf(type.toTypeProjection(Variance.INVARIANT)),
)

abstract class SplitIterableAbstractOperation : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: SplitApproximation by arg()
    val Arguments.names: List<String> by arg()
    val Arguments.extraNamesGenerator by ignore()
    val Arguments.typeArg1 by type()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        // any empty list introduces null to all "split" columns, so they must be nullable
        val targetType = (typeArg1.coneType.typeArguments.getOrNull(0) as? ConeKotlinTypeProjection)?.type?.withNullability(
            nullable = true,
            session.typeContext
        )?.wrap()

        if (targetType == null) return PluginDataFrameSchema.EMPTY

        return receiver.df
            .split(receiver.columns) { List(names.size) { targetType } }
            .operation()
            .toPluginDataFrameSchema()
    }

    context(arguments: Arguments)
    abstract fun SplitWithTransform<ConeTypesAdapter, Any, ColumnType>.operation(): DataFrame<ConeTypesAdapter>
}

class SplitIterableInto : SplitIterableAbstractOperation() {
    context(arguments: Arguments)
    override fun SplitWithTransform<ConeTypesAdapter, Any, ColumnType>.operation(): DataFrame<ConeTypesAdapter> {
        return into(arguments.names)
    }
}

class SplitIterableInward : SplitIterableAbstractOperation() {
    context(arguments: Arguments)
    override fun SplitWithTransform<ConeTypesAdapter, Any, ColumnType>.operation(): DataFrame<ConeTypesAdapter> {
        return inward(arguments.names)
    }
}
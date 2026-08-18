/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.plugin.createConeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeStarProjection
import org.jetbrains.kotlin.fir.types.commonSuperTypeOrNull
import org.jetbrains.kotlin.fir.types.isMarkedNullable
import org.jetbrains.kotlin.fir.types.isNullableNothing
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.fir.types.withNullability
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlinx.dataframe.api.addAll
import org.jetbrains.kotlinx.dataframe.api.remove
import org.jetbrains.kotlinx.dataframe.api.toPath
import org.jetbrains.kotlinx.dataframe.columns.toColumnSet
import org.jetbrains.kotlinx.dataframe.plugin.extensions.KotlinTypeFacade
import org.jetbrains.kotlinx.dataframe.plugin.extensions.approximateIntersections
import org.jetbrains.kotlinx.dataframe.plugin.extensions.isList
import org.jetbrains.kotlinx.dataframe.plugin.extensions.typeArgument
import org.jetbrains.kotlinx.dataframe.plugin.extensions.wrap
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractSchemaModificationInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.Arguments
import org.jetbrains.kotlinx.dataframe.plugin.impl.Interpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleCol
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleColumnGroup
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleDataColumn
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleFrameColumn
import org.jetbrains.kotlinx.dataframe.plugin.impl.asDataColumn
import org.jetbrains.kotlinx.dataframe.plugin.impl.asDataFrame
import org.jetbrains.kotlinx.dataframe.plugin.impl.data.ColumnWithPathApproximation
import org.jetbrains.kotlinx.dataframe.plugin.impl.dataFrame
import org.jetbrains.kotlinx.dataframe.plugin.impl.ignore
import org.jetbrains.kotlinx.dataframe.plugin.impl.makeNullable
import org.jetbrains.kotlinx.dataframe.plugin.impl.simpleColumnOf
import org.jetbrains.kotlinx.dataframe.plugin.impl.toPluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.type
import org.jetbrains.kotlinx.dataframe.plugin.utils.Names

data class GatherApproximation(
    val df: PluginDataFrameSchema,
    val columns: ColumnsResolver,
    val mapKeys: ConeKotlinType? = null,
    val mapValues: ConeKotlinType? = null,
    val explode: Boolean = false,
    val notNull: Boolean = false,
)

class Gather0 : AbstractInterpreter<GatherApproximation>() {
    val Arguments.receiver by dataFrame()
    val Arguments.selector: ColumnsResolver by arg()

    override fun Arguments.interpret(): GatherApproximation {
        return GatherApproximation(receiver, selector)
    }
}

class GatherWhere : AbstractInterpreter<GatherApproximation>() {
    val Arguments.receiver: GatherApproximation by arg()
    val Arguments.filter by ignore()

    override fun Arguments.interpret(): GatherApproximation {
        return receiver
    }
}

class GatherExplodeLists : AbstractInterpreter<GatherApproximation>() {
    val Arguments.receiver: GatherApproximation by arg()

    override fun Arguments.interpret(): GatherApproximation {
        return receiver.copy(explode = true)
    }
}

class GatherChangeType : AbstractInterpreter<GatherApproximation>() {
    val Arguments.functionCall: FirFunctionCall by arg(lens = Interpreter.Id)
    val Arguments.receiver: GatherApproximation by arg()

    override fun Arguments.interpret(): GatherApproximation {
        return if (functionCall.calleeReference.name == Name.identifier("notNull")) {
            receiver.copy(notNull = true)
        } else {
            receiver
        }
    }
}

class GatherMap : AbstractInterpreter<GatherApproximation>() {
    val Arguments.functionCall: FirFunctionCall by arg(lens = Interpreter.Id)
    val Arguments.receiver: GatherApproximation by arg()
    val Arguments.typeArg2 by type()
    val Arguments.typeArg3 by type()
    val Arguments.transform by ignore()

    override fun Arguments.interpret(): GatherApproximation {
        return when (functionCall.calleeReference.name) {
            Name.identifier("mapKeys") -> receiver
            Name.identifier("mapValues") -> receiver.copy(mapValues = typeArg3.coneType)
            else -> {
                error("${functionCall.calleeReference.name} annotated with @Interpretable(\"${GatherMap::class.simpleName}\") is an error")
            }
        }
    }
}

class GatherInto : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: GatherApproximation by arg()
    val Arguments.keyColumn: String by arg()
    val Arguments.valueColumn: String by arg()
    val Arguments.typeArg2 by type()
    val Arguments.typeArg3 by type()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return gatherIntoImpl(
            receiver,
            keyColumn = TargetColumn(keyColumn, typeArg2.coneType),
            valueColumn = TargetColumn(valueColumn, typeArg3.coneType)
        )
    }
}

class GatherKeysInto : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: GatherApproximation by arg()
    val Arguments.keyColumn: String by arg()
    val Arguments.typeArg2 by type()
    override fun Arguments.interpret(): PluginDataFrameSchema {
        return gatherIntoImpl(receiver, keyColumn = TargetColumn(keyColumn, typeArg2.coneType))
    }
}

class GatherValuesInto : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: GatherApproximation by arg()
    val Arguments.valueColumn: String by arg()
    val Arguments.typeArg3 by type()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return gatherIntoImpl(receiver, valueColumn = TargetColumn(valueColumn, typeArg3.coneType))
    }
}

private fun Arguments.gatherIntoImpl(
    receiver: GatherApproximation,
    keyColumn: TargetColumn? = null,
    valueColumn: TargetColumn? = null,
): PluginDataFrameSchema {
    val columnsToGather = receiver.columns.resolve(receiver.df)
    val removeResult = receiver.df.asDataFrame()
        .remove { columnsToGather.map { it.path.toPath() }.toColumnSet() }
    val keys = keyColumn?.let {
        simpleColumnOf(it.name, it.type)
    }
    val values = valueColumn?.let {
        valueColumn(receiver, columnsToGather, it.name, it.type)
    }
    return removeResult.addAll(listOfNotNull(keys, values).map { it.asDataColumn() }).toPluginDataFrameSchema()
}

private class TargetColumn(val name: String, val type: ConeKotlinType)

private fun Arguments.valueColumn(
    gather: GatherApproximation,
    columnsToGather: List<ColumnWithPathApproximation>,
    name: String,
    resultType: ConeKotlinType,
): SimpleCol {
    val columnGroups = columnsToGather.map { it.column }.filterIsInstance<SimpleColumnGroup>()
    val values = if (gather.mapValues == null && columnGroups.size == columnsToGather.size) {
        columnGroups.reduce { schema, otherSchema ->
            SimpleColumnGroup(name, mergeRows(schema, otherSchema))
        }.rename(name)
    } else {
        val fallback = if (gather.notNull && gather.mapValues == null) {
            resultType.withNullability(false, session.typeContext)
        } else {
            resultType
        }
        val valuesType = gather.mapValues ?: valuesType(columnsToGather, gather.explode, fallback)
        simpleColumnOf(name, valuesType)
    }
    return values
}

context(facade: KotlinTypeFacade)
fun mergeRows(schema: SimpleColumnGroup, otherSchema: SimpleColumnGroup): List<SimpleCol> {
    // mergePairs
    val intersections = (schema.columns() + otherSchema.columns()).groupBy { it.name }
        .mapValues {
            if (it.value.size == 1) {
                it.value + simpleColumnOf(it.key, facade.session.builtinTypes.nullableNothingType.coneType)
            } else {
                it.value
            }
        }
    return intersections.map { entry ->
        val cols = entry.value
        cols[0].merge(cols[1])
    }
}

context(facade: KotlinTypeFacade)
fun SimpleCol.merge(other: SimpleCol): SimpleCol {
    return when (this) {
        is SimpleColumnGroup -> merge(other)
        is SimpleDataColumn -> merge(other)
        is SimpleFrameColumn -> merge(other)
    }
}

context(facade: KotlinTypeFacade)
fun SimpleDataColumn.merge(other: SimpleCol): SimpleCol {
    return when (other) {
        is SimpleColumnGroup -> merge(other, this)
        is SimpleDataColumn -> merge(this, other)
        is SimpleFrameColumn -> merge(other, this)
    }
}

context(facade: KotlinTypeFacade)
fun SimpleColumnGroup.merge(other: SimpleCol): SimpleCol {
    return when (other) {
        is SimpleColumnGroup -> SimpleColumnGroup(name, mergeRows(other, this))
        is SimpleDataColumn -> merge(this, other)
        is SimpleFrameColumn -> SimpleFrameColumn(name, intersectFrameColumnSchemas(columns(), other.columns()))
    }
}

context(facade: KotlinTypeFacade)
fun SimpleFrameColumn.merge(other: SimpleCol): SimpleCol {
    return when (other) {
        is SimpleColumnGroup -> SimpleFrameColumn(name, intersectFrameColumnSchemas(columns(), other.columns()))
        is SimpleDataColumn -> merge(this, other)
        is SimpleFrameColumn -> SimpleFrameColumn(name, intersectFrameColumnSchemas(columns(), other.columns()))
    }
}

context(facade: KotlinTypeFacade)
fun merge(col1: SimpleDataColumn, col2: SimpleDataColumn): SimpleDataColumn {
    fun SimpleDataColumn.changeListNullability(nullable: Boolean) =
        changeType(type.coneType.withNullability(nullable, facade.session.typeContext).wrap())

    return when {
        col1.type.isList() && col1.type.typeArgument().coneType == col2.type.coneType -> col1
        col2.type.isList() && col2.type.typeArgument().coneType == col1.type.coneType -> col2
        col1.type.isList() && sameTypeIgnoringNullability(col1.type.typeArgument().coneType, col2.type.coneType) ->
            col1.changeListNullability(col2.type.coneType.isMarkedNullable)
        col2.type.isList() && sameTypeIgnoringNullability(col2.type.typeArgument().coneType, col1.type.coneType) ->
            col2.changeListNullability(col1.type.coneType.isMarkedNullable)
        col1.type.isList() && col2.type.coneType.isNullableNothing -> col1
        col2.type.isList() && col1.type.coneType.isNullableNothing -> col2
        else -> SimpleDataColumn(col1.name, commonColumnType(col1.type.coneType, col2.type.coneType)!!.wrap())
    }
}

context(facade: KotlinTypeFacade)
private fun sameTypeIgnoringNullability(first: ConeKotlinType, second: ConeKotlinType): Boolean {
    val typeContext = facade.session.typeContext
    return first.withNullability(false, typeContext) == second.withNullability(false, typeContext)
}

context(facade: KotlinTypeFacade)
private fun commonColumnType(first: ConeKotlinType, second: ConeKotlinType): ConeKotlinType? {
    val type = facade.session.typeContext.commonSuperTypeOrNull(listOf(first, second)) ?: return null
    return type.approximateIntersections()
}

context(facade: KotlinTypeFacade)
fun merge(col1: SimpleColumnGroup, col2: SimpleDataColumn): SimpleCol {
    return if (col2.type.coneType.isNullableNothing) {
        makeNullable(col1)
    } else {
        simpleColumnOf(
            col1.name,
            facade.session.builtinTypes.anyType.coneType.withNullability(
                col2.type.coneType.isMarkedNullable,
                facade.session.typeContext
            )
        )
    }
}

context(facade: KotlinTypeFacade)
fun merge(col1: SimpleFrameColumn, col2: SimpleDataColumn): SimpleCol {
    return if (col2.type.coneType.isNullableNothing) {
        col1
    } else {
        simpleColumnOf(
            col1.name,
            facade.session.builtinTypes.anyType.coneType.withNullability(
                col2.type.coneType.isMarkedNullable,
                facade.session.typeContext
            )
        )
    }
}

context(facade: KotlinTypeFacade)
fun intersectFrameColumnSchemas(cols1: List<SimpleCol>, cols2: List<SimpleCol>): List<SimpleCol> {
    val schema1 = cols1.associateBy { it.name }
    val schema2 = cols2.associateBy { it.name }
    val names = schema1.keys intersect schema2.keys
    return names.map { name ->
        val first = schema1.getValue(name)
        val second = schema2.getValue(name)
        when (first) {
            is SimpleDataColumn -> if (second is SimpleDataColumn) merge(first, second) else first.merge(second)
            is SimpleFrameColumn -> if (second is SimpleFrameColumn) {
                SimpleFrameColumn(first.name, intersectFrameColumnSchemas(first.columns(), second.columns()))
            } else {
                first.merge(second)
            }
            is SimpleColumnGroup -> if (second is SimpleColumnGroup) {
                SimpleColumnGroup(first.name, intersectFrameColumnSchemas(first.columns(), second.columns()))
            } else {
                first.merge(second)
            }
        }
    }
}

private fun Arguments.valuesType(
    columnsToGather: List<ColumnWithPathApproximation>,
    explode: Boolean,
    fallback: ConeKotlinType,
): ConeKotlinType {
    return if (explode) {
        val types = columnsToGather.map {
            val column = it.column
            explodeLists(column)
        }
        this.session.typeContext.commonSuperTypeOrNull(types) ?: fallback
    } else {
        fallback
    }
}

private fun Arguments.explodeLists(column: SimpleCol): ConeKotlinType = when (column) {
    is SimpleDataColumn -> {
        if (column.type.isList()) {
            column.type.typeArgument().coneType
        } else {
            column.type.coneType
        }
    }

    is SimpleFrameColumn -> {
        Names.DF_CLASS_ID.createConeType(session, arrayOf(ConeStarProjection))
    }

    is SimpleColumnGroup -> {
        Names.DATA_ROW_CLASS_ID.createConeType(session, arrayOf(ConeStarProjection))
    }
}

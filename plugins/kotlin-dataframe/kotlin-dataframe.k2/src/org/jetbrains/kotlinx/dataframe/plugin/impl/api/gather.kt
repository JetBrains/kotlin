/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.plugin.createConeType
import org.jetbrains.kotlin.fir.types.ConeIntersectionType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeStarProjection
import org.jetbrains.kotlin.fir.types.commonSuperTypeOrNull
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.fir.types.withNullability
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlinx.dataframe.api.addAll
import org.jetbrains.kotlinx.dataframe.api.remove
import org.jetbrains.kotlinx.dataframe.api.toPath
import org.jetbrains.kotlinx.dataframe.columns.toColumnSet
import org.jetbrains.kotlinx.dataframe.plugin.extensions.SessionContext
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
    val notNull: Boolean = false
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
    valueColumn: TargetColumn? = null
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
    resultType: ConeKotlinType
): SimpleCol {
    val columnGroups = columnsToGather.map { it.column }.filterIsInstance<SimpleColumnGroup>()
    val values = if (columnGroups.size == columnsToGather.size) {
        val cols = columnGroups.map { it.columns() }.reduce { schema, otherSchema ->
            session.intersect(schema, otherSchema)
        }
        SimpleColumnGroup(name, cols)
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

// keep columns present in both schemas by the name with common supertype / common schema
fun FirSession.intersect(schema: List<SimpleCol>, otherSchema: List<SimpleCol>): List<SimpleCol> {
    val intersection = mutableMapOf<String, List<SimpleCol>>()
    (schema + otherSchema).forEach {
        intersection.compute(it.name) { _, u ->
            (u ?: emptyList()) + it
        }
    }
    return intersection.mapNotNull { (name, cols) ->
        if (cols.size != 2) return@mapNotNull null
        val col1 = cols[0]
        val col2 = cols[1]
        if (col1 is SimpleDataColumn && col2 is SimpleDataColumn) {
            val type = typeContext.commonSuperTypeOrNull(listOf(col1.type.coneType, col2.type.coneType))
            val realType = if (type is ConeIntersectionType) builtinTypes.nullableAnyType.coneType else type
            realType?.let { SimpleDataColumn(name, context(SessionContext(this)) { it.wrap() }) }
        } else if (col1 is SimpleColumnGroup && col2 is SimpleColumnGroup) {
            val res = intersect(col1.columns(), col2.columns())
            SimpleColumnGroup(name, res)
        } else {
            null
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
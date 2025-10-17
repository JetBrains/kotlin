/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlinx.dataframe.api.single
import org.jetbrains.kotlinx.dataframe.impl.aggregation.aggregators.Aggregator
import org.jetbrains.kotlinx.dataframe.impl.aggregation.aggregators.Aggregators
import org.jetbrains.kotlinx.dataframe.plugin.extensions.Marker
import org.jetbrains.kotlinx.dataframe.plugin.impl.*
import org.jetbrains.kotlinx.dataframe.plugin.utils.isPrimitiveOrMixedNumber
import org.jetbrains.kotlinx.dataframe.plugin.utils.isSelfComparable
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.createType

private object PrimitiveClassIds {
    const val INT = "kotlin/Int"
    const val LONG = "kotlin/Long"
    const val DOUBLE = "kotlin/Double"
    const val FLOAT = "kotlin/Float"
    const val SHORT = "kotlin/Short"
    const val BYTE = "kotlin/Byte"
    const val CHAR = "kotlin/Char"
}

private fun KClass<*>.toClassId(): ClassId? = when (this) {
    Int::class -> ClassId.fromString(PrimitiveClassIds.INT)
    Long::class -> ClassId.fromString(PrimitiveClassIds.LONG)
    Double::class -> ClassId.fromString(PrimitiveClassIds.DOUBLE)
    Float::class -> ClassId.fromString(PrimitiveClassIds.FLOAT)
    Short::class -> ClassId.fromString(PrimitiveClassIds.SHORT)
    Byte::class -> ClassId.fromString(PrimitiveClassIds.BYTE)
    Char::class -> ClassId.fromString(PrimitiveClassIds.CHAR)
    else -> null
}

private val primitiveTypeMap = mapOf(
    PrimitiveClassIds.INT to Int::class,
    PrimitiveClassIds.LONG to Long::class,
    PrimitiveClassIds.DOUBLE to Double::class,
    PrimitiveClassIds.FLOAT to Float::class,
    PrimitiveClassIds.SHORT to Short::class,
    PrimitiveClassIds.BYTE to Byte::class,
    PrimitiveClassIds.CHAR to Byte::class,
)

@Deprecated("Use more expressive asPrimitiveToKTypeOrNull", level = DeprecationLevel.ERROR)
fun ConeKotlinType.toKType(): KType? = asPrimitiveToKTypeOrNull()

fun ConeKotlinType.asPrimitiveToKTypeOrNull(): KType? {
    return (this as? ConeClassLikeType)?.let { coneType ->
        val nullable = coneType.isMarkedNullable
        primitiveTypeMap[coneType.lookupTag.classId.asString()]
            ?.createType(nullable = nullable)
    }
}

fun KType.toConeKotlinType(): ConeKotlinType? {
    val kClass = this.classifier as? KClass<*> ?: return null
    val classId = kClass.toClassId() ?: return null

    return classId.constructClassLikeType(
        typeArguments = emptyArray(),
        isMarkedNullable = this.isMarkedNullable
    )
}

internal fun Arguments.generateStatisticResultColumns(
    statisticAggregator: Aggregator<*, *>,
    inputColumns: List<SimpleDataColumn>,
): List<SimpleCol> {
    return inputColumns.map { col -> createColumnWithUpdatedType(col, statisticAggregator) }
}

internal fun Arguments.generateStatisticResultColumn(
    statisticAggregator: Aggregator<*, *>,
    inputColumn: SimpleDataColumn,
): SimpleCol {
    return createColumnWithUpdatedType(inputColumn, statisticAggregator)
}

private fun Arguments.createColumnWithUpdatedType(
    column: SimpleDataColumn,
    statisticAggregator: Aggregator<*, *>,
): SimpleCol {
    val originalType = column.type.type
    val inputKType = originalType.asPrimitiveToKTypeOrNull()
    // we can only get KTypes of primitives, keep the original type otherwise
        ?: return simpleColumnOf(column.name, originalType)

    // we don't know whether the column is empty or not at runtime,
    // it's safest to assume the worst-case scenario and consider it empty
    // this will introduce nullability, but never runtime errors
    val resultKType = statisticAggregator.calculateReturnType(inputKType, emptyInput = true)
    val updatedType = resultKType.toConeKotlinType()
        ?: error("Can't convert $resultKType to ConeKotlinType. This should not happen.")
    return simpleColumnOf(column.name, updatedType)
}

internal val skipNaN = true
internal val ddofDefault: Int = 1
internal val percentileArg: Double = 30.0
internal val sum = Aggregators.sum(skipNaN)
internal val mean = Aggregators.mean(skipNaN)
internal val std = Aggregators.std(skipNaN, ddofDefault)
internal val median = Aggregators.median(skipNaN)
internal val min = Aggregators.min.invoke(skipNaN)
internal val max = Aggregators.max.invoke(skipNaN)
internal val percentile = Aggregators.percentile(percentileArg, skipNaN)

/** [ColumnsResolver] that takes all top-level [primitive- or mixed-number][isPrimitiveOrMixedNumber] columns. */
internal val Arguments.numericStatisticsDefaultColumns: ColumnsResolver
    get() = columnsResolver {
        cols {
            (it.single() as Marker).type.isPrimitiveOrMixedNumber(session)
        }
    }

/** [ColumnsResolver] that takes all top-level [self-comparable][isSelfComparable] columns. */
internal val Arguments.comparableStatisticsDefaultColumns: ColumnsResolver
    get() = columnsResolver {
        cols {
            (it.single() as Marker).type.isSelfComparable(session)
        }
    }

/** Returns `true` if this column is of type `DataColumn<T>` where `T : Comparable<T & Any>` */
internal fun SimpleDataColumn.isIntraComparable(session: FirSession): Boolean = type.type.isSelfComparable(session)

/** Adds to the schema only numerical columns. */
abstract class Aggregator0(val aggregator: Aggregator<*, *>) : AbstractSchemaModificationInterpreter() {
    private val Arguments.receiver: PluginDataFrameSchema by dataFrame()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val resolvedColumns = numericStatisticsDefaultColumns.resolve(receiver)
            .map { (it.column as SimpleDataColumn) }
        val newColumns = generateStatisticResultColumns(aggregator, resolvedColumns)

        return PluginDataFrameSchema(newColumns)
    }
}

class Sum0 : Aggregator0(sum)

class Mean0 : Aggregator0(mean)

class Std0 : Aggregator0(std)

/** Adds to the schema only numerical columns. */
abstract class AggregatorIntraComparable0(val aggregator: Aggregator<*, *>) : AbstractSchemaModificationInterpreter() {
    private val Arguments.receiver: PluginDataFrameSchema by dataFrame()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val resolvedColumns = comparableStatisticsDefaultColumns.resolve(receiver)
            .map { (it.column as SimpleDataColumn) }
        val newColumns = generateStatisticResultColumns(aggregator, resolvedColumns)

        return PluginDataFrameSchema(newColumns)
    }
}

class Median0 : AggregatorIntraComparable0(median)

class Max0 : AggregatorIntraComparable0(max)

class Min0 : AggregatorIntraComparable0(min)

class Percentile0 : AggregatorIntraComparable0(percentile) {
    val Arguments.percentile by ignore()
}

/** Adds to the schema all resolved columns. */
abstract class Aggregator1(val aggregator: Aggregator<*, *>) : AbstractSchemaModificationInterpreter() {
    private val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    private val Arguments.columns: ColumnsResolver by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val resolvedColumns = columns.resolve(receiver).map { it.column }.filterIsInstance<SimpleDataColumn>()

        val newColumns = generateStatisticResultColumns(aggregator, resolvedColumns)

        return PluginDataFrameSchema(newColumns)
    }
}

class Sum1 : Aggregator1(sum)

class Mean1 : Aggregator1(mean)

class Std1 : Aggregator1(std)

class Median1 : Aggregator1(median)

class Max1 : Aggregator1(max)

class Min1 : Aggregator1(min)

class Percentile1 : Aggregator1(percentile) {
    val Arguments.percentile by ignore()
}

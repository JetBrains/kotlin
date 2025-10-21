package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlinx.dataframe.api.remove
import org.jetbrains.kotlinx.dataframe.impl.aggregation.aggregators.Aggregator
import org.jetbrains.kotlinx.dataframe.plugin.InterpretationErrorReporter
import org.jetbrains.kotlinx.dataframe.plugin.extensions.KotlinTypeFacade
import org.jetbrains.kotlinx.dataframe.plugin.impl.*
import org.jetbrains.kotlinx.dataframe.plugin.impl.data.ColumnWithPathApproximation
import org.jetbrains.kotlinx.dataframe.plugin.interpret
import org.jetbrains.kotlinx.dataframe.plugin.loadInterpreter

class GroupBy(val keys: PluginDataFrameSchema, val groups: PluginDataFrameSchema) {
    companion object {
        val EMPTY = GroupBy(PluginDataFrameSchema.EMPTY, PluginDataFrameSchema.EMPTY)
    }
}

class DataFrameGroupBy : AbstractInterpreter<GroupBy>() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.moveToTop: Boolean by arg(defaultValue = Present(true))
    val Arguments.cols: ColumnsResolver by arg()

    override fun Arguments.interpret(): GroupBy {
        return GroupBy(keys = createPluginDataFrameSchema(cols.resolve(receiver), moveToTop), groups = receiver)
    }
}

class AsGroupBy : AbstractInterpreter<GroupBy>() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.selector: ColumnsResolver by arg()

    override fun Arguments.interpret(): GroupBy {
        val column = selector.resolve(receiver).singleOrNull()?.column
        return if (column is SimpleFrameColumn) {
            GroupBy(receiver.asDataFrame().remove { selector }.toPluginDataFrameSchema(), PluginDataFrameSchema(column.columns()))
        } else {
            GroupBy.EMPTY
        }
    }
}

class AsGroupByDefault : AbstractInterpreter<GroupBy>() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()

    override fun Arguments.interpret(): GroupBy {
        val groups = receiver.columns().singleOrNull { it is SimpleFrameColumn } as? SimpleFrameColumn
        return if (groups != null) {
            GroupBy(receiver.asDataFrame().remove(groups.name).toPluginDataFrameSchema(), PluginDataFrameSchema(groups.columns()))
        } else {
            GroupBy.EMPTY
        }
    }
}

class NamedValue(val name: String, val type: ConeKotlinType)

class GroupByDsl {
    val columns = mutableListOf<NamedValue>()
}

class AggregateDslInto : AbstractInterpreter<Unit>() {
    val Arguments.dsl: GroupByDsl by arg()
    val Arguments.receiver: FirExpression by arg(lens = Interpreter.Id)
    val Arguments.name: String by arg()

    override fun Arguments.interpret() {
        dsl.columns.add(NamedValue(name, receiver.resolvedType))
    }
}

class Aggregate : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: GroupBy by groupBy()
    val Arguments.body: FirAnonymousFunctionExpression by arg(lens = Interpreter.Id)
    override fun Arguments.interpret(): PluginDataFrameSchema {
        return aggregate(
            receiver,
            InterpretationErrorReporter.DEFAULT,
            body,
            defaultAggregate = true
        )
    }
}

class AggregateRow : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.body: FirAnonymousFunctionExpression by arg(lens = Interpreter.Id)
    override fun Arguments.interpret(): PluginDataFrameSchema {
        return aggregate(
            GroupBy(PluginDataFrameSchema.EMPTY, receiver),
            InterpretationErrorReporter.DEFAULT,
            body,
            defaultAggregate = false
        )
    }
}

fun KotlinTypeFacade.aggregate(
    groupBy: GroupBy,
    reporter: InterpretationErrorReporter,
    firAnonymousFunctionExpression: FirAnonymousFunctionExpression,
    defaultAggregate: Boolean,
): PluginDataFrameSchema {
    val body = firAnonymousFunctionExpression.anonymousFunction.body
    val lastExpression = (body?.statements?.lastOrNull() as? FirReturnExpression)?.result
    val type = lastExpression?.resolvedType ?: return PluginDataFrameSchema.EMPTY
    val aggregated = if (
        defaultAggregate &&
        type != session.builtinTypes.unitType &&
        type.classId != ClassId(FqName("org.jetbrains.kotlinx.dataframe.aggregation"), Name.identifier("NamedValue")) &&
        type.classId != ClassId(FqName("org.jetbrains.kotlinx.dataframe.impl.api"), Name.identifier("AggregatedPivot"))
    ) {
        listOf(simpleColumnOf("aggregated", type))
    } else {
        val dsl = GroupByDsl()
        val calls = buildList {
            body.statements.filterIsInstance<FirFunctionCall>().let { addAll(it) }
            if (lastExpression is FirFunctionCall) add(lastExpression)
        }
        calls.forEach { call ->
            val schemaProcessor = call.loadInterpreter() ?: return@forEach
            interpret(
                call,
                schemaProcessor,
                mapOf("dsl" to Interpreter.Success(dsl)),
                reporter
            )
        }

        dsl.columns.map {
            simpleColumnOf(it.name, it.type)
        }
    }
    return PluginDataFrameSchema(groupBy.keys.columns() + aggregated)
}

fun KotlinTypeFacade.createPluginDataFrameSchema(
    keys: List<ColumnWithPathApproximation>,
    moveToTop: Boolean,
): PluginDataFrameSchema {
    fun addToHierarchy(
        path: List<String>,
        column: SimpleCol,
        columns: List<SimpleCol>,
    ): List<SimpleCol> {
        if (path.isEmpty()) return columns

        val groupName = path[0]
        val remainingPath = path.drop(1)

        val updatedColumns = columns.map {
            if (it is SimpleColumnGroup && it.name == groupName) {
                SimpleColumnGroup(it.name, columns = addToHierarchy(remainingPath, column, it.columns()))
            } else {
                it
            }
        }

        return if (updatedColumns.any { it is SimpleColumnGroup && it.name == groupName }) {
            updatedColumns
        } else {
            val newGroup = if (remainingPath.isEmpty()) {
                column
            } else {
                SimpleColumnGroup(groupName, addToHierarchy(remainingPath, column, emptyList()))
            }
            updatedColumns + newGroup
        }
    }

    var rootColumns: List<SimpleCol> = emptyList()

    if (moveToTop) {
        rootColumns = keys.map { it.column }
    } else {
        for (key in keys) {
            val path = key.path.path
            val column = key.column
            rootColumns = addToHierarchy(path, column, rootColumns)
        }
    }


    return PluginDataFrameSchema(rootColumns)
}

class ConcatWithKeys : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver by groupBy()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val originalColumns = receiver.groups.columns()
        return PluginDataFrameSchema(
            originalColumns + receiver.keys.columns().filter { it.name !in originalColumns.map { it.name } })
    }
}

class GroupByInto : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: GroupBy by groupBy()
    val Arguments.column: String by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val grouped = listOf(SimpleFrameColumn(column, receiver.groups.columns()))
        return PluginDataFrameSchema(
            receiver.keys.columns() + grouped
        )
    }
}

class GroupByToDataFrame : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: GroupBy by groupBy()
    val Arguments.groupedColumnName: String? by arg(defaultValue = Present(null))

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val grouped = listOf(SimpleFrameColumn(groupedColumnName ?: "group", receiver.groups.columns()))
        return PluginDataFrameSchema(
            receiver.keys.columns() + grouped
        )
    }
}

class GroupByAdd : AbstractInterpreter<GroupBy>() {
    val Arguments.receiver: GroupBy by groupBy()
    val Arguments.name: String by arg()
    val Arguments.infer by ignore()
    val Arguments.type: TypeApproximation by type(name("expression"))

    override fun Arguments.interpret(): GroupBy {
        return GroupBy(receiver.keys, receiver.groups.add(name, type.coneType, context = this))
    }
}

// region GroupByOf

/**
 * Implementation of `df.groupBy { ... }.xOf { row expression }`
 * Produces a single aggregated column based on the expression type.
 */
abstract class GroupByAggregatorOf(val defaultName: String, val aggregator: Aggregator<*, *>) : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver by groupBy()
    val Arguments.name: String? by arg(defaultValue = Present(null))
    val Arguments.expression by type()

    override fun Arguments.interpret(): PluginDataFrameSchema =
        interpretGroupByAggregatorOf(
            receiver = receiver,
            name = name ?: defaultName,
            aggregator = aggregator,
            expressionReturnType = expression,
        )
}

/**
 * See [GroupByAggregatorOf].
 *
 * Implementation for `df.groupBy { ... }.sumOf { row expression }` specifically because its argument is named
 * "resultName" instead of "name".
 */
abstract class GroupByAggregatorSumOf(val defaultName: String) : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver by groupBy()

    // NOTE! due to legacy it's called "resultName" instead of "name"
    val Arguments.resultName: String? by arg(defaultValue = Present(null))
    val Arguments.expression by type()

    override fun Arguments.interpret(): PluginDataFrameSchema =
        interpretGroupByAggregatorOf(
            receiver = receiver,
            name = resultName ?: defaultName,
            aggregator = sum,
            expressionReturnType = expression,
        )
}

private fun Arguments.interpretGroupByAggregatorOf(
    receiver: GroupBy,
    name: String,
    aggregator: Aggregator<*, *>,
    expressionReturnType: TypeApproximation,
): PluginDataFrameSchema {
    val aggregatedCol = makeNullable(simpleColumnOf(name, expressionReturnType.coneType))
    val typeAdjustedCol = generateStatisticResultColumn(aggregator, aggregatedCol as SimpleDataColumn)
    return PluginDataFrameSchema(receiver.keys.columns() + typeAdjustedCol)
}

/** Implementation for `df.groupBy { ... }.sumOf {}`. */
class GroupBySumOf : GroupByAggregatorSumOf(defaultName = "sum")

/** Implementation for `df.groupBy { ... }.meanOf {}`. */
class GroupByMeanOf : GroupByAggregatorOf(defaultName = "mean", mean)

/** Implementation for `df.groupBy { ... }.stdOf {}`. */
class GroupByStdOf : GroupByAggregatorOf(defaultName = "std", std) {
    val Arguments.ddof by ignore()
}

/** Implementation for `df.groupBy { ... }.medianOf {}`. */
class GroupByMedianOf : GroupByAggregatorOf(defaultName = "median", median)

/** Implementation for `df.groupBy { ... }.percentileOf {}`. */
class GroupByPercentileOf : GroupByAggregatorOf(defaultName = "percentile", percentile) {
    val Arguments.percentile by ignore()
}

/** Implementation for `df.groupBy { ... }.minOf {}`. */
class GroupByMinOf : GroupByAggregatorOf(defaultName = "min", min)

/** Implementation for `df.groupBy { ... }.maxOf {}`. */
class GroupByMaxOf : GroupByAggregatorOf(defaultName = "max", max)

// endregion
// region GroupByFor

/**
 * Implementation for `df.groupBy { ... }.xFor { cols }`
 *
 * Produces multiple aggregated columns.
 */
abstract class GroupByForAggregator0(val aggregator: Aggregator<*, *>) : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver by groupBy()
    val Arguments.skipNaN by ignore()
    val Arguments.columns: ColumnsResolver by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema =
        interpretGroupByForAggregator(
            receiver = receiver,
            aggregator = aggregator,
            columns = columns,
        )
}

private fun Arguments.interpretGroupByForAggregator(
    receiver: GroupBy,
    aggregator: Aggregator<*, *>,
    columns: ColumnsResolver,
): PluginDataFrameSchema {
    val resolvedColumns = columns.resolve(receiver.groups).map { (it.column as SimpleDataColumn) }
    val newColumns = generateStatisticResultColumns(aggregator, resolvedColumns)
    return PluginDataFrameSchema(receiver.keys.columns() + newColumns)
}

/** Implementation for `df.groupBy { ... }.sumFor {}`. */
class GroupBySum0 : GroupByForAggregator0(sum)

/** Implementation for `df.groupBy { ... }.meanFor {}`. */
class GroupByMean0 : GroupByForAggregator0(mean)

/** Implementation for `df.groupBy { ... }.stdFor {}`. */
class GroupByStd0 : GroupByForAggregator0(std) {
    val Arguments.ddof by ignore()
}

/** Implementation for `df.groupBy { ... }.medianFor {}`. */
class GroupByMedian0 : GroupByForAggregator0(median)

/** Implementation for `df.groupBy { ... }.percentileFor {}`. */
class GroupByPercentile0 : GroupByForAggregator0(percentile) {
    val Arguments.percentile by ignore()
}

/** Implementation for `df.groupBy { ... }.minFor {}`. */
class GroupByMin0 : GroupByForAggregator0(min)

/** Implementation for `df.groupBy { ... }.maxFor {}`. */
class GroupByMax0 : GroupByForAggregator0(max)

// endregion
// region GroupByDefault

/**
 * Implementation for `df.groupBy { ... }.x()`.
 * -> `df.groupBy { ... }.xFor { default selection of cols }`
 * Adds to the schema only numerical columns.
 */
abstract class GroupByAggregatorNumbers1(val aggregator: Aggregator<*, *>) : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver by groupBy()
    val Arguments.skipNaN by ignore()

    override fun Arguments.interpret(): PluginDataFrameSchema =
        interpretGroupByForAggregator(
            receiver = receiver,
            aggregator = aggregator,
            columns = numericStatisticsDefaultColumns,
        )
}

/** Implementation for `df.groupBy { ... }.sum()`. */
class GroupBySum1 : GroupByAggregatorNumbers1(sum)

/** Implementation for `df.groupBy { ... }.mean()`. */
class GroupByMean1 : GroupByAggregatorNumbers1(mean)

/** Implementation for `df.groupBy { ... }.std()`. */
class GroupByStd1 : GroupByAggregatorNumbers1(std) {
    val Arguments.ddof by ignore()
}

/**
 * Implementation for `df.groupBy { ... }.x()`.
 * -> `df.groupBy { ... }.xFor { default selection of cols }`
 * Keeps in schema only columns with intraComparable values.
 */
abstract class GroupByAggregatorComparable1(val aggregator: Aggregator<*, *>) : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver by groupBy()
    val Arguments.skipNaN by ignore()

    override fun Arguments.interpret(): PluginDataFrameSchema =
        interpretGroupByForAggregator(
            receiver = receiver,
            aggregator = aggregator,
            columns = comparableStatisticsDefaultColumns,
        )
}

/** Implementation for `df.groupBy { ... }.median()`. */
class GroupByMedian1 : GroupByAggregatorComparable1(median)

/** Implementation for `df.groupBy { ... }.percentile()`. */
class GroupByPercentile1 : GroupByAggregatorComparable1(percentile) {
    val Arguments.percentile by ignore()
}

/** Implementation for `df.groupBy { ... }.min()`. */
class GroupByMin1 : GroupByAggregatorComparable1(min)

/** Implementation for `df.groupBy { ... }.max()`. */
class GroupByMax1 : GroupByAggregatorComparable1(max)

// endregion
// region GroupByMultipleCols

/**
 * Implementation for `df.groupBy { ... }.x { cols }`.
 * Creates one column with aggregated type with optionally given name.
 */
abstract class GroupByAggregator2(val defaultName: String, val aggregator: Aggregator<*, *>) : AbstractSchemaModificationInterpreter() {
    // return type for `columns`
    // will be `null` when mixed number columns are provided
    val Arguments.typeArg1: TypeApproximation? by arg(defaultValue = Present(null))

    val Arguments.receiver by groupBy()
    val Arguments.name: String? by arg(defaultValue = Present(null))
    val Arguments.skipNaN by ignore()
    val Arguments.columns: ColumnsResolver by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val selectedColumns = columns.resolve(receiver.groups)
        val newColumnName = name ?: selectedColumns.singleOrNull()?.column?.name ?: defaultName
        val selectedColumnTypes = selectedColumns.mapNotNull {
            (it.column as? SimpleDataColumn)?.type?.coneType
        }.toSet()

        val returnType = typeArg1?.coneType

        // if all our columns are primitives, ask the aggregator what the return type will be given the selected columns
        // else, the type will always be the same as the return type of the selection dsl
        val newColumnType = when {
            selectedColumnTypes.all { it.isPrimitiveOrNullablePrimitive } ->
                aggregator.calculateReturnTypeMultipleColumns(
                    colTypes = selectedColumnTypes.map { it.asPrimitiveToKTypeOrNull()!! }.toSet(),
                    // we don't know whether the column is empty or not at runtime,
                    // it's safest to assume the worst-case scenario and consider it empty
                    // this will introduce nullability, but never runtime errors
                    colsEmpty = true,
                ).toConeKotlinType() ?: returnType

            else -> returnType
        } ?: session.builtinTypes.nullableAnyType.coneType

        val newColumn = simpleColumnOf(newColumnName, newColumnType)
        return PluginDataFrameSchema(receiver.keys.columns() + newColumn)
    }
}

/** Implementation for `df.groupBy { ... }.sum {}`. */
class GroupBySum2 : GroupByAggregator2("sum", sum)

/** Implementation for `df.groupBy { ... }.mean {}`. */
class GroupByMean2 : GroupByAggregator2("mean", mean)

/** Implementation for `df.groupBy { ... }.std {}`. */
class GroupByStd2 : GroupByAggregator2("std", std) {
    val Arguments.ddof by ignore()
}

/** Implementation for `df.groupBy { ... }.median {}`. */
class GroupByMedian2 : GroupByAggregator2("median", median)

/** Implementation for `df.groupBy { ... }.percentile {}`. */
class GroupByPercentile2 : GroupByAggregator2("percentile", percentile) {
    val Arguments.percentile by ignore()
}

/** Implementation for `df.groupBy { ... }.min {}`. */
class GroupByMin2 : GroupByAggregator2("min", min)

/** Implementation for `df.groupBy { ... }.max {}`. */
class GroupByMax2 : GroupByAggregator2("max", max)

// endregion
// region GroupByCumSum

/**
 * Handling `df.groupBy { ... }.cumSum(skipNA) { cols }`
 */
class GroupByCumSum : AbstractInterpreter<GroupBy>() {
    val Arguments.receiver by groupBy()
    val Arguments.skipNA: Boolean by arg(defaultValue = Present(defaultCumSumSkipNA))
    val Arguments.columns: ColumnsResolver by arg()

    override fun Arguments.interpret(): GroupBy =
        GroupBy(
            keys = receiver.keys,
            groups = getSchemaAfterCumSum(
                dataSchema = receiver.groups,
                selectedColumns = columns,
            ),
        )
}

/**
 * Handling `df.groupBy { ... }.cumSum(skipNA)`
 */
class GroupByCumSum0 : AbstractInterpreter<GroupBy>() {
    val Arguments.receiver by groupBy()
    val Arguments.skipNA: Boolean by arg(defaultValue = Present(defaultCumSumSkipNA))

    override fun Arguments.interpret(): GroupBy =
        GroupBy(
            keys = receiver.keys,
            groups = getSchemaAfterCumSum(
                dataSchema = receiver.groups,
                selectedColumns = cumSumDefaultColumns,
            ),
        )
}

// endregion

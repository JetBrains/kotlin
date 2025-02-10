package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlinx.dataframe.plugin.InterpretationErrorReporter
import org.jetbrains.kotlinx.dataframe.plugin.extensions.KotlinTypeFacade
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractSchemaModificationInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.Arguments
import org.jetbrains.kotlinx.dataframe.plugin.impl.Interpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.Present
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleCol
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleColumnGroup
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleFrameColumn
import org.jetbrains.kotlinx.dataframe.plugin.impl.add
import org.jetbrains.kotlinx.dataframe.plugin.impl.data.ColumnWithPathApproximation
import org.jetbrains.kotlinx.dataframe.plugin.impl.dataFrame
import org.jetbrains.kotlinx.dataframe.plugin.impl.groupBy
import org.jetbrains.kotlinx.dataframe.plugin.impl.ignore
import org.jetbrains.kotlinx.dataframe.plugin.impl.makeNullable
import org.jetbrains.kotlinx.dataframe.plugin.impl.simpleColumnOf
import org.jetbrains.kotlinx.dataframe.plugin.impl.type
import org.jetbrains.kotlinx.dataframe.plugin.interpret
import org.jetbrains.kotlinx.dataframe.plugin.loadInterpreter

class GroupBy(val keys: PluginDataFrameSchema, val groups: PluginDataFrameSchema)

class DataFrameGroupBy : AbstractInterpreter<GroupBy>() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.moveToTop: Boolean by arg(defaultValue = Present(true))
    val Arguments.cols: ColumnsResolver by arg()

    override fun Arguments.interpret(): GroupBy {
        return GroupBy(keys = createPluginDataFrameSchema(cols.resolve(receiver), moveToTop), groups = receiver)
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
            body
        )
    }
}

fun KotlinTypeFacade.aggregate(
    groupBy: GroupBy,
    reporter: InterpretationErrorReporter,
    firAnonymousFunctionExpression: FirAnonymousFunctionExpression
): PluginDataFrameSchema {
    val body = firAnonymousFunctionExpression.anonymousFunction.body
    val lastExpression = (body?.statements?.lastOrNull() as? FirReturnExpression)?.result
    val type = lastExpression?.resolvedType
    return if (type != session.builtinTypes.unitType) {
        val dsl = GroupByDsl()
        val calls = buildList {
            body?.statements?.filterIsInstance<FirFunctionCall>()?.let { addAll(it) }
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

        val cols = groupBy.keys.columns() + dsl.columns.map {
            simpleColumnOf(it.name, it.type)
        }
        PluginDataFrameSchema(cols)
    } else {
        PluginDataFrameSchema.EMPTY
    }
}

fun KotlinTypeFacade.createPluginDataFrameSchema(keys: List<ColumnWithPathApproximation>, moveToTop: Boolean): PluginDataFrameSchema {
    fun addToHierarchy(
        path: List<String>,
        column: SimpleCol,
        columns: List<SimpleCol>
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
        return GroupBy(receiver.keys, receiver.groups.add(name, type.type, context = this))
    }
}

abstract class GroupByAggregator(val defaultName: String) : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver by groupBy()
    val Arguments.name: String? by arg(defaultValue = Present(null))
    val Arguments.expression by type()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val aggregated = makeNullable(simpleColumnOf(name ?: defaultName, expression.type))
        return PluginDataFrameSchema(receiver.keys.columns() + aggregated)
    }
}

class GroupByMaxOf : GroupByAggregator(defaultName = "max")
class GroupByMinOf : GroupByAggregator(defaultName = "min")

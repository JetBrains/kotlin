package org.jetbrains.kotlin.fir.dataframe.api

import org.jetbrains.kotlin.fir.dataframe.InterpretationErrorReporter
import org.jetbrains.kotlin.fir.dataframe.interpret
import org.jetbrains.kotlin.fir.dataframe.loadInterpreter
import org.jetbrains.kotlin.fir.dataframe.pluginDataFrameSchema
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeNullability
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlinx.dataframe.DATA_ROW_CLASS_ID
import org.jetbrains.kotlinx.dataframe.KotlinTypeFacade
import org.jetbrains.kotlinx.dataframe.annotations.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.annotations.AbstractSchemaModificationInterpreter
import org.jetbrains.kotlinx.dataframe.annotations.Arguments
import org.jetbrains.kotlinx.dataframe.annotations.Interpreter
import org.jetbrains.kotlinx.dataframe.annotations.Present
import org.jetbrains.kotlinx.dataframe.plugin.ColumnWithPathApproximation
import org.jetbrains.kotlinx.dataframe.plugin.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.SimpleCol
import org.jetbrains.kotlinx.dataframe.plugin.SimpleColumnGroup
import org.jetbrains.kotlinx.dataframe.plugin.SimpleFrameColumn
import org.jetbrains.kotlinx.dataframe.plugin.dataFrame

class GroupBy(val df: PluginDataFrameSchema, val keys: List<ColumnWithPathApproximation>, val moveToTop: Boolean)

class DataFrameGroupBy : AbstractInterpreter<GroupBy>() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.moveToTop: Boolean by arg(defaultValue = Present(true))
    val Arguments.cols: List<ColumnWithPathApproximation> by arg()

    override fun Arguments.interpret(): GroupBy {
        return GroupBy(receiver, cols, moveToTop)
    }
}

class NamedValue(val name: String, val type: ConeKotlinType)

class GroupByDsl {
    val columns = mutableListOf<NamedValue>()
}

class GroupByInto : AbstractInterpreter<Unit>() {
    val Arguments.dsl: GroupByDsl by arg()
    val Arguments.receiver: FirExpression by arg(lens = Interpreter.Id)
    val Arguments.name: String by arg()

    override fun Arguments.interpret() {
        dsl.columns.add(NamedValue(name, receiver.resolvedType))
    }
}

fun KotlinTypeFacade.aggregate(
    groupByCall: FirFunctionCall,
    interpreter: Interpreter<*>,
    reporter: InterpretationErrorReporter,
    call: FirFunctionCall
): PluginDataFrameSchema? {
    val groupBy = interpret(groupByCall, interpreter, reporter = reporter)?.value as? GroupBy ?: return null
    val aggregate = call.argumentList.arguments.singleOrNull() as? FirAnonymousFunctionExpression
    val body = aggregate?.anonymousFunction?.body ?: return null
    val lastExpression = (body.statements.lastOrNull() as? FirReturnExpression)?.result
    val type = lastExpression?.resolvedType
    return if (type != session.builtinTypes.unitType) {
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

        // important to create FrameColumns, nullable DataRows?
        val cols = createPluginDataFrameSchema(groupBy.keys, groupBy.moveToTop).columns() + dsl.columns.map {
            when (it.type.classId) {
                DATA_ROW_CLASS_ID -> {
                    when (it.type.nullability) {
                        ConeNullability.NULLABLE -> SimpleCol(
                            it.name,
                            org.jetbrains.kotlinx.dataframe.annotations.TypeApproximation(it.type)
                        )
                        ConeNullability.UNKNOWN -> SimpleCol(
                            it.name,
                            org.jetbrains.kotlinx.dataframe.annotations.TypeApproximation(it.type)
                        )
                        ConeNullability.NOT_NULL -> {
                            val typeProjection = it.type.typeArguments[0]
                            SimpleColumnGroup(it.name, pluginDataFrameSchema(typeProjection).columns(), anyRow)
                        }
                    }
                }
                else -> SimpleCol(it.name, org.jetbrains.kotlinx.dataframe.annotations.TypeApproximation(it.type))
            }
        }
        PluginDataFrameSchema(cols)
    } else {
        null
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
                SimpleColumnGroup(it.name, columns = addToHierarchy(remainingPath, column, it.columns()), anyRow)
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
                SimpleColumnGroup(groupName, addToHierarchy(remainingPath, column, emptyList()), anyRow)
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

class GroupByToDataFrame : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: GroupBy by arg()
    val Arguments.groupedColumnName: String? by arg(defaultValue = Present(null))

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val grouped = listOf(SimpleFrameColumn(groupedColumnName ?: "group", receiver.df.columns(), anyDataFrame))
        return PluginDataFrameSchema(
            createPluginDataFrameSchema(receiver.keys, receiver.moveToTop).columns() + grouped
        )
    }
}

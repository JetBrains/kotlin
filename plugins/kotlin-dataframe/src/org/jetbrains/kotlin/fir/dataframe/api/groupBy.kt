package org.jetbrains.kotlin.fir.dataframe.api

import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlinx.dataframe.KotlinTypeFacade
import org.jetbrains.kotlinx.dataframe.annotations.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.annotations.Arguments
import org.jetbrains.kotlinx.dataframe.annotations.Interpreter
import org.jetbrains.kotlinx.dataframe.annotations.Present
import org.jetbrains.kotlinx.dataframe.annotations.TypeApproximation
import org.jetbrains.kotlinx.dataframe.plugin.ColumnWithPathApproximation
import org.jetbrains.kotlinx.dataframe.plugin.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.SimpleCol
import org.jetbrains.kotlinx.dataframe.plugin.SimpleColumnGroup
import org.jetbrains.kotlinx.dataframe.plugin.dataFrame
import org.jetbrains.kotlinx.dataframe.plugin.type

class GroupBy(val df: PluginDataFrameSchema, val keys: List<ColumnWithPathApproximation>, val moveToTop: Boolean)

class DataFrameGroupBy : AbstractInterpreter<GroupBy>() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.moveToTop: Boolean by arg(defaultValue = Present(true))
    val Arguments.cols: List<ColumnWithPathApproximation> by arg()

    override fun Arguments.interpret(): GroupBy {
        return GroupBy(receiver, cols, moveToTop)
    }
}

class A(val name: String, val type: ConeKotlinType)

class GroupByDsl {
    val columns = mutableListOf<A>()
}

class GroupByInto : AbstractInterpreter<Unit>() {
    val Arguments.dsl: GroupByDsl by arg()
    val Arguments.receiver: FirExpression by arg(lens = Interpreter.Id)
    val Arguments.name: String by arg()

    override fun Arguments.interpret() {
        dsl.columns.add(A(name, receiver.resolvedType))
    }
}

fun KotlinTypeFacade.createPluginDataFrameSchema(keys: List<ColumnWithPathApproximation>): PluginDataFrameSchema {
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

    for (key in keys) {
        val path = key.path.path
        val column = key.column
        rootColumns = addToHierarchy(path, column, rootColumns)
    }

    return PluginDataFrameSchema(rootColumns)
}

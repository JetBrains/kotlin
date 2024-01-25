package org.jetbrains.kotlinx.dataframe.plugin

import org.jetbrains.kotlinx.dataframe.KotlinTypeFacade
import org.jetbrains.kotlinx.dataframe.annotations.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.annotations.Arguments
import org.jetbrains.kotlinx.dataframe.annotations.Present

internal class Explode0 : AbstractInterpreter<PluginDataFrameSchema>() {
    val Arguments.dropEmpty: Boolean by arg(defaultValue = Present(true))
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.selector: List<ColumnWithPathApproximation>? by arg(defaultValue = Present(null))
    override val Arguments.startingSchema get() = receiver

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val columns = selector ?: TODO()
        return receiver.explodeImpl(dropEmpty, columns.map { ColumnPathApproximation(it.path.path) })
    }
}

public val KotlinTypeFacade.explodeImpl: PluginDataFrameSchema.(dropEmpty: Boolean, selector: List<ColumnPathApproximation>?) -> PluginDataFrameSchema get()  = { dropEmpty, selector ->
    val columns = selector ?: TODO()

    val selected: Set<List<String>> = columns.map { it.path }.toSet()

    fun makeNullable(column: SimpleCol): SimpleCol {
        return when (column) {
            is SimpleColumnGroup -> SimpleColumnGroup(column.name, column.columns().map { makeNullable(it) }, column.type)
            is SimpleFrameColumn -> column
            else -> {
//                val nullable = if (dropEmpty) (column.type as TypeApproximationImpl).nullable else true

                column.changeType(type = column.type.changeNullability { nullable -> if (dropEmpty) nullable else true })
            }
        }
    }

    fun explode(column: SimpleCol, path: List<String>): SimpleCol {
        val fullPath = path + listOf(column.name)
        return when (column) {
            is SimpleColumnGroup -> {
                SimpleColumnGroup(column.name, column.columns().map { explode(it, fullPath) }, column.type)
            }
            is SimpleFrameColumn -> {
                if (fullPath in selected) {
                    SimpleColumnGroup(column.name, column.columns().map { makeNullable(it) }, anyDataFrame)
                } else {
                    column
                }
            }
            else -> if (fullPath in selected) {
                val newType = when {
                    column.type.isList() -> column.type.typeArgument()
                    else -> column.type
                }
                SimpleCol(column.name, newType)
            } else {
                column
            }
        }
    }

    PluginDataFrameSchema(
        columns().map { column ->
            explode(column, emptyList())
        }
    )
}

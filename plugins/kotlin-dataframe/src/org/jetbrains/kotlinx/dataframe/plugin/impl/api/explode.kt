package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.plugin.extensions.KotlinTypeFacade
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.Arguments
import org.jetbrains.kotlinx.dataframe.plugin.impl.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.Present
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleCol
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleDataColumn
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleColumnGroup
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleFrameColumn
import org.jetbrains.kotlinx.dataframe.plugin.impl.data.ColumnPathApproximation
import org.jetbrains.kotlinx.dataframe.plugin.impl.data.ColumnWithPathApproximation
import org.jetbrains.kotlinx.dataframe.plugin.impl.dataFrame

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

val KotlinTypeFacade.explodeImpl: PluginDataFrameSchema.(dropEmpty: Boolean, selector: List<ColumnPathApproximation>?) -> PluginDataFrameSchema
    get()  = { dropEmpty, selector ->
    val columns = selector ?: TODO()

    val selected: Set<List<String>> = columns.map { it.path }.toSet()

    fun makeNullable(column: SimpleCol): SimpleCol {
        return when (column) {
            is SimpleColumnGroup -> SimpleColumnGroup(column.name, column.columns().map { makeNullable(it) })
            is SimpleFrameColumn -> column
            is SimpleDataColumn -> {
//                val nullable = if (dropEmpty) (column.type as TypeApproximationImpl).nullable else true

                column.changeType(type = column.type.changeNullability { nullable -> if (dropEmpty) nullable else true })
            }
        }
    }

    fun explode(column: SimpleCol, path: List<String>): SimpleCol {
        val fullPath = path + listOf(column.name)
        return when (column) {
            is SimpleColumnGroup -> {
                SimpleColumnGroup(column.name, column.columns().map { explode(it, fullPath) })
            }
            is SimpleFrameColumn -> {
                if (fullPath in selected) {
                    SimpleColumnGroup(column.name, column.columns().map { makeNullable(it) })
                } else {
                    column
                }
            }
            is SimpleDataColumn -> if (fullPath in selected) {
                val newType = when {
                    column.type.isList() -> column.type.typeArgument()
                    else -> column.type
                }
                SimpleDataColumn(column.name, newType)
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

package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.plugin.extensions.KotlinTypeFacade
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.Arguments
import org.jetbrains.kotlinx.dataframe.plugin.impl.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.Present
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleCol
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleColumnGroup
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleDataColumn
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleFrameColumn
import org.jetbrains.kotlinx.dataframe.plugin.impl.data.ColumnWithPathApproximation
import org.jetbrains.kotlinx.dataframe.plugin.impl.dataFrame
import org.jetbrains.kotlinx.dataframe.plugin.impl.simpleColumnOf

internal class Explode0 : AbstractInterpreter<PluginDataFrameSchema>() {
    val Arguments.dropEmpty: Boolean by arg(defaultValue = Present(true))
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.selector: ColumnsResolver? by arg(defaultValue = Present(null))
    override val Arguments.startingSchema get() = receiver

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val columns = selector ?: object : ColumnsResolver {
            override fun resolve(df: PluginDataFrameSchema): List<ColumnWithPathApproximation> {
                return df.flatten(includeFrames = false).filter {
                    val column = it.column
                    column is SimpleFrameColumn || column is SimpleDataColumn && column.type.isList()
                }
            }
        }
        return receiver.explodeImpl(dropEmpty, columns.resolve(receiver))
    }
}

val KotlinTypeFacade.explodeImpl: PluginDataFrameSchema.(dropEmpty: Boolean, selector: List<ColumnWithPathApproximation>) -> PluginDataFrameSchema
    get()  = { dropEmpty, selector ->
    val columns = selector

    val selected = columns.associateBy { it.path }

    fun makeNullable(column: SimpleCol): SimpleCol {
        return when (column) {
            is SimpleColumnGroup -> SimpleColumnGroup(column.name, column.columns().map { makeNullable(it) })
            is SimpleFrameColumn -> column
            is SimpleDataColumn -> {
                column.changeType(type = column.type.changeNullability { nullable -> selector.size > 1 || !dropEmpty || nullable })
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
                val s = selected[fullPath]
                if (s != null) {
                    SimpleColumnGroup(s.column.name, column.columns().map { makeNullable(it) })
                } else {
                    column
                }
            }
            is SimpleDataColumn -> {
                val s = selected[fullPath]
                if (s != null) {
                    val newType = when {
                        column.type.isList() -> column.type.typeArgument()
                        else -> column.type
                    }
                    makeNullable(simpleColumnOf(s.column.name, newType.type))
                } else {
                    column
                }
            }
        }
    }

    PluginDataFrameSchema(
        columns().map { column ->
            explode(column, emptyList())
        }
    )
}

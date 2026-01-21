package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.ColumnsSelector
import org.jetbrains.kotlinx.dataframe.api.cast
import org.jetbrains.kotlinx.dataframe.api.getColumnsWithPaths
import org.jetbrains.kotlinx.dataframe.columns.*
import org.jetbrains.kotlinx.dataframe.plugin.impl.*
import org.jetbrains.kotlinx.dataframe.plugin.impl.data.ColumnWithPathApproximation

internal class And10 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.other: ColumnsResolver by arg()
    val Arguments.receiver: ColumnsResolver by arg()

    override fun Arguments.interpret(): ColumnsResolver {
        return object : ColumnsResolver {
            override fun resolve(df: PluginDataFrameSchema): List<ColumnWithPathApproximation> {
                return receiver.resolve(df) + other.resolve(df)
            }
        }
    }
}

interface SingleColumnApproximation : ColumnsResolver, SingleColumn<Any?>, ColumnReference<Any?> {
    override fun resolve(df: PluginDataFrameSchema): List<ColumnWithPathApproximation>

    override fun resolve(context: ColumnResolutionContext): List<ColumnWithPath<Any?>>

    override fun resolveSingle(context: ColumnResolutionContext): ColumnWithPath<Any?>? {
        return resolve(context).single()
    }

    val path: ColumnPath
}

class ResolvedDataColumn(private val col: ColumnWithPathApproximation) : SingleColumnApproximation {

    override fun name(): String {
        return col.column.name
    }

    override fun rename(newName: String): ColumnReference<Any?> {
        return ResolvedDataColumn(ColumnWithPathApproximation(col.path, col.column.rename(newName)))
    }

    override fun resolve(df: PluginDataFrameSchema): List<ColumnWithPathApproximation> {
        return listOf(col)
    }

    override fun resolve(context: ColumnResolutionContext): List<ColumnWithPath<Any?>> {
        return listOf(resolveSingle(context))
    }

    override fun resolveSingle(context: ColumnResolutionContext): ColumnWithPath<Any?> {
        return ColumnWithPath(col.column.asDataColumn(), col.path)
    }

    override val path: ColumnPath = col.path
}

interface ColumnsResolver : ColumnSet<Any?> {
    fun resolve(df: PluginDataFrameSchema): List<ColumnWithPathApproximation>

    override fun resolve(context: ColumnResolutionContext): List<ColumnWithPath<Any?>> {
        val schema = context.df.cast<ConeTypesAdapter>().toPluginDataFrameSchema()
        return resolve(schema).map { ColumnWithPath(it.column.asDataColumn(), it.path) }
    }
}

abstract class ColumnsResolverAdapter : ColumnSet<Any?>, ColumnsResolver {
    override fun resolve(df: PluginDataFrameSchema): List<ColumnWithPathApproximation> {
        return resolve(ColumnResolutionContext(df.asDataFrame(), UnresolvedColumnsPolicy.Skip))
            .map { ColumnWithPathApproximation(it.path, it.data.asSimpleColumn()) }
    }
}

fun columnsResolver(f: ColumnsSelector<*, *>): ColumnsResolver {
    return object : ColumnsResolverAdapter() {
        override fun resolve(context: ColumnResolutionContext): List<ColumnWithPath<Any?>> {
            return context.df.getColumnsWithPaths(f)
        }
    }
}

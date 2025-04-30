package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.ColumnsContainer
import org.jetbrains.kotlinx.dataframe.api.JoinColumnsSelector
import org.jetbrains.kotlinx.dataframe.api.JoinDsl
import org.jetbrains.kotlinx.dataframe.api.JoinType
import org.jetbrains.kotlinx.dataframe.api.getColumnsWithPaths
import org.jetbrains.kotlinx.dataframe.api.isColumnGroup
import org.jetbrains.kotlinx.dataframe.api.remove
import org.jetbrains.kotlinx.dataframe.api.toDataFrameFromPairs
import org.jetbrains.kotlinx.dataframe.columns.ColumnSet
import org.jetbrains.kotlinx.dataframe.columns.toColumnSet
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.Arguments
import org.jetbrains.kotlinx.dataframe.plugin.impl.ConeTypesAdapter
import org.jetbrains.kotlinx.dataframe.plugin.impl.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.Present
import org.jetbrains.kotlinx.dataframe.plugin.impl.asDataColumn
import org.jetbrains.kotlinx.dataframe.plugin.impl.asDataFrame
import org.jetbrains.kotlinx.dataframe.plugin.impl.asSimpleColumn
import org.jetbrains.kotlinx.dataframe.plugin.impl.dataFrame
import org.jetbrains.kotlinx.dataframe.plugin.impl.enum
import org.jetbrains.kotlinx.dataframe.plugin.impl.makeNullable
import org.jetbrains.kotlinx.dataframe.plugin.impl.toPluginDataFrameSchema

internal abstract class AbstractJoin() : AbstractInterpreter<PluginDataFrameSchema>() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.other: PluginDataFrameSchema by dataFrame()
    val Arguments.selector: ColumnSet<*>? by arg()

    fun Arguments.join(type: JoinType): PluginDataFrameSchema {
        val leftDf = receiver.asDataFrame()
        val rightDf = other.asDataFrame()
        val joinSelector: JoinColumnsSelector<*, *> = if (selector != null) {
            { it: ColumnsContainer<*> -> selector!! }
        } else {
            JoinDsl.defaultJoinColumns(leftDf, rightDf)
        }
        val joinColumns = JoinDsl.getColumns(leftDf, rightDf, joinSelector)

        val filteredRightDf = rightDf.remove { joinColumns.map { it.right }.toColumnSet() }
        val left = leftDf.getColumnsWithPaths { colsAtAnyDepth().filter { !it.isColumnGroup() } }
            .map { it.path to it.data }
        val right = filteredRightDf.getColumnsWithPaths { colsAtAnyDepth().filter { !it.isColumnGroup() } }
            .map { it.path to it.data }

        val result = buildList {
            when (type) {
                JoinType.Inner -> {
                    addAll(left)
                    addAll(right)
                }
                JoinType.Left -> {
                    addAll(left)
                    addAll(right.map { it.first to makeNullable(it.second.asSimpleColumn()).asDataColumn() })
                }
                JoinType.Right -> {
                    addAll(left.map { it.first to makeNullable(it.second.asSimpleColumn()).asDataColumn() })
                    addAll(right)
                }
                JoinType.Full -> {
                    addAll(left.map { it.first to makeNullable(it.second.asSimpleColumn()).asDataColumn() })
                    addAll(right.map { it.first to makeNullable(it.second.asSimpleColumn()).asDataColumn() })
                }
                JoinType.Filter -> addAll(left)
                JoinType.Exclude -> addAll(left)
            }
        }
        return result.toDataFrameFromPairs<ConeTypesAdapter>().toPluginDataFrameSchema()
    }
}

internal class Join0 : AbstractJoin() {
    val Arguments.type: JoinType by enum(defaultValue = Present(JoinType.Inner))

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return join(type)
    }
}

internal class LeftJoin : AbstractJoin() {
    override fun Arguments.interpret(): PluginDataFrameSchema {
        return join(JoinType.Left)
    }
}

internal class RightJoin : AbstractJoin() {
    override fun Arguments.interpret(): PluginDataFrameSchema {
        return join(JoinType.Right)
    }
}

internal class FullJoin : AbstractJoin() {
    override fun Arguments.interpret(): PluginDataFrameSchema {
        return join(JoinType.Full)
    }
}

internal class InnerJoin : AbstractJoin() {
    override fun Arguments.interpret(): PluginDataFrameSchema {
        return join(JoinType.Inner)
    }
}

internal class FilterJoin : AbstractJoin() {
    override fun Arguments.interpret(): PluginDataFrameSchema {
        return join(JoinType.Filter)
    }
}

internal class ExcludeJoin : AbstractJoin() {
    override fun Arguments.interpret(): PluginDataFrameSchema {
        return join(JoinType.Exclude)
    }
}

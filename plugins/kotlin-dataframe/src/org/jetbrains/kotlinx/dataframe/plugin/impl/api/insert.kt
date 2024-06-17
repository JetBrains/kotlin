@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.Arguments
import org.jetbrains.kotlinx.dataframe.plugin.impl.Present
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.Infer
import org.jetbrains.kotlinx.dataframe.api.pathOf
import org.jetbrains.kotlinx.dataframe.api.toPath
import org.jetbrains.kotlinx.dataframe.impl.api.GenericColumnsToInsert
import org.jetbrains.kotlinx.dataframe.impl.api.GenericColumnGroup
import org.jetbrains.kotlinx.dataframe.impl.api.insertImplGenericContainer
import org.jetbrains.kotlinx.dataframe.plugin.impl.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleCol
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleDataColumn
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleColumnGroup
import org.jetbrains.kotlinx.dataframe.plugin.impl.data.ColumnAccessorApproximation
import org.jetbrains.kotlinx.dataframe.plugin.impl.data.ColumnPathApproximation
import org.jetbrains.kotlinx.dataframe.plugin.impl.data.ColumnWithPathApproximation
import org.jetbrains.kotlinx.dataframe.plugin.impl.data.InsertClauseApproximation
import org.jetbrains.kotlinx.dataframe.plugin.impl.data.KPropertyApproximation
import org.jetbrains.kotlinx.dataframe.plugin.impl.columnAccessor
import org.jetbrains.kotlinx.dataframe.plugin.impl.columnPath
import org.jetbrains.kotlinx.dataframe.plugin.impl.columnWithPath
import org.jetbrains.kotlinx.dataframe.plugin.impl.dataColumn
import org.jetbrains.kotlinx.dataframe.plugin.impl.dataFrame
import org.jetbrains.kotlinx.dataframe.plugin.impl.enum
import org.jetbrains.kotlinx.dataframe.plugin.impl.insertClause
import org.jetbrains.kotlinx.dataframe.plugin.impl.kproperty
import org.jetbrains.kotlinx.dataframe.plugin.impl.string
import org.jetbrains.kotlinx.dataframe.plugin.impl.type

/**
 * @see DataFrame.insert
 */
internal class Insert0 : AbstractInterpreter<InsertClauseApproximation>() {
    val Arguments.column: SimpleCol by dataColumn()
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()

    override fun Arguments.interpret(): InsertClauseApproximation {
        return InsertClauseApproximation(receiver, column)
    }
}

internal class Insert1 : AbstractInterpreter<InsertClauseApproximation>() {
    val Arguments.name: String by string()
    val Arguments.infer: Infer by enum(defaultValue = Present(Infer.Nulls))
    val Arguments.expression: TypeApproximation by type()
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()

    override fun Arguments.interpret(): InsertClauseApproximation {
        return InsertClauseApproximation(receiver, SimpleDataColumn(name, expression))
    }
}

internal class Insert2 : AbstractInterpreter<InsertClauseApproximation>() {
    val Arguments.column: ColumnAccessorApproximation by columnAccessor()
    val Arguments.infer: Infer by enum(defaultValue = Present(Infer.Nulls))
    val Arguments.expression: TypeApproximation by type()
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()

    override fun Arguments.interpret(): InsertClauseApproximation {
        return InsertClauseApproximation(receiver, SimpleDataColumn(column.name, expression))
    }
}

internal class Insert3 : AbstractInterpreter<InsertClauseApproximation>() {
    val Arguments.column: KPropertyApproximation by kproperty()
    val Arguments.infer: Infer by enum(defaultValue = Present(Infer.Nulls))
    val Arguments.expression: TypeApproximation by type()
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()

    override fun Arguments.interpret(): InsertClauseApproximation {
        return InsertClauseApproximation(receiver, SimpleDataColumn(column.name, expression))
    }
}

internal class Under0 : AbstractInterpreter<PluginDataFrameSchema>() {
    val Arguments.column: ColumnWithPathApproximation by columnWithPath()
    val Arguments.receiver: InsertClauseApproximation by insertClause()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return receiver.df.insertImpl(listOf(GenericColumnsToInsert(column.path.path.toPath(), receiver.column)))
    }
}

internal class Under1 : AbstractInterpreter<PluginDataFrameSchema>() {
    val Arguments.columnPath: ColumnPathApproximation by columnPath()
    val Arguments.receiver: InsertClauseApproximation by insertClause()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return receiver.df.insertImpl(listOf(GenericColumnsToInsert(columnPath.path.toPath(), receiver.column)))
    }
}

internal class Under2 : AbstractInterpreter<PluginDataFrameSchema>() {
    val Arguments.column: ColumnAccessorApproximation by columnAccessor()
    val Arguments.receiver: InsertClauseApproximation by insertClause()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return receiver.df.insertImpl(listOf(GenericColumnsToInsert(pathOf(column.name), receiver.column)))
    }
}

internal class Under3 : AbstractInterpreter<PluginDataFrameSchema>() {
    val Arguments.column: KPropertyApproximation by kproperty()
    val Arguments.receiver: InsertClauseApproximation by insertClause()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return receiver.df.insertImpl(listOf(GenericColumnsToInsert(pathOf(column.name), receiver.column)))
    }
}

internal class Under4 : AbstractInterpreter<PluginDataFrameSchema>() {
    val Arguments.column: String by string()
    val Arguments.receiver: InsertClauseApproximation by insertClause()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return receiver.df.insertImpl(listOf(GenericColumnsToInsert(pathOf(column), receiver.column)))
    }
}

@PublishedApi
internal fun PluginDataFrameSchema.insertImpl(
    columns: List<GenericColumnsToInsert<SimpleCol>>,
    columnGroupType: TypeApproximation
): PluginDataFrameSchema {
    return insertImplGenericContainer<PluginDataFrameSchema, SimpleCol, GenericColumnGroup<SimpleCol>>(
        this,
        columns,
        columns.firstOrNull()?.referenceNode?.getRoot(),
        0,
        factory = { PluginDataFrameSchema(it) },
        empty = PluginDataFrameSchema(emptyList()),
        rename = { rename(it) },
        createColumnGroup = { name, columns ->
            SimpleColumnGroup(name, columns)
        }
    )
}

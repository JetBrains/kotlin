package org.jetbrains.kotlinx.dataframe.plugin

import org.jetbrains.kotlinx.dataframe.annotations.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.annotations.AbstractSchemaModificationInterpreter
import org.jetbrains.kotlinx.dataframe.annotations.Arguments
import org.jetbrains.kotlinx.dataframe.annotations.ConvertApproximation
import org.jetbrains.kotlinx.dataframe.annotations.FrameColumnTypeApproximation
import org.jetbrains.kotlinx.dataframe.annotations.Present
import org.jetbrains.kotlinx.dataframe.annotations.TypeApproximation
import org.jetbrains.kotlinx.dataframe.api.Infer

internal class Convert0 : AbstractInterpreter<ConvertApproximation>() {
    val Arguments.columns: List<ColumnWithPathApproximation> by arg()
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    override val Arguments.startingSchema get() = receiver

    override fun Arguments.interpret(): ConvertApproximation {
        return ConvertApproximation(receiver, columns.map { it.path.path })
    }
}

public class Convert2 : AbstractInterpreter<ConvertApproximation>() {
    public val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    public val Arguments.columns: List<String> by varargString()

    override fun Arguments.interpret(): ConvertApproximation {
        return ConvertApproximation(receiver, columns.map { listOf(it) })
    }
}

internal class Convert6 : AbstractInterpreter<PluginDataFrameSchema>() {
    val Arguments.firstCol: String by string()
    val Arguments.cols: List<String> by varargString(defaultValue = Present(emptyList()))
    val Arguments.infer: Infer by enum(defaultValue = Present(Infer.Nulls))
    val Arguments.expression: TypeApproximation by type()
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    override val Arguments.startingSchema get() = receiver

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val columns = (listOf(firstCol) + cols).map { listOf(it) }
        return convertImpl(receiver, columns, expression)
    }
}

public class With0 : AbstractSchemaModificationInterpreter() {
    public val Arguments.receiver: ConvertApproximation by arg()
    public val Arguments.type: TypeApproximation by type(name("rowConverter"))

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return convertImpl(receiver.schema, receiver.columns, type)
    }
}

internal fun convertImpl(
    pluginDataFrameSchema: PluginDataFrameSchema,
    columns: List<List<String>>,
    type: TypeApproximation
): PluginDataFrameSchema {
    return pluginDataFrameSchema.map(columns.toSet()) { path, column ->
        require(column.kind() == SimpleColumnKind.VALUE) {
            "$path must be ${SimpleColumnKind.VALUE}, but was ${column.kind()}"
        }
        column.changeType(type)
    }
}

internal fun PluginDataFrameSchema.map(selected: ColumnsSet, transform: ColumnMapper): PluginDataFrameSchema {
    return PluginDataFrameSchema(
        f(columns(), transform, selected, emptyList())
    )
}

internal typealias ColumnsSet = Set<List<String>>

internal typealias ColumnMapper = (List<String>, SimpleCol) -> SimpleCol

internal fun f(columns: List<SimpleCol>, transform: ColumnMapper, selected: ColumnsSet, path: List<String>): List<SimpleCol> {
    return columns.map {
        val fullPath = path + listOf(it.name)
        when (it) {
            is SimpleColumnGroup -> if (fullPath in selected) {
                transform(fullPath, it)
            } else {
                it.map(transform, selected, fullPath)
            }
            is SimpleFrameColumn -> if (fullPath in selected) {
                transform(fullPath, it)
            } else {
                it.map(transform, selected, fullPath)
            }
            else -> if (fullPath in selected) {
                transform(path, it)
            } else {
                it
            }
        }
    }
}

internal fun SimpleColumnGroup.map(transform: ColumnMapper, selected: ColumnsSet, path: List<String>): SimpleColumnGroup {
    return SimpleColumnGroup(
        name,
        f(columns(), transform, selected, path),
        type
    )
}

internal fun SimpleFrameColumn.map(transform: ColumnMapper, selected: ColumnsSet, path: List<String>): SimpleFrameColumn {
    return SimpleFrameColumn(
        name,
        f(columns(), transform, selected, path),
        nullable,
        anyFrameType
    )
}

internal class To0 : AbstractInterpreter<PluginDataFrameSchema>() {
    val Arguments.receiver: ConvertApproximation by arg()
    val Arguments.typeArg0: TypeApproximation by arg()
    override val Arguments.startingSchema get() = receiver.schema

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return convertImpl(receiver.schema, receiver.columns, typeArg0)
    }
}

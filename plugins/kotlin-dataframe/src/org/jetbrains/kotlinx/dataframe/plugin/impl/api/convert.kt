package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.plugin.pluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.utils.Names
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.isNullable
import org.jetbrains.kotlinx.dataframe.plugin.extensions.KotlinTypeFacade
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractSchemaModificationInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.Arguments
import org.jetbrains.kotlinx.dataframe.plugin.impl.Present
import org.jetbrains.kotlinx.dataframe.api.Infer
import org.jetbrains.kotlinx.dataframe.plugin.impl.data.ColumnWithPathApproximation
import org.jetbrains.kotlinx.dataframe.plugin.impl.dataFrame
import org.jetbrains.kotlinx.dataframe.plugin.impl.enum
import org.jetbrains.kotlinx.dataframe.plugin.impl.string
import org.jetbrains.kotlinx.dataframe.plugin.impl.type
import org.jetbrains.kotlinx.dataframe.plugin.impl.varargString

internal class Convert0 : AbstractInterpreter<ConvertApproximation>() {
    val Arguments.columns: List<ColumnWithPathApproximation> by arg()
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    override val Arguments.startingSchema get() = receiver

    override fun Arguments.interpret(): ConvertApproximation {
        return ConvertApproximation(receiver, columns.map { it.path.path })
    }
}

class Convert2 : AbstractInterpreter<ConvertApproximation>() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.columns: List<String> by varargString()

    override fun Arguments.interpret(): ConvertApproximation {
        return ConvertApproximation(receiver, columns.map { listOf(it) })
    }
}

class ConvertApproximation(val schema: PluginDataFrameSchema, val columns: List<List<String>>)

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

class With0 : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: ConvertApproximation by arg()
    val Arguments.type: TypeApproximation by type(name("rowConverter"))

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return convertImpl(receiver.schema, receiver.columns, type)
    }
}

internal fun KotlinTypeFacade.convertImpl(
    pluginDataFrameSchema: PluginDataFrameSchema,
    columns: List<List<String>>,
    type: TypeApproximation
): PluginDataFrameSchema {
    return pluginDataFrameSchema.map(columns.toSet()) { path, column ->
        require(column.kind() == SimpleColumnKind.VALUE) {
            "$path must be ${SimpleColumnKind.VALUE}, but was ${column.kind()}"
        }
        val unwrappedType = type.type
        // TODO: AnyRow
        if (unwrappedType is ConeClassLikeType && unwrappedType.classId == Names.DF_CLASS_ID && !unwrappedType.isNullable) {
            val f = unwrappedType.typeArguments.single()
            SimpleFrameColumn(column.name, pluginDataFrameSchema(f).columns(), anyDataFrame)
        } else {
            column.changeType(type)
        }
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

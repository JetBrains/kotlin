package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.getBooleanArgument
import org.jetbrains.kotlin.fir.declarations.getKClassArgument
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.references.toResolvedFunctionSymbol
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.fir.types.withNullability
import org.jetbrains.kotlin.fir.types.withNullabilityOf
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.mapToSetOrEmpty
import org.jetbrains.kotlinx.dataframe.api.asColumn
import org.jetbrains.kotlinx.dataframe.api.convert
import org.jetbrains.kotlinx.dataframe.api.pathOf
import org.jetbrains.kotlinx.dataframe.columns.toColumnSet
import org.jetbrains.kotlinx.dataframe.plugin.extensions.wrap
import org.jetbrains.kotlinx.dataframe.plugin.impl.*
import org.jetbrains.kotlinx.dataframe.plugin.impl.convert
import org.jetbrains.kotlinx.dataframe.plugin.utils.Names

internal class Convert0 : AbstractInterpreter<ConvertApproximation>() {
    val Arguments.columns: ColumnsResolver by arg()
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    override val Arguments.startingSchema get() = receiver

    override fun Arguments.interpret(): ConvertApproximation {
        return ConvertApproximation(receiver, columns)
    }
}

class Convert2 : AbstractInterpreter<ConvertApproximation>() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.columns: List<String> by arg(defaultValue = Present(emptyList()))

    override fun Arguments.interpret(): ConvertApproximation {
        return ConvertApproximation(
            receiver.createImpliedColumns(columns),
            columnsResolver { columns.map { pathOf(it) }.toColumnSet() }
        )
    }
}

class ConvertApproximation(val schema: PluginDataFrameSchema, val columns: ColumnsResolver)

internal class Convert6 : AbstractInterpreter<PluginDataFrameSchema>() {
    val Arguments.firstCol: String by arg()
    val Arguments.cols: List<String> by arg(defaultValue = Present(emptyList()))
    val Arguments.infer by ignore()
    val Arguments.expression: TypeApproximation by type()
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    override val Arguments.startingSchema get() = receiver

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val columns = (listOf(firstCol) + cols)
        val topLevelNames = receiver.columns().mapToSetOrEmpty { it.name }
        val assumedColumns = columns
            .filter { it !in topLevelNames }
            .map { simpleColumnOf(it, expression.type) }
        val df = PluginDataFrameSchema(receiver.columns() + assumedColumns)
        return df.convert(columnsResolver { columns.map { pathOf(it) }.toColumnSet() }) {
            expression
        }
    }
}

class With0 : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: ConvertApproximation by arg()
    val Arguments.infer by ignore()
    val Arguments.type: TypeApproximation by type(name("rowConverter"))

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return receiver.schema.convert(receiver.columns) {
            type
        }
    }
}

class ConvertNotNull : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: ConvertApproximation by arg()
    val Arguments.type: TypeApproximation by type(name("expression"))

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return receiver.schema.convertAsColumn(receiver.columns) {
            if (it is SimpleDataColumn) {
                simpleColumnOf(it.name, type.type.withNullabilityOf(it.type.type, session.typeContext))
            } else {
                simpleColumnOf(it.name, type.type)
            }
        }
    }
}

class PerRowCol : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: ConvertApproximation by arg()
    val Arguments.infer by ignore()
    val Arguments.type: TypeApproximation by type(name("expression"))

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return receiver.schema.convert(receiver.columns) {
            type
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
            is SimpleDataColumn -> if (fullPath in selected) {
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
        f(columns(), transform, selected, path)
    )
}

internal fun SimpleFrameColumn.map(transform: ColumnMapper, selected: ColumnsSet, path: List<String>): SimpleFrameColumn {
    return SimpleFrameColumn(
        name,
        f(columns(), transform, selected, path)
    )
}

internal class To0 : AbstractInterpreter<PluginDataFrameSchema>() {
    val Arguments.receiver: ConvertApproximation by arg()
    val Arguments.typeArg0: TypeApproximation by arg()
    override val Arguments.startingSchema get() = receiver.schema

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return receiver.schema.convert(receiver.columns) {
            typeArg0
        }
    }
}

internal class ConvertAsColumn : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: ConvertApproximation by arg()
    val Arguments.typeArg2: TypeApproximation by arg()
    val Arguments.type: TypeApproximation by type(name("columnConverter"))

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return receiver.schema.asDataFrame()
            .convert { receiver.columns }
            .asColumn { simpleColumnOf("", typeArg2.type).asDataColumn() }
            .toPluginDataFrameSchema()
    }
}

internal abstract class AbstractToSpecificType : AbstractInterpreter<PluginDataFrameSchema>() {
    val Arguments.functionCall: FirFunctionCall by arg(lens = Interpreter.Id)
    val Arguments.receiver: ConvertApproximation by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val converterAnnotation =
            functionCall.calleeReference.toResolvedFunctionSymbol()?.getAnnotationByClassId(Names.CONVERTER_ANNOTATION, session)
        val to = converterAnnotation?.getKClassArgument(Name.identifier("klass"), session)
        val nullable = converterAnnotation?.getBooleanArgument(Name.identifier("nullable"), session)
        return if (to != null && nullable != null) {
            val targetType = to.withNullability(nullable, session.typeContext)
            receiver.schema.convert(receiver.columns) {
                targetType.wrap()
            }
        } else {
            PluginDataFrameSchema.EMPTY
        }
    }
}

internal class ToSpecificType : AbstractToSpecificType()

internal class ToSpecificTypeZone : AbstractToSpecificType() {
    val Arguments.zone by ignore()
}

internal class ToSpecificTypePattern : AbstractToSpecificType() {
    val Arguments.pattern by ignore()
    val Arguments.locale by ignore()
}

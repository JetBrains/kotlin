package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.columns.ColumnPath
import org.jetbrains.kotlinx.dataframe.columns.toColumnSet
import org.jetbrains.kotlinx.dataframe.impl.ColumnNameGenerator
import org.jetbrains.kotlinx.dataframe.plugin.extensions.KotlinTypeFacade
import org.jetbrains.kotlinx.dataframe.plugin.impl.*
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.mergeRows

data class MergeApproximation(
    val df: PluginDataFrameSchema,
    val columns: ColumnsResolver,
    val transform: Boolean = false
)

class Merge0 : AbstractInterpreter<MergeApproximation>() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.selector: ColumnsResolver by arg()

    override fun Arguments.interpret(): MergeApproximation {
        return MergeApproximation(receiver, selector)
    }
}

class MergeInto0 : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: MergeApproximation by arg()
    val Arguments.columnName: String by arg()
    val Arguments.typeArg2 by type()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return merge(receiver.df, receiver.columns, pathOf(columnName), typeArg2.coneType, receiver.transform)
    }
}

class MergeId : AbstractInterpreter<MergeApproximation>() {
    val Arguments.receiver: MergeApproximation by arg()

    override fun Arguments.interpret(): MergeApproximation {
        return receiver
    }
}

class MergeBy0 : AbstractInterpreter<MergeApproximation>() {
    val Arguments.receiver: MergeApproximation by arg()
    val Arguments.separator by ignore()
    val Arguments.prefix by ignore()
    val Arguments.postfix by ignore()
    val Arguments.limit by ignore()
    val Arguments.truncated by ignore()

    override fun Arguments.interpret(): MergeApproximation {
        return receiver.copy(transform = true)
    }
}

class MergeBy1 : AbstractInterpreter<MergeApproximation>() {
    val Arguments.receiver: MergeApproximation by arg()
    val Arguments.infer by ignore()
    val Arguments.transform by ignore()

    override fun Arguments.interpret(): MergeApproximation {
        return receiver.copy(transform = true)
    }
}

context(_: KotlinTypeFacade)
fun merge(
    schema: PluginDataFrameSchema,
    columns: ColumnsResolver,
    path: ColumnPath,
    result: ConeKotlinType,
    transform: Boolean
): PluginDataFrameSchema {
    val df = schema.asDataFrame(impliedColumnsResolver = columns)
    val mergedPath = if (df.getColumnOrNull(path) != null) {
        val temp = ColumnNameGenerator(df.columnNames()).addUnique("temp")
        pathOf(temp)
    } else {
        path
    }

    val merged = if (transform) {
        simpleColumnOf(mergedPath.columnName, result)
    } else {
        val colsToMerge = columns.resolve(schema).map { it.column }
        val columnGroups = colsToMerge.filterIsInstance<SimpleColumnGroup>()
        if (colsToMerge.size == columnGroups.size) {
            val mergedGroup = columnGroups.reduce { schema, otherSchema ->
                SimpleColumnGroup(mergedPath.columnName, mergeRows(schema, otherSchema))
            }
            SimpleFrameColumn(mergedGroup.name, mergedGroup.columns())
        } else {
            simpleColumnOf(mergedPath.columnName, result)
        }
    }

    val grouped = df.move { columns }.under { mergedPath }

    var res = grouped.replace { mergedPath }.with { merged.asDataColumn() }
    if (mergedPath != path) {
        res = res.remove { path }.move { mergedPath }.into { path }
    }
    return res.toPluginDataFrameSchema()
}

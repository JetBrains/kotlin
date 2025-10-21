package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.columns.ColumnPath
import org.jetbrains.kotlinx.dataframe.columns.toColumnSet
import org.jetbrains.kotlinx.dataframe.impl.ColumnNameGenerator
import org.jetbrains.kotlinx.dataframe.plugin.impl.*

class MergeApproximation(
    val df: PluginDataFrameSchema,
    val columns: ColumnsResolver,
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
        val columns = receiver.columns.resolve(receiver.df).map { it.path.toPath() }
        return merge(receiver.df, columns, pathOf(columnName), simpleColumnOf(columnName, typeArg2.coneType))
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
        return receiver
    }
}

class MergeBy1 : AbstractInterpreter<MergeApproximation>() {
    val Arguments.receiver: MergeApproximation by arg()
    val Arguments.infer by ignore()
    val Arguments.transform by ignore()

    override fun Arguments.interpret(): MergeApproximation {
        return receiver
    }
}

fun merge(
    schema: PluginDataFrameSchema,
    columns: List<ColumnPath>,
    path: ColumnPath,
    result: SimpleCol,
): PluginDataFrameSchema {
    val df = schema.asDataFrame()
    val mergedPath = if (df.getColumnOrNull(path) != null) {
        val temp = ColumnNameGenerator(df.columnNames()).addUnique("temp")
        pathOf(temp)
    } else {
        path
    }

    val grouped = df.move { columns.toColumnSet() }.under { mergedPath }

    var res = grouped.replace { mergedPath }.with { result.rename(mergedPath.columnName).asDataColumn() }
    if (mergedPath != path) {
        res = res.remove { path }.move { mergedPath }.into { path }
    }
    return res.toPluginDataFrameSchema()
}

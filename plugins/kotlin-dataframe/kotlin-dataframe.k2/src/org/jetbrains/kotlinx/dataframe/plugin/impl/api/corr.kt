package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlinx.dataframe.api.pathOf
import org.jetbrains.kotlinx.dataframe.columns.ColumnPath
import org.jetbrains.kotlinx.dataframe.plugin.extensions.KotlinTypeFacade
import org.jetbrains.kotlinx.dataframe.plugin.extensions.wrap
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractSchemaModificationInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.Arguments
import org.jetbrains.kotlinx.dataframe.plugin.impl.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleCol
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleColumnGroup
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleDataColumn
import org.jetbrains.kotlinx.dataframe.plugin.impl.data.ColumnWithPathApproximation
import org.jetbrains.kotlinx.dataframe.plugin.impl.dataFrame

data class CorrApproximation(
    val df: PluginDataFrameSchema,
    val columns: ColumnsResolver,
)

class Corr0 : AbstractInterpreter<CorrApproximation>() {
    val Arguments.receiver by dataFrame()
    val Arguments.columns: ColumnsResolver by arg()

    override fun Arguments.interpret(): CorrApproximation {
        return CorrApproximation(receiver, columns)
    }
}

class CorrWith : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: CorrApproximation by arg()
    val Arguments.otherColumns: ColumnsResolver by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return corrImpl(receiver.df, receiver.columns, otherColumns)
    }
}

class CorrWithItself : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: CorrApproximation by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return corrImpl(receiver.df, receiver.columns, receiver.columns)
    }
}

class DataFrameCorr : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver by dataFrame()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val columns = receiver.columns().flatMap {
            it.collectSuitableColumns(pathOf(it.name))
        }
        return correlationMatrix(columns, columns)
    }
}

context(facade: KotlinTypeFacade)
internal fun corrImpl(
    df: PluginDataFrameSchema,
    columns: ColumnsResolver,
    otherColumns: ColumnsResolver,
): PluginDataFrameSchema {
    val cols1 = columns.resolve(df)
        .filter { it.column is SimpleColumnGroup || it.column.isSuitableForCorr() }
    val cols2 = otherColumns.resolve(df)
        .filter { it.column is SimpleColumnGroup || it.column.isSuitableForCorr() }
        .unpackColumnGroups()

    val indexColumnName = if (cols1.size == 1 && cols1[0].column is SimpleColumnGroup) cols1[0].column.name() else "column"
    return correlationMatrix(cols1.unpackColumnGroups(), cols2, indexColumnName)
}

context(facade: KotlinTypeFacade)
private fun correlationMatrix(
    cols1: List<ColumnWithPathApproximation>,
    cols2: List<ColumnWithPathApproximation>,
    indexColumnName: String = "column",
): PluginDataFrameSchema {
    val doubleType = facade.session.builtinTypes.doubleType.coneType.wrap()
    val index = SimpleDataColumn(indexColumnName, facade.session.builtinTypes.stringType.coneType.wrap())
    val newColumns = cols2.map {
        SimpleDataColumn(it.path.last(), doubleType)
    }
    return PluginDataFrameSchema(listOf(index) + newColumns)
}

context(facade: KotlinTypeFacade)
private fun List<ColumnWithPathApproximation>.unpackColumnGroups(): List<ColumnWithPathApproximation> {
    return flatMap { col ->
        if (col.column is SimpleColumnGroup) {
            col.column.columns().flatMap {
                it.collectSuitableColumns(ColumnPath(col.path + it.name))
            }
        } else {
            listOf(col)
        }
    }
}

context(facade: KotlinTypeFacade)
private fun SimpleCol.collectSuitableColumns(path: ColumnPath): List<ColumnWithPathApproximation> {
    return when (this) {
        is SimpleColumnGroup -> columns().flatMap { it.collectSuitableColumns(ColumnPath(path + it.name)) }

        else -> if (isSuitableForCorr()) listOf(ColumnWithPathApproximation(path, this)) else emptyList()
    }
}

context(facade: KotlinTypeFacade)
private fun SimpleCol.isSuitableForCorr(): Boolean {
    if (this !is SimpleDataColumn) return false
    val session = facade.session
    return type.coneType.isSubtypeOf(session.builtinTypes.numberType.coneType, session) ||
            type.coneType.equalTypes(session.builtinTypes.booleanType.coneType, session)
}

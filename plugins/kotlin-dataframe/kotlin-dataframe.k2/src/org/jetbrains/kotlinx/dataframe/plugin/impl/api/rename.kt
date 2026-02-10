package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlinx.dataframe.api.pathOf
import org.jetbrains.kotlinx.dataframe.api.rename
import org.jetbrains.kotlinx.dataframe.api.renameToCamelCase
import org.jetbrains.kotlinx.dataframe.api.toCamelCase
import org.jetbrains.kotlinx.dataframe.columns.toColumnSet
import org.jetbrains.kotlinx.dataframe.plugin.impl.*
import org.jetbrains.kotlinx.dataframe.plugin.impl.data.ColumnWithPathApproximation

class Rename : AbstractInterpreter<RenameClauseApproximation>() {
    private val Arguments.receiver by dataFrame()
    private val Arguments.columns: ColumnsResolver by arg()

    override fun Arguments.interpret(): RenameClauseApproximation = RenameClauseApproximation(receiver, columns)
}

class RenameClauseApproximation(val schema: PluginDataFrameSchema, val columns: ColumnsResolver)

class RenameInto : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: RenameClauseApproximation by arg()
    val Arguments.newNames: List<String> by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val columns = receiver.columns.resolve(receiver.schema)
        require(columns.size == newNames.size)
        var i = 0
        return receiver.schema.map(
            selected = receiver.columns,
            nextName = { newNames[i].also { i += 1 } },
        )
    }
}

class RenameIntoLambda : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: RenameClauseApproximation by arg()
    val Arguments.transform: String by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val columns = receiver.columns.resolve(receiver.schema)
        require(columns.size == 1)
        return receiver.schema.map(
            selected = receiver.columns,
            nextName = { transform },
        )
    }
}

class RenameMapping : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver by dataFrame()
    val Arguments.mappings: List<Interpreter.Success<Pair<*, *>>> by arg()
    override fun Arguments.interpret(): PluginDataFrameSchema {
        val mappings = mappings.map {
            val it = it.value
            val name = (it.first as? FirLiteralExpression)?.value as? String
            val newName = (it.second as? FirLiteralExpression)?.value as? String
            if (name == null || newName == null) return PluginDataFrameSchema(emptyList())
            name to newName
        }

        val expectedColumns = object : ColumnsResolver {
            override fun resolve(df: PluginDataFrameSchema): List<ColumnWithPathApproximation> {
                return mappings.flatMap {
                    stringApiColumnResolver(
                        pathOf(it.first),
                        session.builtinTypes.nullableAnyType.coneType
                    ).resolve(df)
                }
            }
        }
        return receiver.asDataFrame(impliedColumnsResolver = expectedColumns).rename(*mappings.toTypedArray()).toPluginDataFrameSchema()
    }
}

internal fun PluginDataFrameSchema.map(selected: ColumnsResolver, nextName: () -> String): PluginDataFrameSchema =
    PluginDataFrameSchema(
        f(columns(impliedColumnsResolver = selected), nextName, selected.resolve(this).mapTo(mutableSetOf()) { it.path }, emptyList()),
    )

internal fun f(
    columns: List<SimpleCol>,
    nextName: () -> String,
    selected: ColumnsSet,
    path: List<String>,
): List<SimpleCol> =
    columns.map {
        val fullPath = path + listOf(it.name)
        when (it) {
            is SimpleColumnGroup -> {
                val group = if (fullPath in selected) {
                    it.rename(nextName())
                } else {
                    it
                }
                group.map(selected, fullPath, nextName)
            }

            is SimpleFrameColumn -> {
                val frame = if (fullPath in selected) {
                    it.rename(nextName())
                } else {
                    it
                }
                frame.map(selected, fullPath, nextName)
            }

            is SimpleDataColumn -> if (fullPath in selected) {
                it.rename(nextName())
            } else {
                it
            }
        }
    }

internal fun SimpleColumnGroup.map(
    selected: ColumnsSet,
    path: List<String>,
    nextName: () -> String,
): SimpleColumnGroup =
    SimpleColumnGroup(
        name = name,
        columns = f(columns(), nextName, selected, path),
    )

internal fun SimpleFrameColumn.map(
    selected: ColumnsSet,
    path: List<String>,
    nextName: () -> String,
): SimpleFrameColumn =
    SimpleFrameColumn(
        name = name,
        columns = f(columns(), nextName, selected, path),
    )

class RenameToCamelCase : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver by dataFrame()

    override fun Arguments.interpret(): PluginDataFrameSchema =
        receiver.asDataFrame(/*operation has no selector*/)
            .renameToCamelCase()
            .toPluginDataFrameSchema()
}

class RenameToCamelCaseClause : AbstractSchemaModificationInterpreter() {
    val Arguments.receiver: RenameClauseApproximation by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val selectedPaths = receiver.columns.resolve(receiver.schema).map { it.path }
        return receiver.schema.asDataFrame(impliedColumnsResolver = receiver.columns)
            .rename { selectedPaths.toColumnSet() }.toCamelCase()
            .toPluginDataFrameSchema()
    }
}

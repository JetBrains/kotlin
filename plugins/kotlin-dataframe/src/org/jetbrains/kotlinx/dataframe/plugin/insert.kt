package org.jetbrains.kotlinx.dataframe.plugin

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.Infer
import org.jetbrains.kotlinx.dataframe.api.pathOf
import org.jetbrains.kotlinx.dataframe.api.toPath
import org.jetbrains.kotlinx.dataframe.columns.ColumnPath
import org.jetbrains.kotlinx.dataframe.impl.api.Col
import org.jetbrains.kotlinx.dataframe.impl.api.ColumnToInsert1
import org.jetbrains.kotlinx.dataframe.impl.api.DataFrameLikeContainer
import org.jetbrains.kotlinx.dataframe.impl.api.MyColumnGroup
import org.jetbrains.kotlinx.dataframe.impl.api.insertImplGenericContainer

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
        return InsertClauseApproximation(receiver, SimpleCol(name, expression))
    }
}

internal class Insert2 : AbstractInterpreter<InsertClauseApproximation>() {
    val Arguments.column: ColumnAccessorApproximation by columnAccessor()
    val Arguments.infer: Infer by enum(defaultValue = Present(Infer.Nulls))
    val Arguments.expression: TypeApproximation by type()
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()

    override fun Arguments.interpret(): InsertClauseApproximation {
        return InsertClauseApproximation(receiver, SimpleCol(column.name, expression))
    }
}

internal class Insert3 : AbstractInterpreter<InsertClauseApproximation>() {
    val Arguments.column: KPropertyApproximation by kproperty()
    val Arguments.infer: Infer by enum(defaultValue = Present(Infer.Nulls))
    val Arguments.expression: TypeApproximation by type()
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()

    override fun Arguments.interpret(): InsertClauseApproximation {
        return InsertClauseApproximation(receiver, SimpleCol(column.name, expression))
    }
}

internal class Under0 : AbstractInterpreter<PluginDataFrameSchema>() {
    val Arguments.column: ColumnWithPathApproximation by columnWithPath()
    val Arguments.receiver: InsertClauseApproximation by insertClause()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return receiver.df.insertImpl(column.path.path.toPath(), receiver.column, anyDataFrame)
    }
}

internal class Under1 : AbstractInterpreter<PluginDataFrameSchema>() {
    val Arguments.columnPath: ColumnPathApproximation by columnPath()
    val Arguments.receiver: InsertClauseApproximation by insertClause()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return receiver.df.insertImpl(columnPath.path.toPath(), receiver.column, anyRow)
    }
}

internal class Under2 : AbstractInterpreter<PluginDataFrameSchema>() {
    val Arguments.column: ColumnAccessorApproximation by columnAccessor()
    val Arguments.receiver: InsertClauseApproximation by insertClause()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return receiver.df.insertImpl(pathOf(column.name), receiver.column, anyRow)
    }
}

internal class Under3 : AbstractInterpreter<PluginDataFrameSchema>() {
    val Arguments.column: KPropertyApproximation by kproperty()
    val Arguments.receiver: InsertClauseApproximation by insertClause()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return receiver.df.insertImpl(pathOf(column.name), receiver.column, anyRow)
    }
}

internal class Under4 : AbstractInterpreter<PluginDataFrameSchema>() {
    val Arguments.column: String by string()
    val Arguments.receiver: InsertClauseApproximation by insertClause()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return receiver.df.insertImpl(pathOf(column), receiver.column, anyRow)
    }
}

//@Serializable
public data class PluginDataFrameSchema(
    @Contextual private val columns: List<SimpleCol>
) : DataFrameLikeContainer<SimpleCol> {
    override fun columns(): List<SimpleCol> {
        return columns
    }

    override fun toString(): String {
        return columns.asString()
    }
}

private fun List<SimpleCol>.asString(indent: String = ""): String {
    return joinToString("\n") {
        val col = when (it) {
            is SimpleFrameColumn -> {
                val nullability = if (it.nullable) "?" else ""
                "${it.name}$nullability*\n" + it.columns().asString("$indent   ")
            }
            is SimpleColumnGroup -> {
                "${it.name}\n" + it.columns().asString("$indent   ")
            }
            is SimpleCol -> {
//                val type = (it.type as TypeApproximationImpl).let {
//                    val nullability = if (it.nullable) "?" else ""
//                    "${it.fqName}$nullability"
//                }
                "${it.name}: ${it.type}"
            }
            else -> TODO()
        }
        "$indent$col"
    }
}

//@Serializable
public open class SimpleCol(
    public val name: String,
    @SerialName("valuesType") public open val type: TypeApproximation
) : Col {
//    public constructor(name: String, type: TypeApproximation) : this(name, type as TypeApproximation)

    override fun name(): String {
        return name
    }

    public open fun rename(s: String): SimpleCol {
        return SimpleCol(s, type)
    }

    public open fun changeType(type: TypeApproximation): SimpleCol {
        return SimpleCol(name, type)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SimpleCol

        if (name != other.name) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }

    override fun toString(): String {
        return "SimpleCol(name='$name', type=$type)"
    }

    public open fun kind(): SimpleColumnKind {
        return SimpleColumnKind.VALUE
    }
}

public enum class SimpleColumnKind {
    VALUE, GROUP, FRAME
}

//@Serializable
public data class SimpleFrameColumn(
    private val name1: String,
    @Contextual private val columns: List<SimpleCol>,
    val nullable: Boolean,
    // probably shouldn't be called at all?
    // exists only because SimpleCol has it
    // but in fact it's for `materialize` to decide what should be the type of the property / accessors
    val anyFrameType: TypeApproximation,
) : MyColumnGroup<SimpleCol>, SimpleCol(name1, anyFrameType) {
    override fun columns(): List<SimpleCol> {
        return columns
    }

    override fun rename(s: String): SimpleCol {
        return SimpleFrameColumn(name1, columns, nullable, anyFrameType)
    }

    override fun kind(): SimpleColumnKind {
        return SimpleColumnKind.FRAME
    }
}

//@Serializable
public class SimpleColumnGroup(
    private val name1: String,
    @Contextual private val columns: List<SimpleCol>,
    columnGroupType: TypeApproximation
) : MyColumnGroup<SimpleCol>, SimpleCol(name1, columnGroupType) {

    override fun columns(): List<SimpleCol> {
        return columns
    }

    override fun rename(s: String): SimpleColumnGroup {
        return SimpleColumnGroup(s, columns, type)
    }

    override fun changeType(type: TypeApproximation): SimpleCol {
        return TODO()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as SimpleColumnGroup

        if (name != other.name) return false
        if (columns != other.columns) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + columns.hashCode()
        return result
    }

    override fun kind(): SimpleColumnKind {
        return SimpleColumnKind.GROUP
    }
}

@PublishedApi
internal fun PluginDataFrameSchema.insertImpl(path: ColumnPath, column: SimpleCol, columnGroupType: TypeApproximation): PluginDataFrameSchema {
    val columns = listOf(ColumnToInsert1(path, column))

    return insertImplGenericContainer<PluginDataFrameSchema, SimpleCol, MyColumnGroup<SimpleCol>>(
        this,
        columns,
        columns.firstOrNull()?.referenceNode?.getRoot(),
        0,
        factory = { PluginDataFrameSchema(it) },
        empty = PluginDataFrameSchema(emptyList()),
        rename = { rename(it) },
        createColumnGroup = { name, columns ->
            SimpleColumnGroup(name, columns, columnGroupType)
        }
    )
}

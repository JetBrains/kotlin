package org.jetbrains.kotlinx.dataframe.plugin.impl

import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeNullability
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.isNullable
import org.jetbrains.kotlinx.dataframe.impl.api.DataFrameLikeContainer
import org.jetbrains.kotlinx.dataframe.impl.api.GenericColumn
import org.jetbrains.kotlinx.dataframe.impl.api.GenericColumnGroup
import org.jetbrains.kotlinx.dataframe.plugin.extensions.KotlinTypeFacade
import org.jetbrains.kotlinx.dataframe.plugin.extensions.wrap
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.TypeApproximation
import org.jetbrains.kotlinx.dataframe.plugin.pluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.utils.Names

data class PluginDataFrameSchema(
    private val columns: List<SimpleCol>
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
                "${it.name}*\n" + it.columns().asString("$indent   ")
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

open class SimpleCol(
    val name: String,
    open val type: TypeApproximation
) : GenericColumn {

    override fun name(): String {
        return name
    }

    open fun rename(s: String): SimpleCol {
        return SimpleCol(s, type)
    }

    open fun changeType(type: TypeApproximation): SimpleCol {
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

    open fun kind(): SimpleColumnKind {
        return SimpleColumnKind.VALUE
    }
}

enum class SimpleColumnKind {
    VALUE, GROUP, FRAME
}

data class SimpleFrameColumn(
    private val name1: String,
    private val columns: List<SimpleCol>,
    // probably shouldn't be called at all?
    // exists only because SimpleCol has it
    // but in fact it's for `materialize` to decide what should be the type of the property / accessors
    val anyFrameType: TypeApproximation,
) : GenericColumnGroup<SimpleCol>, SimpleCol(name1, anyFrameType) {
    override fun columns(): List<SimpleCol> {
        return columns
    }

    override fun rename(s: String): SimpleFrameColumn {
        return SimpleFrameColumn(name1, columns, anyFrameType)
    }

    override fun kind(): SimpleColumnKind {
        return SimpleColumnKind.FRAME
    }
}

class SimpleColumnGroup(
    name: String,
    private val columns: List<SimpleCol>,
    columnGroupType: TypeApproximation
) : GenericColumnGroup<SimpleCol>, SimpleCol(name, columnGroupType) {

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

fun KotlinTypeFacade.simpleColumnOf(name: String, type: ConeKotlinType): SimpleCol {
    return if (type.classId == Names.DATA_ROW_CLASS_ID) {
        val schema = pluginDataFrameSchema(type)
        val group = SimpleColumnGroup(name, schema.columns(), anyRow)
        val column = if (type.isNullable) {
            makeNullable(group)
        } else {
            group
        }
        column
    } else if (type.classId == Names.DF_CLASS_ID && type.nullability == ConeNullability.NOT_NULL) {
        val schema = pluginDataFrameSchema(type)
        SimpleFrameColumn(name, schema.columns(), anyDataFrame)
    } else {
        SimpleCol(name, type.wrap())
    }
}

private fun KotlinTypeFacade.makeNullable(column: SimpleCol): SimpleCol {
    return when (column) {
        is SimpleColumnGroup -> {
            SimpleColumnGroup(column.name, column.columns().map { makeNullable(column) }, anyRow)
        }
        is SimpleFrameColumn -> column
        else -> SimpleCol(column.name, column.type.changeNullability { true })
    }
}

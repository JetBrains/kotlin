@file:Suppress("INVISIBLE_REFERENCE", "CANNOT_OVERRIDE_INVISIBLE_MEMBER")

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
            is SimpleDataColumn -> {
                "${it.name}: ${it.type}"
            }
        }
        "$indent$col"
    }
}

sealed interface SimpleCol : GenericColumn {
    val name: String

    override fun name(): String {
        return name
    }

    fun rename(s: String): SimpleCol
}

data class SimpleDataColumn(
    override val name: String,
    val type: TypeApproximation
) : GenericColumn, SimpleCol {

    override fun name(): String {
        return name
    }

    override fun rename(s: String): SimpleDataColumn {
        return SimpleDataColumn(s, type)
    }

    fun changeType(type: TypeApproximation): SimpleDataColumn {
        return SimpleDataColumn(name, type)
    }

}

data class SimpleFrameColumn(
    override val name: String,
    private val columns: List<SimpleCol>
) : GenericColumnGroup<SimpleCol>, SimpleCol {
    override fun columns(): List<SimpleCol> {
        return columns
    }

    override fun rename(s: String): SimpleFrameColumn {
        return SimpleFrameColumn(s, columns)
    }
}

data class SimpleColumnGroup(
    override val name: String,
    private val columns: List<SimpleCol>
) : GenericColumnGroup<SimpleCol>, SimpleCol {

    override fun columns(): List<SimpleCol> {
        return columns
    }

    override fun rename(s: String): SimpleColumnGroup {
        return SimpleColumnGroup(s, columns)
    }
}

fun KotlinTypeFacade.simpleColumnOf(name: String, type: ConeKotlinType): SimpleCol {
    return if (type.classId == Names.DATA_ROW_CLASS_ID) {
        val schema = pluginDataFrameSchema(type)
        val group = SimpleColumnGroup(name, schema.columns())
        val column = if (type.isNullable) {
            makeNullable(group)
        } else {
            group
        }
        column
    } else if (type.classId == Names.DF_CLASS_ID && type.nullability == ConeNullability.NOT_NULL) {
        val schema = pluginDataFrameSchema(type)
        SimpleFrameColumn(name, schema.columns())
    } else {
        SimpleDataColumn(name, type.wrap())
    }
}

private fun KotlinTypeFacade.makeNullable(column: SimpleCol): SimpleCol {
    return when (column) {
        is SimpleColumnGroup -> {
            SimpleColumnGroup(column.name, column.columns().map { makeNullable(column) })
        }
        is SimpleFrameColumn -> column
        is SimpleDataColumn -> SimpleDataColumn(column.name, column.type.changeNullability { true })
    }
}

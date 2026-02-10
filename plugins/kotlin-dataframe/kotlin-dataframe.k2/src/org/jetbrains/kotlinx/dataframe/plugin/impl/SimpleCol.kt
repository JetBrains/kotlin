package org.jetbrains.kotlinx.dataframe.plugin.impl

import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeStarProjection
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.fir.types.isMarkedNullable
import org.jetbrains.kotlin.fir.types.isNullableNothing
import org.jetbrains.kotlin.fir.types.isSubtypeOf
import org.jetbrains.kotlin.fir.types.renderReadable
import org.jetbrains.kotlinx.dataframe.plugin.extensions.ColumnType
import org.jetbrains.kotlinx.dataframe.plugin.extensions.KotlinTypeFacade
import org.jetbrains.kotlinx.dataframe.plugin.extensions.changeNullability
import org.jetbrains.kotlinx.dataframe.plugin.extensions.wrap
import org.jetbrains.kotlinx.dataframe.plugin.findSchemaArgument
import org.jetbrains.kotlinx.dataframe.plugin.getSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.ColumnsResolver
import org.jetbrains.kotlinx.dataframe.plugin.utils.Names

data class PluginDataFrameSchema(
    private val columns: List<SimpleCol>,
) {
    companion object {
        val EMPTY = PluginDataFrameSchema(emptyList())
    }

    /**
     * [impliedColumnsResolver] for operations that need to provide String API support
     */
    fun columns(impliedColumnsResolver: ColumnsResolver): List<SimpleCol> {
        return insertImpliedColumns(impliedColumnsResolver).columns
    }

    // with 2 separate functions, it's easier to find usages.
    // ideally, argument-less function should have a reason to be used, because proper usage indicated String API support
    fun columns(): List<SimpleCol> {
        return columns
    }

    override fun toString(): String {
        return columns.asString()
    }
}

fun PluginDataFrameSchema.add(name: String, type: ConeKotlinType, context: KotlinTypeFacade): PluginDataFrameSchema {
    return PluginDataFrameSchema(columns() + context(context) { simpleColumnOf(name, type) })
}

private fun List<SimpleCol>.asString(indent: String = ""): String {
    if (isEmpty()) return "$indent<empty compile time schema>"
    return joinToString("\n") {
        val col = when (it) {
            is SimpleFrameColumn -> {
                "${it.name}: *\n" + it.columns().asString("$indent ")
            }

            is SimpleColumnGroup -> {
                "${it.name}:\n" + it.columns().asString("$indent ")
            }

            is SimpleDataColumn -> {
                "${it.name}: ${it.type.coneType.renderReadable()}"
            }
        }
        "$indent$col"
    }
}

sealed interface SimpleCol {
    val name: String

    fun name(): String {
        return name
    }

    fun rename(s: String): SimpleCol
}

data class SimpleDataColumn(
    override val name: String,
    val type: ColumnType,
) : SimpleCol {

    override fun name(): String {
        return name
    }

    override fun rename(s: String): SimpleDataColumn {
        return SimpleDataColumn(s, type)
    }

    fun changeType(type: ColumnType): SimpleDataColumn {
        return SimpleDataColumn(name, type)
    }

}

data class SimpleFrameColumn(
    override val name: String,
    private val columns: List<SimpleCol>,
) : SimpleCol {
    fun columns(): List<SimpleCol> {
        return columns
    }

    override fun rename(s: String): SimpleFrameColumn {
        return SimpleFrameColumn(s, columns)
    }
}

data class SimpleColumnGroup(
    override val name: String,
    private val columns: List<SimpleCol>,
) : SimpleCol {

    fun columns(): List<SimpleCol> {
        return columns
    }

    override fun rename(s: String): SimpleColumnGroup {
        return SimpleColumnGroup(s, columns)
    }
}

context(facade: KotlinTypeFacade)
fun simpleColumnOf(name: String, type: ConeKotlinType): SimpleCol {
    fun extractSchema(): PluginDataFrameSchema {
        val objectWithSchema = type.findSchemaArgument(facade.isTest) ?: error("Cannot extract DataFrame schema from type: $type")
        val schema = objectWithSchema.getSchema()
        return schema
    }

    val nullableDataRow = Names.DATA_ROW_CLASS_ID.constructClassLikeType(arrayOf(ConeStarProjection), isMarkedNullable = true)
    val dataFrame = Names.DF_CLASS_ID.constructClassLikeType(arrayOf(ConeStarProjection))
    return if (!type.isNullableNothing && type.isSubtypeOf(nullableDataRow, facade.session)) {
        val schema = extractSchema()
        val group = SimpleColumnGroup(name, schema.columns())
        val column = if (type.isMarkedNullable) {
            makeNullable(group)
        } else {
            group
        }
        column
    } else if (!type.isMarkedNullable && type.isSubtypeOf(dataFrame, facade.session)) {
        val schema = extractSchema()
        SimpleFrameColumn(name, schema.columns())
    } else {
        SimpleDataColumn(name, type.wrap())
    }
}

context(facade: KotlinTypeFacade)
internal fun makeNullable(column: SimpleCol): SimpleCol {
    return when (column) {
        is SimpleColumnGroup -> {
            SimpleColumnGroup(column.name, column.columns().map { makeNullable(it) })
        }

        is SimpleFrameColumn -> column
        is SimpleDataColumn -> SimpleDataColumn(column.name, column.type.changeNullability { true })
    }
}

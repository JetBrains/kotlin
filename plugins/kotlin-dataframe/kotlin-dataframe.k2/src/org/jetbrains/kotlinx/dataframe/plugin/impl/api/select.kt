package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeStarProjection
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.fir.types.isMarkedNullable
import org.jetbrains.kotlin.fir.types.isSubtypeOf
import org.jetbrains.kotlinx.dataframe.api.asColumnGroup
import org.jetbrains.kotlinx.dataframe.api.cast
import org.jetbrains.kotlinx.dataframe.api.pathOf
import org.jetbrains.kotlinx.dataframe.api.select
import org.jetbrains.kotlinx.dataframe.columns.*
import org.jetbrains.kotlinx.dataframe.impl.columns.ColumnsList
import org.jetbrains.kotlinx.dataframe.plugin.extensions.ColumnType
import org.jetbrains.kotlinx.dataframe.plugin.impl.*
import org.jetbrains.kotlinx.dataframe.plugin.impl.data.ColumnWithPathApproximation
import org.jetbrains.kotlinx.dataframe.plugin.utils.Names

/**
 * NOTE: Serves both, select and distinct operations.
 */
internal class Select0 : AbstractInterpreter<PluginDataFrameSchema>() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.columns: ColumnsResolver by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return PluginDataFrameSchema(columns.resolve(receiver).map { it.column })
    }
}

internal class SelectString : AbstractInterpreter<PluginDataFrameSchema>() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.columns: List<String> by arg(defaultValue = Present(emptyList()))

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val df = receiver.insertImpliedColumns(columns)
        return df.asDataFrame().select { columns.toColumnSet() }.toPluginDataFrameSchema()
    }
}

internal class Expr0 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver by ignore()
    val Arguments.name: String by arg(defaultValue = Present("untitled"))
    val Arguments.infer by ignore()
    val Arguments.expression: ColumnType by type()

    override fun Arguments.interpret(): ColumnsResolver {
        return ResolvedDataColumn(
            ColumnWithPathApproximation(
                ColumnPath(listOf(name)),
                SimpleDataColumn(name, expression)
            )
        )
    }
}

internal class And0 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: ColumnsResolver by arg()
    val Arguments.other: ColumnsResolver by arg()

    override fun Arguments.interpret(): ColumnsResolver {
        return object : ColumnsResolver, ColumnsList<Any?> {
            override val columns = listOf(receiver, other)

            override fun resolve(df: PluginDataFrameSchema): List<ColumnWithPathApproximation> {
                return receiver.resolve(df) + other.resolve(df)
            }

            override fun toString(): String {
                return "$receiver and $other"
            }
        }
    }
}

internal class All0 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: ColumnsResolver by arg()
    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver { receiver.all() }
    }
}

internal class All1 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver by ignore()
    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver { all() }
    }
}

internal class All2 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: SingleColumnApproximation by arg()
    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver {
            receiver.asColumnGroup().allCols()
        }
    }
}

internal class AllAfter0 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: ColumnsResolver by arg()
    val Arguments.column: SingleColumnApproximation by arg()
    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver { receiver.allAfter(column) }
    }
}

internal class AllAfter1 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver by ignore()
    val Arguments.column: SingleColumnApproximation by arg()
    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver { allAfter(column) }
    }
}

internal class AllAfter2 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver by ignore()
    val Arguments.column: SingleColumnApproximation by arg()
    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver { allAfter(column) }
    }
}

internal class AllAfter3 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: SingleColumnApproximation by arg()
    val Arguments.column: SingleColumnApproximation by arg()
    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver {
            receiver.asColumnGroup().allColsAfter(column)
        }
    }
}

internal class AllBefore0 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: ColumnsResolver by arg()
    val Arguments.column: SingleColumnApproximation by arg()
    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver { receiver.allBefore(column) }
    }
}

internal class AllBefore1 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver by ignore()
    val Arguments.column: SingleColumnApproximation by arg()
    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver { allBefore(column) }
    }
}

internal class AllBefore2 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver by ignore()
    val Arguments.column: SingleColumnApproximation by arg()
    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver { allBefore(column) }
    }
}

internal class AllUpTo0 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: ColumnsResolver by arg()
    val Arguments.column: SingleColumnApproximation by arg()
    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver { receiver.allUpTo(column) }
    }
}

internal class AllUpTo1 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver by ignore()
    val Arguments.column: SingleColumnApproximation by arg()
    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver { allUpTo(column) }
    }
}

internal class AllUpTo2 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver by ignore()
    val Arguments.column: SingleColumnApproximation by arg()
    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver { allUpTo(column) }
    }
}

internal class AllFrom0 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: ColumnsResolver by arg()
    val Arguments.column: SingleColumnApproximation by arg()
    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver { receiver.allFrom(column) }
    }
}

internal class AllFrom1 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver by ignore()
    val Arguments.column: SingleColumnApproximation by arg()
    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver { allFrom(column) }
    }
}

internal class AllFrom2 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver by ignore()
    val Arguments.column: SingleColumnApproximation by arg()
    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver { allFrom(column) }
    }
}

internal class ColsOf0 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver by ignore()
    val Arguments.typeArg0: ColumnType by arg()

    override fun Arguments.interpret(): ColumnsResolver {
        return object : ColumnsResolver {
            override fun resolve(df: PluginDataFrameSchema): List<ColumnWithPathApproximation> {
                val cols = df.columns().map {
                    val path = ColumnPath(listOf(it.name))
                    ColumnWithPathApproximation(path, it)
                }
                return colsOf(cols, typeArg0.coneType)
            }
        }
    }

}

private fun Arguments.colsOf(cols: List<ColumnWithPathApproximation>, type: ConeKotlinType) =
    cols
        .filter {
            it.column.isColOf(type, session)
        }

fun SimpleCol.isColOf(type: ConeKotlinType, session: FirSession): Boolean {
    val columnType = when (this) {
        is SimpleDataColumn -> this.type.coneType
        is SimpleColumnGroup -> Names.DATA_ROW_CLASS_ID.constructClassLikeType(
            typeArguments = arrayOf(session.builtinTypes.anyType.coneType)
        )
        is SimpleFrameColumn -> Names.DF_CLASS_ID.constructClassLikeType(
            typeArguments = arrayOf(session.builtinTypes.anyType.coneType)
        )
    }
    return columnType.isSubtypeOf(type, session)
}

internal class ColsAtAnyDepth0 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: ColumnsResolver by arg()

    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver { receiver.colsAtAnyDepth() }
    }
}

internal class ColsAtAnyDepth1 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver by ignore()
    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver { colsAtAnyDepth() }
    }
}

internal class ColsAtAnyDepth2 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: SingleColumnApproximation by arg()
    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver {
            receiver.asColumnGroup().colsAtAnyDepth()
        }
    }
}

internal class ColsOf1 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: ColumnsResolver by arg()
    val Arguments.typeArg0: ColumnType by arg()

    override fun Arguments.interpret(): ColumnsResolver {
        return object : ColumnsResolver {
            override fun resolve(df: PluginDataFrameSchema): List<ColumnWithPathApproximation> {
                return colsOf(receiver.resolve(df), typeArg0.coneType)
            }
        }
    }
}

internal class ColsOf2 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: SingleColumnApproximation by arg()
    val Arguments.typeArg0: ColumnType by arg()

    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver {
            receiver.all().filter { it.asSimpleColumn().isColOf(typeArg0.coneType, session) }
        }
    }
}

internal class WithoutNulls0 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: ColumnsResolver by arg()

    override fun Arguments.interpret(): ColumnsResolver {
        return object : ColumnsResolver {
            override fun resolve(df: PluginDataFrameSchema): List<ColumnWithPathApproximation> {
                return receiver.resolve(df).filter {
                    withoutNulls(it.column)
                }
            }
        }
    }
}

private fun withoutNulls(col: SimpleCol): Boolean = col !is SimpleDataColumn || !col.type.coneType.isMarkedNullable

internal class WithoutNulls1 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver by ignore()
    override fun Arguments.interpret(): ColumnsResolver {
        return object : ColumnsResolver {
            override fun resolve(df: PluginDataFrameSchema): List<ColumnWithPathApproximation> {
                val cols = df.columns().map {
                    val path = ColumnPath(listOf(it.name))
                    ColumnWithPathApproximation(path, it)
                }
                return cols.filter {
                    withoutNulls(it.column)
                }
            }
        }
    }
}

internal class WithoutNulls2 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: SingleColumnApproximation by arg()
    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver {
            receiver.filter {
                withoutNulls(it.data.asSimpleColumn())
            }
        }
    }
}

internal class FrameCols0 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: ColumnsResolver by arg()

    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver { receiver.frameCols() }
    }
}

internal class FrameCols1 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver by ignore()

    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver { frameCols() }
    }
}

internal class FrameCols2 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: SingleColumnApproximation by arg()

    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver { receiver.asColumnGroup().frameCols() }
    }
}

internal class ColGroups0 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: ColumnsResolver by arg()

    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver { receiver.colGroups() }
    }
}

internal class ColGroups1 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver by ignore()

    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver { colGroups() }
    }
}

internal class ColGroups2 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: SingleColumnApproximation by arg()

    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver { receiver.asColumnGroup().colGroups() }
    }
}

internal class NameContains0 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: ColumnsResolver by arg()
    val Arguments.text: String by arg()
    val Arguments.ignoreCase: Boolean by arg(defaultValue = Present(false))
    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver { receiver.nameContains(text, ignoreCase) }
    }
}

internal class NameContains1 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver by ignore()
    val Arguments.text: String by arg()
    val Arguments.ignoreCase: Boolean by arg(defaultValue = Present(false))
    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver { nameContains(text, ignoreCase) }
    }
}

internal class NameContains2 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: SingleColumnApproximation by arg()
    val Arguments.text: String by arg()
    val Arguments.ignoreCase: Boolean by arg(defaultValue = Present(false))
    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver {
            receiver.asColumnGroup().colsNameContains(text, ignoreCase)
        }
    }
}

internal class NameStartsWith0 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: ColumnsResolver by arg()
    val Arguments.prefix: String by arg()
    val Arguments.ignoreCase: Boolean by arg(defaultValue = Present(false))
    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver { receiver.nameStartsWith(prefix, ignoreCase) }
    }
}

internal class NameStartsWith1 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver by ignore()
    val Arguments.prefix: String by arg()
    val Arguments.ignoreCase: Boolean by arg(defaultValue = Present(false))
    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver { nameStartsWith(prefix, ignoreCase) }
    }
}

internal class NameStartsWith2 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: SingleColumnApproximation by arg()
    val Arguments.prefix: String by arg()
    val Arguments.ignoreCase: Boolean by arg(defaultValue = Present(false))
    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver {
            receiver.asColumnGroup().colsNameStartsWith(prefix, ignoreCase)
        }
    }
}

internal class NameEndsWith0 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: ColumnsResolver by arg()
    val Arguments.suffix: String by arg()
    val Arguments.ignoreCase: Boolean by arg(defaultValue = Present(false))
    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver { receiver.nameEndsWith(suffix, ignoreCase) }
    }
}

internal class NameEndsWith1 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver by ignore()
    val Arguments.suffix: String by arg()
    val Arguments.ignoreCase: Boolean by arg(defaultValue = Present(false))
    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver { nameEndsWith(suffix, ignoreCase) }
    }
}

internal class NameEndsWith2 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: SingleColumnApproximation by arg()
    val Arguments.suffix: String by arg()
    val Arguments.ignoreCase: Boolean by arg(defaultValue = Present(false))
    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver {
            receiver.asColumnGroup().colsNameEndsWith(suffix, ignoreCase)
        }
    }
}

internal class ColumnRange : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: SingleColumnApproximation by arg()
    val Arguments.endInclusive: SingleColumnApproximation by arg()
    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver {
            receiver..endInclusive
        }
    }
}

internal class Cols0 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver by ignore()
    val Arguments.firstCol: SingleColumnApproximation by arg()
    val Arguments.otherCols: List<Interpreter.Success<*>> by arg()
    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver { cols(firstCol, *otherCols.map { it.value as SingleColumnApproximation }.toTypedArray()) }
    }
}

internal class Single0 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: ColumnsResolver by arg()

    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver { receiver.single() }
    }
}

internal class Single1 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver by ignore()
    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver { single() }
    }
}

internal class Single2 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: SingleColumnApproximation by arg()
    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver {
            receiver.asColumnGroup().singleCol()
        }
    }
}

internal class First0 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: ColumnsResolver by arg()

    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver { receiver.first() }
    }
}

internal class First1 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver by ignore()
    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver { first() }
    }
}

internal class First2 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: SingleColumnApproximation by arg()
    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver {
            receiver.asColumnGroup().firstCol()
        }
    }
}

internal class Last0 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: ColumnsResolver by arg()

    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver { receiver.last() }
    }
}

internal class Last1 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver by ignore()
    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver { last() }
    }
}

internal class Last2 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: SingleColumnApproximation by arg()
    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver {
            receiver.asColumnGroup().lastCol()
        }
    }
}

internal class Take0 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: ColumnsResolver by arg()
    val Arguments.n: Int by arg()

    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver { receiver.take(n) }
    }
}

internal class Take1 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver by ignore()
    val Arguments.n: Int by arg()

    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver { take(n) }
    }
}

internal class Take2 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: SingleColumnApproximation by arg()
    val Arguments.n: Int by arg()

    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver {
            receiver.asColumnGroup().takeCols(n)
        }
    }
}

internal class TakeLast0 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: ColumnsResolver by arg()
    val Arguments.n: Int by arg(defaultValue = Present(1))

    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver { receiver.takeLast(n) }
    }
}

internal class TakeLast1 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver by ignore()
    val Arguments.n: Int by arg(defaultValue = Present(1))

    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver { takeLast(n) }
    }
}

internal class TakeLast2 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: SingleColumnApproximation by arg()
    val Arguments.n: Int by arg()

    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver {
            receiver.asColumnGroup().takeLastCols(n)
        }
    }
}

internal class Drop0 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: ColumnsResolver by arg()
    val Arguments.n: Int by arg()

    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver { receiver.drop(n) }
    }
}

internal class Drop1 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver by ignore()
    val Arguments.n: Int by arg()

    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver { drop(n) }
    }
}

internal class Drop2 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: SingleColumnApproximation by arg()
    val Arguments.n: Int by arg()

    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver {
            receiver.asColumnGroup().dropCols(n)
        }
    }
}

internal class DropLast0 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: ColumnsResolver by arg()
    val Arguments.n: Int by arg(defaultValue = Present(1))

    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver { receiver.dropLast(n) }
    }
}

internal class DropLast1 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver by ignore()
    val Arguments.n: Int by arg(defaultValue = Present(1))

    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver { dropLast(n) }
    }
}

internal class DropLast2 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: SingleColumnApproximation by arg()
    val Arguments.n: Int by arg()

    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver {
            receiver.asColumnGroup().dropLastCols(n)
        }
    }
}

internal class ValueCols0 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: ColumnsResolver by arg()

    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver { receiver.valueCols() }
    }
}

internal class ValueCols1 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver by ignore()
    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver { valueCols() }
    }
}

internal class ValueCols2 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: SingleColumnApproximation by arg()
    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver {
            receiver.valueCols()
        }
    }
}

internal class Named0 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: SingleColumnApproximation by arg()
    val Arguments.newName: String by arg()

    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver { receiver named newName }
    }
}

internal class NestedSelect : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: SingleColumnApproximation by arg()
    val Arguments.selector: ColumnsResolver by arg()

    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver { receiver.asColumnGroup().select { selector } }
    }
}

// "myCol"()
internal class StringInvokeUntyped : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: String by arg()

    override fun Arguments.interpret(): ColumnsResolver {
        return stringApiColumnResolver(path = pathOf(receiver), session.builtinTypes.nullableAnyType.coneType)
    }
}

// "myCol"<Int>()
internal class StringInvokeTyped : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: String by arg()
    val Arguments.typeArg0 by type()

    override fun Arguments.interpret(): ColumnsResolver {
        return stringApiColumnResolver(path = pathOf(receiver), typeArg0.coneType)
    }
}

// "group"["myCol"]<Int>()
internal class ColumnPathInvokeTyped : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: ColumnPathApproximation by arg()
    val Arguments.typeArg0 by type()

    override fun Arguments.interpret(): ColumnsResolver {
        return stringApiColumnResolver(receiver.path, typeArg0.coneType)
    }
}

// "group"["myCol"]
internal class StringGetColumn : AbstractInterpreter<ColumnPathApproximation>() {
    val Arguments.receiver: String by arg()
    val Arguments.column: String by arg()

    override fun Arguments.interpret(): ColumnPathApproximation {
        return ColumnPathApproximation(listOf(receiver, column))
    }
}

// "group"["anotherGroup"]["myCol"]
internal class ColumnPathGetColumn : AbstractInterpreter<ColumnPathApproximation>() {
    val Arguments.receiver: ColumnPathApproximation by arg()
    val Arguments.column: String by arg()

    override fun Arguments.interpret(): ColumnPathApproximation {
        return ColumnPathApproximation(receiver.path + column)
    }
}

fun Arguments.stringApiColumnResolver(path: ColumnPath, type: ConeKotlinType): SingleColumnApproximation {
    return object : ColumnsResolverAdapter(), SingleColumnApproximation {
        // we want to help users gradually introduce typed information to their dataframe
        // if they refer to a column by String API once, let's apply logic similar to smart cast and
        // add such a column to the schema.
        // over time all callers of columns() and asDataFrame() should be migrated to correctly use impliedColumnsResolver
        override fun resolve(df: PluginDataFrameSchema): List<ColumnWithPathApproximation> {
            val df = df.asDataFrame()
            val col = df.getColumnOrNull(path)
                ?.let { ColumnWithPathApproximation(path, it.asSimpleColumn(), isImpliedColumn = false) }
                ?: createImpliedColumn(path, type)
            return listOf(col)
        }

        private fun createImpliedColumn(path: ColumnPath, type: ConeKotlinType): ColumnWithPathApproximation {
            return ColumnWithPathApproximation(path, simpleColumnOf(path.name(), type), isImpliedColumn = true)
        }

        override fun rename(newName: String): SingleColumnApproximation {
            return stringApiColumnResolver(path.rename(newName), type)
        }

        override fun name(): String = path.name()

        override val path: ColumnPath = path

        override fun resolve(context: ColumnResolutionContext): List<ColumnWithPath<Any?>> {
            return resolve(context.df.cast<ConeTypesAdapter>().toPluginDataFrameSchema())
                .map { ColumnWithPath(it.column.asDataColumn(), it.path) }
        }

        override fun toString(): String {
            return "StringApiReference($path: $type)"
        }
    }
}

// col<Int>(0) [named "newName"]
class ColByIndex : AbstractInterpreter<SingleColumnApproximation>() {
    val Arguments.receiver by ignore()
    val Arguments.index: Int by arg()
    val Arguments.typeArg0 by type()

    override fun Arguments.interpret(): SingleColumnApproximation {
        return columnByIndexResolver(name = null, typeArg0.coneType, index)
    }
}

// col(0) [named "newName"]
class ColByIndexUntyped : AbstractInterpreter<SingleColumnApproximation>() {
    val Arguments.receiver by ignore()
    val Arguments.index: Int by arg()

    override fun Arguments.interpret(): SingleColumnApproximation {
        return columnByIndexResolver(name = null, session.builtinTypes.nullableAnyType.coneType, index)
    }
}

private fun Arguments.columnByIndexResolver(
    name: String?,
    type: ConeKotlinType,
    index: Int,
): SingleColumnApproximation = object : SingleColumnApproximation {
    override fun resolve(df: PluginDataFrameSchema): List<ColumnWithPathApproximation> {
        return stringApiColumnResolver(pathOf(requireName()), type).resolve(df)
    }

    override fun resolve(context: ColumnResolutionContext): List<ColumnWithPath<Any?>> {
        return stringApiColumnResolver(pathOf(requireName()), type).resolve(context)
    }

    override val path: ColumnPath get() = pathOf(requireName())

    override fun name(): String = requireName()

    override fun rename(newName: String): SingleColumnApproximation {
        return columnByIndexResolver(newName, type, index)
    }

    private fun requireName(): String {
        if (name == null) {
            error("col($index) needs to be 'named'")
        }
        return name
    }
}

internal class Named1 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: SingleColumnApproximation by arg()
    val Arguments.newName: String by arg()

    override fun Arguments.interpret(): ColumnsResolver {
        return receiver.rename(newName)
    }
}

// col<Int>("name")
internal class ColByString : AbstractInterpreter<SingleColumnApproximation>() {
    val Arguments.receiver by ignore()
    val Arguments.name: String by arg()
    val Arguments.typeArg0 by type()

    override fun Arguments.interpret(): SingleColumnApproximation {
        return stringApiColumnResolver(pathOf(name), typeArg0.coneType)
    }
}

// col("name")
internal class ColByStringUntyped : AbstractInterpreter<SingleColumnApproximation>() {
    val Arguments.receiver by ignore()
    val Arguments.name: String by arg()

    override fun Arguments.interpret(): SingleColumnApproximation {
        return stringApiColumnResolver(pathOf(name), session.builtinTypes.nullableAnyType.coneType)
    }
}

// "group".col("name")
internal class StringNestedColUntyped : AbstractInterpreter<SingleColumnApproximation>() {
    val Arguments.receiver: String by arg()
    val Arguments.name: String by arg()

    override fun Arguments.interpret(): SingleColumnApproximation {
        return stringApiColumnResolver(pathOf(receiver, name), session.builtinTypes.nullableAnyType.coneType)
    }
}

// "group".col<Int>("name")
internal class StringNestedCol : AbstractInterpreter<SingleColumnApproximation>() {
    val Arguments.receiver: String by arg()
    val Arguments.name: String by arg()
    val Arguments.typeArg0 by type()

    override fun Arguments.interpret(): SingleColumnApproximation {
        return stringApiColumnResolver(pathOf(receiver, name), typeArg0.coneType)
    }
}

// pathOf("group").col("name")
internal class ColumnPathColUntyped : AbstractInterpreter<SingleColumnApproximation>() {
    val Arguments.receiver: ColumnPathApproximation by arg()
    val Arguments.name: String by arg()

    override fun Arguments.interpret(): SingleColumnApproximation {
        return stringApiColumnResolver(receiver.path + name, session.builtinTypes.nullableAnyType.coneType)
    }
}

// pathOf("group").col<Int>("name")
internal class ColumnPathCol : AbstractInterpreter<SingleColumnApproximation>() {
    val Arguments.receiver: ColumnPathApproximation by arg()
    val Arguments.name: String by arg()
    val Arguments.typeArg0 by type()

    override fun Arguments.interpret(): SingleColumnApproximation {
        return stringApiColumnResolver(receiver.path + name, typeArg0.coneType)
    }
}

// "group".select { ... }
internal class StringSelect : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: String by arg()
    val Arguments.selector: ColumnsResolver by arg()

    override fun Arguments.interpret(): ColumnsResolver {
        val anyRow = Names.DATA_ROW_CLASS_ID.constructClassLikeType(arrayOf(ConeStarProjection), isMarkedNullable = false)
        val columnRef = stringApiColumnResolver(pathOf(receiver), anyRow)
        return columnsResolver { columnRef.asColumnGroup().select { selector } }
    }
}

// pathOf("group").select { ... }
internal class ColumnPathSelect : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: ColumnPathApproximation by arg()
    val Arguments.selector: ColumnsResolver by arg()

    override fun Arguments.interpret(): ColumnsResolver {
        val anyRow = Names.DATA_ROW_CLASS_ID.constructClassLikeType(arrayOf(ConeStarProjection), isMarkedNullable = false)
        val columnRef = stringApiColumnResolver(receiver.path, anyRow)
        return columnsResolver { columnRef.asColumnGroup().select { selector } }
    }
}

// pathOf("group")
internal class PathOf : AbstractInterpreter<ColumnPathApproximation>() {
    val Arguments.receiver by ignore()
    val Arguments.columnNames: List<String> by arg()

    override fun Arguments.interpret(): ColumnPathApproximation {
        return ColumnPathApproximation(columnNames)
    }
}

// Value of this class can appear where SingleColumnApproximation is expected, for example, df.select { pathOf() }
// That's why simple ColumnPath cannot be used as is in return type or arguments of Interpreters, need a wrapper
class ColumnPathApproximation(
    override val path: ColumnPath,
    private val resolver: SingleColumnApproximation,
) : SingleColumnApproximation by resolver

fun Arguments.ColumnPathApproximation(
    path: List<String>,
    type: ConeKotlinType = session.builtinTypes.nullableAnyType.coneType,
): ColumnPathApproximation {
    val columnPath = ColumnPath(path)
    return ColumnPathApproximation(
        columnPath,
        stringApiColumnResolver(columnPath, type)
    )
}
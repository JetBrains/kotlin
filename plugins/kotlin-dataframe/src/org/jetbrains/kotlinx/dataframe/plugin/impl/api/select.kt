package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.isMarkedNullable
import org.jetbrains.kotlin.fir.types.isSubtypeOf
import org.jetbrains.kotlinx.dataframe.api.asColumnGroup
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.Arguments
import org.jetbrains.kotlinx.dataframe.plugin.impl.Interpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.Present
import org.jetbrains.kotlinx.dataframe.plugin.impl.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleCol
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleColumnGroup
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleDataColumn
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleFrameColumn
import org.jetbrains.kotlinx.dataframe.plugin.impl.asSimpleColumn
import org.jetbrains.kotlinx.dataframe.plugin.impl.data.ColumnPathApproximation
import org.jetbrains.kotlinx.dataframe.plugin.impl.data.ColumnWithPathApproximation
import org.jetbrains.kotlinx.dataframe.plugin.impl.dataFrame
import org.jetbrains.kotlinx.dataframe.plugin.impl.ignore
import org.jetbrains.kotlinx.dataframe.plugin.impl.type
import org.jetbrains.kotlinx.dataframe.plugin.utils.Names

internal class Select0 : AbstractInterpreter<PluginDataFrameSchema>() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.columns: ColumnsResolver by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return PluginDataFrameSchema(columns.resolve(receiver).map { it.column })
    }
}

internal class Expr0 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver by ignore()
    val Arguments.name: String by arg(defaultValue = Present("untitled"))
    val Arguments.infer by ignore()
    val Arguments.expression: TypeApproximation by type()

    override fun Arguments.interpret(): ColumnsResolver {
        return SingleColumnApproximation(
            ColumnWithPathApproximation(
                ColumnPathApproximation(listOf(name)),
                SimpleDataColumn(name, expression)
            )
        )
    }
}

internal class And0 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: ColumnsResolver by arg()
    val Arguments.other: ColumnsResolver by arg()

    override fun Arguments.interpret(): ColumnsResolver {
        return object : ColumnsResolver {
            override fun resolve(df: PluginDataFrameSchema): List<ColumnWithPathApproximation> {
                return receiver.resolve(df) + other.resolve(df)
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
    val Arguments.typeArg0: TypeApproximation by arg()

    override fun Arguments.interpret(): ColumnsResolver {
        return object : ColumnsResolver {
            override fun resolve(df: PluginDataFrameSchema): List<ColumnWithPathApproximation> {
                val cols = df.columns().map {
                    val path = ColumnPathApproximation(listOf(it.name))
                    ColumnWithPathApproximation(path, it)
                }
                return colsOf(cols, typeArg0.type)
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
        is SimpleDataColumn -> this.type.type
        is SimpleColumnGroup -> ConeClassLikeTypeImpl(
            ConeClassLikeLookupTagImpl(Names.DATA_ROW_CLASS_ID),
            typeArguments = arrayOf(session.builtinTypes.anyType.coneType),
            isMarkedNullable = false
        )
        is SimpleFrameColumn -> ConeClassLikeTypeImpl(
            ConeClassLikeLookupTagImpl(Names.DF_CLASS_ID),
            typeArguments = arrayOf(session.builtinTypes.anyType.coneType),
            isMarkedNullable = false
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
    val Arguments.typeArg0: TypeApproximation by arg()

    override fun Arguments.interpret(): ColumnsResolver {
        return object : ColumnsResolver {
            override fun resolve(df: PluginDataFrameSchema): List<ColumnWithPathApproximation> {
                return colsOf(receiver.resolve(df), typeArg0.type)
            }
        }
    }
}

internal class ColsOf2 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: SingleColumnApproximation by arg()
    val Arguments.typeArg0: TypeApproximation by arg()

    override fun Arguments.interpret(): ColumnsResolver {
        return columnsResolver {
            receiver.cols().filter { it.asSimpleColumn().isColOf(typeArg0.type, session) }
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

private fun withoutNulls(col: SimpleCol): Boolean = col !is SimpleDataColumn || !col.type.type.isMarkedNullable

internal class WithoutNulls1 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver by ignore()
    override fun Arguments.interpret(): ColumnsResolver {
        return object : ColumnsResolver {
            override fun resolve(df: PluginDataFrameSchema): List<ColumnWithPathApproximation> {
                val cols = df.columns().map {
                    val path = ColumnPathApproximation(listOf(it.name))
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

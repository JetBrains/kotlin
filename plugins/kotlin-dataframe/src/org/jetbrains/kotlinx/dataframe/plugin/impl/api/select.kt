package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.isSubtypeOf
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.Arguments
import org.jetbrains.kotlinx.dataframe.plugin.impl.Present
import org.jetbrains.kotlinx.dataframe.api.Infer
import org.jetbrains.kotlinx.dataframe.plugin.impl.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleDataColumn
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleFrameColumn
import org.jetbrains.kotlinx.dataframe.plugin.impl.data.ColumnPathApproximation
import org.jetbrains.kotlinx.dataframe.plugin.impl.data.ColumnWithPathApproximation
import org.jetbrains.kotlinx.dataframe.plugin.impl.dataFrame
import org.jetbrains.kotlinx.dataframe.plugin.impl.enum
import org.jetbrains.kotlinx.dataframe.plugin.impl.type

internal class Select0 : AbstractInterpreter<PluginDataFrameSchema>() {
    val Arguments.receiver: PluginDataFrameSchema by dataFrame()
    val Arguments.columns: ColumnsResolver by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return PluginDataFrameSchema(columns.resolve(receiver).map { it.column })
    }
}

internal class Expr0 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.name: String by arg(defaultValue = Present("untitled"))
    val Arguments.infer: Infer by enum(defaultValue = Present(Infer.Nulls))
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
    override fun Arguments.interpret(): ColumnsResolver {
        return object : ColumnsResolver {
            override fun resolve(df: PluginDataFrameSchema): List<ColumnWithPathApproximation> {
                return df.columns().map {
                    val path = ColumnPathApproximation(listOf(it.name))
                    ColumnWithPathApproximation(path, it)
                }
            }
        }
    }
}

internal class ColsOf0 : AbstractInterpreter<ColumnsResolver>() {
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
            val column = it.column
            column is SimpleDataColumn && column.type.type.isSubtypeOf(type, session)
        }

internal class ColsAtAnyDepth0 : AbstractInterpreter<ColumnsResolver>() {
    override fun Arguments.interpret(): ColumnsResolver {
        return object : ColumnsResolver {
            override fun resolve(df: PluginDataFrameSchema): List<ColumnWithPathApproximation> {
                return df.flatten(includeFrames = false)
            }
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

internal class FrameCols0 : AbstractInterpreter<ColumnsResolver>() {
    val Arguments.receiver: ColumnsResolver by arg()

    override fun Arguments.interpret(): ColumnsResolver {
        return object : ColumnsResolver {
            override fun resolve(df: PluginDataFrameSchema): List<ColumnWithPathApproximation> {
                return receiver.resolve(df).filter { it.column is SimpleFrameColumn }
            }
        }
    }
}

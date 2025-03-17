/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlinx.dataframe.plugin.extensions.KotlinTypeFacade
import org.jetbrains.kotlinx.dataframe.plugin.impl.Interpreter
import org.jetbrains.kotlinx.dataframe.plugin.utils.Names.INTERPRETABLE_FQNAME
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Add
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.AddWithDsl
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.And0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.And10
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Convert0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Convert2
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Convert6
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.DataFrameGroupBy
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.DropNulls0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Exclude0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Exclude1
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Explode0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Expr0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.From
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Group0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.AggregateDslInto
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.GroupByToDataFrame
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Insert0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Insert1
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Insert2
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Insert3
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Into
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Into0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Join0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Match0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Preserve0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Preserve1
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Properties0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Remove0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Rename
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.RenameInto
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Select0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.To0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Under0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Under1
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Under2
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Under3
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Under4
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Ungroup0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.With0
import org.jetbrains.kotlin.fir.declarations.findArgumentByName
import org.jetbrains.kotlin.fir.expressions.FirClassReferenceExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirGetClassCall
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.UnresolvedExpressionTypeAccess
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.toResolvedFunctionSymbol
import org.jetbrains.kotlin.fir.resolve.fqName
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.AddDslNamedGroup
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.AddDslStringInvoke
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.AddId
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Aggregate
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.All0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.All1
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.All2
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.AllAfter0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.AllAfter1
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.AllAfter2
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.AllAfter3
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.AllBefore0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.AllBefore1
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.AllBefore2
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.AllFrom0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.AllFrom1
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.AllFrom2
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.AllUpTo0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.AllUpTo1
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.AllUpTo2
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.ByName
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.ColGroups0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.ColGroups1
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.ColGroups2
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Cols0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.ColsAtAnyDepth0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.ColsAtAnyDepth1
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.ColsAtAnyDepth2
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.ColsOf0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.ColsOf1
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.ColsOf2
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.ColumnRange
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.DataFrameBuilderInvoke0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.DataFrameOf0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.DataFrameOf3
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.DataFrameXs
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Drop0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Drop1
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Drop2
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.DropLast0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.DropLast1
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.DropLast2
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.DropNa0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.FillNulls0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.First0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.First1
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.First2
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Flatten0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.FlattenDefault
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.FrameCols0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.FrameCols1
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.FrameCols2
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.GroupByAdd
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.GroupByCount0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.GroupByInto
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.GroupByMaxOf
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.GroupByMinOf
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.GroupByReduceExpression
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.GroupByReduceInto
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.GroupByReducePredicate
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.GroupByXs
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Last0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Last1
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Last2
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.MapToFrame
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Merge0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.MergeId
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.MergeBy0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.MergeBy1
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.MergeInto0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Move0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.MoveAfter0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.MoveInto0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.MoveToStart0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.MoveToStart1
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.MoveToEnd0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.MoveUnder0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.MoveUnder1
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.NameContains0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.NameContains1
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.NameContains2
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.NameEndsWith0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.NameEndsWith1
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.NameEndsWith2
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.NameStartsWith0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.NameStartsWith1
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.NameStartsWith2
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Named0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.PairConstructor
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.PairToConstructor
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.PerRowCol
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.RenameMapping
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.ToDataFrame
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.ToDataFrameColumn
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.ToDataFrameDefault
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.ToDataFrameDsl
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.ToDataFrameDslStringInvoke
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.ToDataFrameFrom
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.ToTop
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.TrimMargin
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Update0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.UpdateWith0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.ValueCounts
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.RenameToCamelCase
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.RenameToCamelCaseClause
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Reorder
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.ReorderColumnsByName
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Single0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Single1
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Single2
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.ValueCols2
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Take0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Take1
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Take2
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.TakeLast0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.TakeLast1
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.TakeLast2
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.ValueCols0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.ValueCols1
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.WithoutNulls0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.WithoutNulls1
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.WithoutNulls2
import org.jetbrains.kotlinx.dataframe.plugin.utils.Names

internal fun FirFunctionCall.loadInterpreter(session: FirSession): Interpreter<*>? {
    val interpreter = Stdlib.interpreter(this)
    if (interpreter != null) return interpreter
    val symbol =
        (calleeReference as? FirResolvedNamedReference)?.resolvedSymbol as? FirCallableSymbol ?: return null
    val argName = Name.identifier("interpreter")
    return symbol.resolvedAnnotationsWithClassIds
        .find { it.fqName(session)?.equals(INTERPRETABLE_FQNAME) ?: false }
        ?.let { annotation ->
            val name = (annotation.findArgumentByName(argName) as FirLiteralExpression).value as String
            name.load<Interpreter<*>>()
        }
}

private object Stdlib {
    private val map: MutableMap<Key, Interpreter<*>> = mutableMapOf()
    init {
        register(Names.TO, Names.PAIR, PairToConstructor())
        register(Names.PAIR_CONSTRUCTOR, Names.PAIR, PairConstructor())
        register(Names.TRIM_MARGIN, StandardClassIds.String, TrimMargin())
        register(Names.TRIM_INDENT, StandardClassIds.String, TrimMargin())
    }

    @OptIn(UnresolvedExpressionTypeAccess::class)
    fun interpreter(call: FirFunctionCall): Interpreter<*>? {
        val id = call.calleeReference.toResolvedFunctionSymbol()?.callableId ?: return null
        val returnType = call.coneTypeOrNull?.classId ?: return null
        return map[Key(id, returnType)]
    }

    fun register(id: CallableId, returnType: ClassId, interpreter: Interpreter<*>) {
        map[Key(id, returnType)] = interpreter
    }
}

private data class Key(
    val id: CallableId,
    val returnType: ClassId,
)

internal fun FirFunctionCall.interpreterName(session: FirSession): String? {
    val symbol =
        (calleeReference as? FirResolvedNamedReference)?.resolvedSymbol as? FirCallableSymbol ?: return null
    val argName = Name.identifier("interpreter")
    return symbol.resolvedAnnotationsWithClassIds
        .find { it.fqName(session)?.equals(INTERPRETABLE_FQNAME) ?: false }
        ?.let { annotation ->
            val name = (annotation.findArgumentByName(argName) as FirLiteralExpression).value as String
            name
        }
}

internal val KotlinTypeFacade.loadInterpreter: FirFunctionCall.() -> Interpreter<*>? get() = { this.loadInterpreter(session) }

internal val FirGetClassCall.classId: ClassId?
    get() {
        return when (val argument = argument) {
            is FirResolvedQualifier -> argument.classId!!
            is FirClassReferenceExpression -> argument.classTypeRef.coneType.classId
            else -> null
        }
    }

internal inline fun <reified T> ClassId.load(): T {
    val constructor = Class.forName(asFqNameString())
        .constructors
        .firstOrNull { constructor -> constructor.parameterCount == 0 }
        ?: error("Interpreter $this must have an empty constructor")

    return constructor.newInstance() as T
}

internal inline fun <reified T> String.load(): T {
    return when (this) {
        "Add" -> Add()
        "From" -> From()
        "Into" -> Into()
        "AddWithDsl" -> AddWithDsl()
        "And10" -> And10()
        "Convert0" -> Convert0()
        "Convert2" -> Convert2()
        "Convert6" -> Convert6()
        "To0" -> To0()
        "With0" -> With0()
        "PerRowCol" -> PerRowCol()
        "Explode0" -> Explode0()
        "Insert0" -> Insert0()
        "Insert1" -> Insert1()
        "Insert2" -> Insert2()
        "Insert3" -> Insert3()
        "Under0" -> Under0()
        "Under1" -> Under1()
        "Under2" -> Under2()
        "Under3" -> Under3()
        "Under4" -> Under4()
        "Join0" -> Join0()
        "Match0" -> Match0()
        "Rename" -> Rename()
        "RenameMapping" -> RenameMapping()
        "Select0" -> Select0()
        "Expr0" -> Expr0()
        "And0" -> And0()
        "Remove0" -> Remove0()
        "Group0" -> Group0()
        "Into0" -> Into0()
        "Ungroup0" -> Ungroup0()
        "DropNulls0" -> DropNulls0()
        "DropNa0" -> DropNa0()
        "Properties0" -> Properties0()
        "Preserve0" -> Preserve0()
        "Preserve1" -> Preserve1()
        "Exclude0" -> Exclude0()
        "Exclude1" -> Exclude1()
        "RenameInto" -> RenameInto()
        "DataFrameGroupBy" -> DataFrameGroupBy()
        "AggregateDslInto" -> AggregateDslInto()
        "GroupByToDataFrame" -> GroupByToDataFrame()
        "GroupByInto" -> GroupByInto()
        "ToDataFrameFrom0" -> ToDataFrameFrom()
        "All0" -> All0()
        "All1" -> All1()
        "All2" -> All2()
        "Cols0" -> Cols0()
        "AllAfter0" -> AllAfter0()
        "AllAfter1" -> AllAfter1()
        "AllAfter2" -> AllAfter2()
        "AllAfter3" -> AllAfter3()
        "AllBefore0" -> AllBefore0()
        "AllBefore1" -> AllBefore1()
        "AllBefore2" -> AllBefore2()
        "AllUpTo0" -> AllUpTo0()
        "AllUpTo1" -> AllUpTo1()
        "AllUpTo2" -> AllUpTo2()
        "AllFrom0" -> AllFrom0()
        "AllFrom1" -> AllFrom1()
        "AllFrom2" -> AllFrom2()
        "ColsOf0" -> ColsOf0()
        "ColsOf1" -> ColsOf1()
        "ColsOf2" -> ColsOf2()
        "ColsAtAnyDepth0" -> ColsAtAnyDepth0()
        "ColsAtAnyDepth1" -> ColsAtAnyDepth1()
        "ColsAtAnyDepth2" -> ColsAtAnyDepth2()
        "FrameCols0" -> FrameCols0()
        "FrameCols1" -> FrameCols1()
        "FrameCols2" -> FrameCols2()
        "ColGroups0" -> ColGroups0()
        "ColGroups1" -> ColGroups1()
        "ColGroups2" -> ColGroups2()
        "NameContains0" -> NameContains0()
        "NameContains1" -> NameContains1()
        "NameContains2" -> NameContains2()
        "NameStartsWith0" -> NameStartsWith0()
        "NameStartsWith1" -> NameStartsWith1()
        "NameStartsWith2" -> NameStartsWith2()
        "NameEndsWith0" -> NameEndsWith0()
        "NameEndsWith" -> NameEndsWith1()
        "NameEndsWith2" -> NameEndsWith2()
        "First0" -> First0()
        "First1" -> First1()
        "First2" -> First2()
        "Single0" -> Single0()
        "Single1" -> Single1()
        "Single2" -> Single2()
        "Last0" -> Last0()
        "Last1" -> Last1()
        "Last2" -> Last2()
        "Take0" -> Take0()
        "Take1" -> Take1()
        "Take2" -> Take2()
        "TakeLast0" -> TakeLast0()
        "TakeLast1" -> TakeLast1()
        "TakeLast2" -> TakeLast2()
        "Drop0" -> Drop0()
        "Drop1" -> Drop1()
        "Drop2" -> Drop2()
        "DropLast0" -> DropLast0()
        "DropLast1" -> DropLast1()
        "DropLast2" -> DropLast2()
        "WithoutNulls0" -> WithoutNulls0()
        "WithoutNulls1" -> WithoutNulls1()
        "WithoutNulls2" -> WithoutNulls2()
        "ValueCols0" -> ValueCols0()
        "ValueCols1" -> ValueCols1()
        "ValueCols2" -> ValueCols2()
        "ColumnRange" -> ColumnRange()
        "Named0" -> Named0()
        "toDataFrameDsl" -> ToDataFrameDsl()
        "toDataFrame" -> ToDataFrame()
        "toDataFrameDefault" -> ToDataFrameDefault()
        "ToDataFrameDslStringInvoke" -> ToDataFrameDslStringInvoke()
        "DataFrameOf0" -> DataFrameOf0()
        "DataFrameBuilderInvoke0" -> DataFrameBuilderInvoke0()
        "ToDataFrameColumn" -> ToDataFrameColumn()
        "FillNulls0" -> FillNulls0()
        "UpdateWith0" -> UpdateWith0()
        "Flatten0" -> Flatten0()
        "FlattenDefault" -> FlattenDefault()
        "AddId" -> AddId()
        "AddDslStringInvoke" -> AddDslStringInvoke()
        "AddDslNamedGroup" -> AddDslNamedGroup()
        "MapToFrame" -> MapToFrame()
        "Move0" -> Move0()
        "ToTop" -> ToTop()
        "Update0" -> Update0()
        "Aggregate" -> Aggregate()
        "DataFrameOf3" -> DataFrameOf3()
        "ValueCounts" -> ValueCounts()
        "RenameToCamelCase" -> RenameToCamelCase()
        "RenameToCamelCaseClause" -> RenameToCamelCaseClause()
        "MoveUnder0" -> MoveUnder0()
        "MoveUnder1" -> MoveUnder1()
        "MoveInto0" -> MoveInto0()
        "MoveToStart0" -> MoveToStart0()
        "MoveToStart1" -> MoveToStart1()
        "MoveToEnd0" -> MoveToEnd0()
        "MoveAfter0" -> MoveAfter0()
        "GroupByAdd" -> GroupByAdd()
        "Merge0" -> Merge0()
        "MergeInto0" -> MergeInto0()
        "MergeId" -> MergeId()
        "MergeBy0" -> MergeBy0()
        "MergeBy1" -> MergeBy1()
        "ReorderColumnsByName" -> ReorderColumnsByName()
        "Reorder" -> Reorder()
        "ByName" -> ByName()
        "GroupByCount0" -> GroupByCount0()
        "GroupByReducePredicate" -> GroupByReducePredicate()
        "GroupByReduceExpression" -> GroupByReduceExpression()
        "GroupByReduceInto" -> GroupByReduceInto()
        "GroupByMaxOf" -> GroupByMaxOf()
        "GroupByMinOf" -> GroupByMinOf()
        "DataFrameXs" -> DataFrameXs()
        "GroupByXs" -> GroupByXs()
        else -> error("$this")
    } as T
}

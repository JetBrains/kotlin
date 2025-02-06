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
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Read0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.ReadCSV0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.ReadDelimStr
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.ReadJson0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.ReadJsonStr
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
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.ColsAtAnyDepth0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.ColsOf0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.ColsOf1
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.DataFrameBuilderInvoke0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.DataFrameOf0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.DataFrameOf3
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.DataRowReadJsonStr
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.DropNa0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.FillNulls0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Flatten0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.FlattenDefault
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.FrameCols0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.GroupByAdd
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.GroupByInto
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.MapToFrame
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Merge0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.MergeId
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.MergeBy0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.MergeBy1
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.MergeInto0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.Move0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.MoveAfter0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.MoveInto0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.MoveToLeft0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.MoveToLeft1
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.MoveToRight0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.MoveUnder0
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.MoveUnder1
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.PairConstructor
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.PairToConstructor
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.PerRowCol
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.ReadExcel
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.RenameMapping
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.StringColumnsConstructor
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
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.ReorderColumnsByName
import org.jetbrains.kotlinx.dataframe.plugin.utils.Names

internal fun FirFunctionCall.loadInterpreter(session: FirSession): Interpreter<*>? {
    val interpreter = Stdlib.interpreter(this)
    if (interpreter != null) return interpreter
    val symbol =
        (calleeReference as? FirResolvedNamedReference)?.resolvedSymbol as? FirCallableSymbol ?: return null
    val argName = Name.identifier("interpreter")
    return symbol.annotations
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
    return symbol.annotations
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
        "Read0" -> Read0()
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
        "ReadJson0" -> ReadJson0()
        "ReadCSV0" -> ReadCSV0()
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
        "ReadJsonStr" -> ReadJsonStr()
        "DataRowReadJsonStr" -> DataRowReadJsonStr()
        "ReadDelimStr" -> ReadDelimStr()
        "GroupByToDataFrame" -> GroupByToDataFrame()
        "GroupByInto" -> GroupByInto()
        "ToDataFrameFrom0" -> ToDataFrameFrom()
        "All0" -> All0()
        "ColsOf0" -> ColsOf0()
        "ColsOf1" -> ColsOf1()
        "ColsAtAnyDepth0" -> ColsAtAnyDepth0()
        "FrameCols0" -> FrameCols0()
        "toDataFrameDsl" -> ToDataFrameDsl()
        "toDataFrame" -> ToDataFrame()
        "toDataFrameDefault" -> ToDataFrameDefault()
        "ToDataFrameDslStringInvoke" -> ToDataFrameDslStringInvoke()
        "DataFrameOf0" -> DataFrameOf0()
        "DataFrameBuilderInvoke0" -> DataFrameBuilderInvoke0()
        "ToDataFrameColumn" -> ToDataFrameColumn()
        "StringColumns" -> StringColumnsConstructor()
        "ReadExcel" -> ReadExcel()
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
        "MoveToLeft0" -> MoveToLeft0()
        "MoveToLeft1" -> MoveToLeft1()
        "MoveToRight0" -> MoveToRight0()
        "MoveAfter0" -> MoveAfter0()
        "GroupByAdd" -> GroupByAdd()
        "Merge0" -> Merge0()
        "MergeInto0" -> MergeInto0()
        "MergeId" -> MergeId()
        "MergeBy0" -> MergeBy0()
        "MergeBy1" -> MergeBy1()
        "ReorderColumnsByName" -> ReorderColumnsByName()
        else -> error("$this")
    } as T
}

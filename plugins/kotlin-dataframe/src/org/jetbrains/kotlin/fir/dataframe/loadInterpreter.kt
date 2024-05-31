/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.dataframe

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.dataframe.utils.Names.INTERPRETABLE_FQNAME
import org.jetbrains.kotlin.fir.dataframe.api.*
import org.jetbrains.kotlin.fir.declarations.findArgumentByName
import org.jetbrains.kotlin.fir.expressions.FirClassReferenceExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirGetClassCall
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.fqName
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlinx.dataframe.KotlinTypeFacade
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.plugin.*

internal fun FirFunctionCall.loadInterpreter(session: FirSession): Interpreter<*>? {
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
        "Select0" -> Select0()
        "Expr0" -> Expr0()
        "And0" -> And0()
        "Remove0" -> Remove0()
        "Group0" -> Group0()
        "Into0" -> Into0()
        "Ungroup0" -> Ungroup0()
        "DropNulls0" -> DropNulls0()
        "Properties0" -> Properties0()
        "Preserve0" -> Preserve0()
        "Preserve1" -> Preserve1()
        "Exclude0" -> Exclude0()
        "Exclude1" -> Exclude1()
        "RenameInto" -> RenameInto()
        "DataFrameGroupBy" -> DataFrameGroupBy()
        "GroupByInto" -> GroupByInto()
        "ReadJsonStr" -> ReadJsonStr()
        "ReadDelimStr" -> ReadDelimStr()
        "GroupByToDataFrame" -> GroupByToDataFrame()
        else -> error("$this")
    } as T
}

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.testUtils

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import kotlin.test.fail


fun KaSession.getClassOrFail(file: KtFile, name: String): KaNamedClassSymbol {
    return getClassOrFail(file.symbol.fileScope, name)
}

fun KaSession.getClassOrFail(scope: KaScope, name: String): KaNamedClassSymbol {
    val allSymbols = scope.classifiers(Name.identifier(name)).toList()
    if (allSymbols.isEmpty()) fail("Missing class '$name'")
    if (allSymbols.size > 1) fail("Found multiple classes with name '$name'")
    val classifier = allSymbols.single()
    if (classifier !is KaNamedClassSymbol) fail("$classifier is not a named class or object")
    return classifier
}

fun KaSession.getFunctionOrFail(file: KtFile, name: String): KaNamedFunctionSymbol {
    return getFunctionOrFail(file.symbol.fileScope, name)
}

fun KaSession.getPropertyOrFail(file: KtFile, name: String): KaPropertySymbol {
    return getPropertyOrFail(file.symbol.fileScope, name)
}

fun KaSession.getFunctionOrFail(scope: KaScope, name: String): KaNamedFunctionSymbol {
    val allSymbols = scope.callables(Name.identifier(name)).toList()
    if (allSymbols.isEmpty()) fail("Missing function '$name'")
    if (allSymbols.size > 1) fail("Found multiple functions with name '$name'")
    val symbol = allSymbols.single()
    if (symbol !is KaNamedFunctionSymbol) fail("$symbol is not a function")
    return symbol
}

fun KaSession.getPropertyOrFail(scope: KaScope, name: String): KaPropertySymbol {
    val allSymbols = scope.callables(Name.identifier(name)).toList()
    if (allSymbols.isEmpty()) fail("Missing property '$name'")
    if (allSymbols.size > 1) fail("Found multiple callables with name '$name'")
    val symbol = allSymbols.single()
    if (symbol !is KaPropertySymbol) fail("$symbol is not a property")
    return symbol
}

fun KaSession.getFunctionOrFail(symbol: KaClassSymbol, name: String): KaNamedFunctionSymbol {
    return getFunctionOrFail(symbol.memberScope, name)
}

context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
fun KaClassSymbol.getPropertyOrFail(name: String): KaPropertySymbol {
    return this.memberScope.getPropertyOrFail(name)
}


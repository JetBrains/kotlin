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


@JvmName("getClassOrFailKaSession")
fun KaSession.getClassOrFail(file: KtFile, name: String): KaNamedClassSymbol {
    return getClassOrFail(file.symbol.fileScope, name)
}

fun KtFile.getClassOrFail(name: String, kaSession: KaSession): KaNamedClassSymbol {
    val file = this
    return getClassOrFail(with(kaSession) { file.symbol.fileScope }, name)
}

fun KtFile.getFunctionOrFail(name: String, kaSession: KaSession): KaNamedFunctionSymbol {
    val file = this
    return getFunctionOrFail(with(kaSession) { file.symbol.fileScope }, name)
}

fun KtFile.getPropertyOrFail(name: String, kaSession: KaSession): KaPropertySymbol {
    val file = this
    return getPropertyOrFail(with(kaSession) { file.symbol.fileScope }, name)
}

@JvmName("getClassOrFailKaScope")
fun KaScope.getClassOrFail(name: String): KaNamedClassSymbol {
    return getClassOrFail(this, name)
}

fun getClassOrFail(scope: KaScope, name: String): KaNamedClassSymbol {
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


@JvmName("getPropertyOrFailKaSession")
fun KaSession.getPropertyOrFail(file: KtFile, name: String): KaPropertySymbol {
    return getPropertyOrFail(file.symbol.fileScope, name)
}

fun KaClassSymbol.getFunctionOrFail(name: String, kaSession: KaSession): KaNamedFunctionSymbol {
    val symbol = this
    return getFunctionOrFail(with(kaSession) { symbol.memberScope }, name)
}

@JvmName("getFunctionOrFailKaScope")
fun KaScope.getFunctionOrFail(name: String): KaNamedFunctionSymbol {
    return getFunctionOrFail(this, name)
}

@JvmName("getPropertyOrFailKaScope")
fun KaScope.getPropertyOrFail(name: String): KaPropertySymbol {
    return getPropertyOrFail(this, name)
}

fun getFunctionOrFail(scope: KaScope, name: String): KaNamedFunctionSymbol {
    val allSymbols = scope.callables(Name.identifier(name)).toList()
    if (allSymbols.isEmpty()) fail("Missing function '$name'")
    if (allSymbols.size > 1) fail("Found multiple functions with name '$name'")
    val symbol = allSymbols.single()
    if (symbol !is KaNamedFunctionSymbol) fail("$symbol is not a function")
    return symbol
}

fun getPropertyOrFail(scope: KaScope, name: String): KaPropertySymbol {
    val allSymbols = scope.callables(Name.identifier(name)).toList()
    if (allSymbols.isEmpty()) fail("Missing property '$name'")
    if (allSymbols.size > 1) fail("Found multiple callables with name '$name'")
    val symbol = allSymbols.single()
    if (symbol !is KaPropertySymbol) fail("$symbol is not a property")
    return symbol
}

@JvmName("getFunctionOrFailKaSession")
fun KaSession.getFunctionOrFail(symbol: KaClassSymbol, name: String): KaNamedFunctionSymbol {
    return getFunctionOrFail(symbol.memberScope, name)
}

fun KaSession.getPropertyOrFail(symbol: KaClassSymbol, name: String): KaPropertySymbol {
    return symbol.memberScope.getPropertyOrFail(name)
}


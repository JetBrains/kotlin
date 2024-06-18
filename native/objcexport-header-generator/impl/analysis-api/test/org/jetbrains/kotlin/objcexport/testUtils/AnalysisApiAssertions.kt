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

context(KaSession)
fun KtFile.getClassOrFail(name: String): KaNamedClassSymbol {
    return symbol.fileScope.getClassOrFail(name)
}

context(KaSession)
fun KaScope.getClassOrFail(name: String): KaNamedClassSymbol {
    val allSymbols = classifiers(Name.identifier(name)).toList()
    if (allSymbols.isEmpty()) fail("Missing class '$name'")
    if (allSymbols.size > 1) fail("Found multiple classes with name '$name'")
    val classifier = allSymbols.single()
    if (classifier !is KaNamedClassSymbol) fail("$classifier is not a named class or object")
    return classifier
}

context(KaSession)
fun KtFile.getFunctionOrFail(name: String): KaNamedFunctionSymbol {
    return symbol.fileScope.getFunctionOrFail(name)
}

context(KaSession)
fun KtFile.getPropertyOrFail(name: String): KaPropertySymbol {
    return symbol.fileScope.getPropertyOrFail(name)
}

context(KaSession)
fun KaScope.getFunctionOrFail(name: String): KaNamedFunctionSymbol {
    val allSymbols = callables(Name.identifier(name)).toList()
    if (allSymbols.isEmpty()) fail("Missing function '$name'")
    if (allSymbols.size > 1) fail("Found multiple functions with name '$name'")
    val symbol = allSymbols.single()
    if (symbol !is KaNamedFunctionSymbol) fail("$symbol is not a function")
    return symbol
}

context(KaSession)
fun KaScope.getPropertyOrFail(name: String): KaPropertySymbol {
    val allSymbols = callables(Name.identifier(name)).toList()
    if (allSymbols.isEmpty()) fail("Missing property '$name'")
    if (allSymbols.size > 1) fail("Found multiple callables with name '$name'")
    val symbol = allSymbols.single()
    if (symbol !is KaPropertySymbol) fail("$symbol is not a property")
    return symbol
}

context(KaSession)
fun KaClassSymbol.getFunctionOrFail(name: String): KaNamedFunctionSymbol {
    return this.memberScope.getFunctionOrFail(name)
}
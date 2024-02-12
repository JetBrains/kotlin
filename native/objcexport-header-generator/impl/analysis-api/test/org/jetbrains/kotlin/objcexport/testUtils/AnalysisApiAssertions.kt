/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.testUtils

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.scopes.KtScope
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import kotlin.test.fail

context(KtAnalysisSession)
fun KtFile.getClassOrFail(name: String): KtNamedClassOrObjectSymbol {
    return getFileSymbol().getFileScope().getClassOrFail(name)
}

context(KtAnalysisSession)
fun KtScope.getClassOrFail(name: String): KtNamedClassOrObjectSymbol {
    val allSymbols = getClassifierSymbols(Name.identifier(name)).toList()
    if (allSymbols.isEmpty()) fail("Missing class '$name'")
    if (allSymbols.size > 1) fail("Found multiple classes with name '$name'")
    val classifier = allSymbols.single()
    if (classifier !is KtNamedClassOrObjectSymbol) fail("$classifier is not a named class or object")
    return classifier
}

context(KtAnalysisSession)
fun KtFile.getFunctionOrFail(name: String): KtFunctionSymbol {
    return getFileSymbol().getFileScope().getFunctionOrFail(name)
}

context(KtAnalysisSession)
fun KtFile.getPropertyOrFail(name: String): KtPropertySymbol {
    return getFileSymbol().getFileScope().getPropertyOrFail(name)
}

context(KtAnalysisSession)
fun KtScope.getFunctionOrFail(name: String): KtFunctionSymbol {
    val allSymbols = getCallableSymbols(Name.identifier(name)).toList()
    if (allSymbols.isEmpty()) fail("Missing function '$name'")
    if (allSymbols.size > 1) fail("Found multiple functions with name '$name'")
    val symbol = allSymbols.single()
    if (symbol !is KtFunctionSymbol) fail("$symbol is not a function")
    return symbol
}

context(KtAnalysisSession)
fun KtScope.getPropertyOrFail(name: String): KtPropertySymbol {
    val allSymbols = getCallableSymbols(Name.identifier(name)).toList()
    if (allSymbols.isEmpty()) fail("Missing property '$name'")
    if (allSymbols.size > 1) fail("Found multiple callables with name '$name'")
    val symbol = allSymbols.single()
    if (symbol !is KtPropertySymbol) fail("$symbol is not a property")
    return symbol
}

context(KtAnalysisSession)
fun KtClassOrObjectSymbol.getFunctionOrFail(name: String): KtFunctionSymbol {
    return this.getMemberScope().getFunctionOrFail(name)
}
/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.symbols

import org.jetbrains.kotlin.name.FqName

abstract class KtVariableLikeSymbol : KtTypedSymbol, KtNamedSymbol, KtSymbolWithKind

abstract class KtEnumEntrySymbol : KtVariableLikeSymbol() {
    final override val symbolKind: KtSymbolKind get() = KtSymbolKind.MEMBER
}

abstract class KtParameterSymbol : KtVariableLikeSymbol()

abstract class KtVariableSymbol : KtVariableLikeSymbol() {
    abstract val isVal: Boolean
}

abstract class KtJavaFieldSymbol : KtVariableSymbol(), KtSymbolWithModality<KtCommonSymbolModality> {
    final override val symbolKind: KtSymbolKind get() = KtSymbolKind.MEMBER
}

// TODO getters & setters
abstract class KtPropertySymbol : KtVariableSymbol(), KtPossibleExtensionSymbol, KtSymbolWithModality<KtCommonSymbolModality> {
    abstract val fqName: FqName
}

abstract class KtLocalVariableSymbol : KtVariableSymbol()

abstract class KtFunctionParameterSymbol : KtParameterSymbol() {
    abstract val isVararg: Boolean
}

abstract class KtConstructorParameterSymbol : KtParameterSymbol(), KtNamedSymbol {
    abstract val constructorParameterKind: KtConstructorParameterSymbolKind
}

enum class KtConstructorParameterSymbolKind {
    VAL_PROPERTY, VAR_PROPERTY, NON_PROPERTY
}
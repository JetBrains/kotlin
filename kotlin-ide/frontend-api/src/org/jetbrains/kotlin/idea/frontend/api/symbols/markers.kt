/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.symbols

import org.jetbrains.kotlin.idea.frontend.api.TypeInfo
import org.jetbrains.kotlin.name.Name

interface KtNamedSymbol : KtSymbol {
    val name: Name
}

interface KtSymbolWithKind : KtSymbol {
    val symbolKind: KtSymbolKind
}

enum class KtSymbolKind {
    TOP_LEVEL, MEMBER, LOCAL
}

interface KtTypedSymbol : KtSymbol {
    val type: TypeInfo
}

interface KtPossibleExtensionSymbol {
    val isExtension: Boolean
    val receiverType: TypeInfo?
}

interface KtSymbolWithTypeParameters {
    val typeParameters: List<KtTypeParameterSymbol>
}

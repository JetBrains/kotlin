/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.symbols

import org.jetbrains.kotlin.name.ClassId


abstract class KtClassLikeSymbol : KtSymbol, KtNamedSymbol, KtSymbolWithKind {
    abstract val classId: ClassId
}

abstract class KtTypeAliasSymbol : KtClassLikeSymbol() {
    final override val symbolKind: KtSymbolKind get() = KtSymbolKind.TOP_LEVEL
}

abstract class KtClassOrObjectSymbol : KtClassLikeSymbol(), KtSymbolWithTypeParameters, KtSymbolWithModality<KtSymbolModality> {
    abstract val classKind: KtClassKind
}

enum class KtClassKind {
    CLASS, ENUM_CLASS, ENUM_ENTRY, ANNOTATION_CLASS, OBJECT, COMPANION_OBJECT, INTERFACE
}
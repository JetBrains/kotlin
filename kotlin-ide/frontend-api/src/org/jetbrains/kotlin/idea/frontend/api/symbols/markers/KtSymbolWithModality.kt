/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.symbols.markers

interface KtSymbolWithModality<M : KtSymbolModality> {
    val modality: M
}

sealed class KtSymbolModality {
    object SEALED : KtSymbolModality()
}

sealed class KtCommonSymbolModality : KtSymbolModality() {
    object FINAL : KtCommonSymbolModality()
    object ABSTRACT : KtCommonSymbolModality()
    object OPEN : KtCommonSymbolModality()
}

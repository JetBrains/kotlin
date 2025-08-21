/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol

/**
 * A callback which is triggered every time a [KaClassLikeSymbol] is encountered
 * during translation.
 */
public fun interface SirKaClassReferenceHandler {
    public fun onClassReference(symbol: KaClassLikeSymbol)
}
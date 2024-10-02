/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.utils

import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolModality

public fun KaSymbolModality.isAbstract(): Boolean = when (this) {
    KaSymbolModality.FINAL, KaSymbolModality.OPEN -> false
    KaSymbolModality.SEALED, KaSymbolModality.ABSTRACT -> true
}

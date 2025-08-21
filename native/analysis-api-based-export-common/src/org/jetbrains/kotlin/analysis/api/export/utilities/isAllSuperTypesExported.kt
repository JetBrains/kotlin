/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.export.utilities

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

context(ka: KaSession)
public fun KaNamedClassSymbol.isAllSuperTypesExported(
    isExported: KaNamedClassSymbol.() -> Boolean,
): Boolean = with(ka) {
    return defaultType.allSupertypes.all { type ->
        val isAny = type.symbol?.classId == ClassId.topLevel(FqName("kotlin.Any"))
        if (isAny) {
            true
        } else {
            (type.symbol as? KaNamedClassSymbol)?.isExported() == true
        }
    }
}

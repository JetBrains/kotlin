/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.utils

import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.name.FqName

/**
 * Ignore currently unsupported packages. See more at KT-76119
 */
internal fun KaClassLikeSymbol.isFromTemporarilyIgnoredPackage(): Boolean {
    val fqName = classId?.asSingleFqName()
    return when {
        fqName?.startsWith(FqName("kotlin.reflect")) == true -> true
        else -> false
    }
}
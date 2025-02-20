/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.export.utilities

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.name.ClassId

public fun KaSession.isThrowable(symbol: KaClassSymbol?): Boolean {
    val classId = symbol?.classId ?: return false
    return isThrowable(classId)
}

public fun KaSession.isThrowable(clazzId: ClassId): Boolean {
    return StandardNames.FqNames.throwable == clazzId.asSingleFqName()
}

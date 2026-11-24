/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.objcexport.getClassIfCategory

context(session: KaSession)
internal fun isTopLevel(symbol: KaCallableSymbol): Boolean =
    symbol.callableId?.classId == null && getClassIfCategory(symbol) == null

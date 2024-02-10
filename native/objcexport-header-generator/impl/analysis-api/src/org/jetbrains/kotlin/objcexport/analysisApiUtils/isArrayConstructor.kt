/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.backend.konan.descriptors.arrayTypes

context(KtAnalysisSession)
internal val KtCallableSymbol.isArrayConstructor: Boolean
    get() = this is KtConstructorSymbol && getContainingSymbol()
        ?.let { containingSymbol -> containingSymbol as? KtClassOrObjectSymbol }
        ?.let { classSymbol -> classSymbol.classIdIfNonLocal?.asFqNameString() in arrayTypes } ?: false

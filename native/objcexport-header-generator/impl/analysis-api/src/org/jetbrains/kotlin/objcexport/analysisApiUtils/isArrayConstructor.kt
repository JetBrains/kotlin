/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.backend.konan.descriptors.arrayTypes

internal fun KaSession.isArrayConstructor(symbol: KaCallableSymbol): Boolean = symbol is KaConstructorSymbol && symbol.containingDeclaration
    ?.let { containingSymbol -> containingSymbol as? KaClassSymbol }
    ?.let { classSymbol -> classSymbol.classId?.asFqNameString() in arrayTypes } ?: false

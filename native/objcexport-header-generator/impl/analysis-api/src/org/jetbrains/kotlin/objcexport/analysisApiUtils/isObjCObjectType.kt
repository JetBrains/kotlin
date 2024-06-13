/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.name.NativeStandardInteropNames

context(KtAnalysisSession)
internal fun KtType.isObjCObjectType(): Boolean {
    val symbol = this.symbol

    if (symbol != null) {
        if (symbol.classId == NativeStandardInteropNames.objCObjectClassId) {
            return true
        }

        if (symbol is KaClassOrObjectSymbol) {
            return symbol.superTypes.any { it.isObjCObjectType() }
        }
    }

    return false
}

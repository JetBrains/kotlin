/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isCompanion

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslator.needCompanionObjectProperty]
 */
internal fun KaSession.getNeedsCompanionProperty(symbol: KaClassSymbol): Boolean {
    return symbol.staticMemberScope.classifiers
        .any { (it as? KaClassSymbol)?.isCompanion == true }
}

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.export.utilities.isCompanion
import org.jetbrains.kotlin.analysis.api.scopes.staticMemberScope
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassifierSymbol
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isVisibleInObjC

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslator.needCompanionObjectProperty]
 */
context(session: KaSession)
internal fun hasCompanionObject(symbol: KaClassSymbol): Boolean = getCompanion(symbol) != null

context(session: KaSession)
internal fun hasVisibleCompanionObject(symbol: KaClassSymbol): Boolean =
    getCompanion(symbol) != null && isVisibleInObjC(getCompanion(symbol))

context(session: KaSession)
internal fun getCompanion(symbol: KaClassSymbol): KaClassifierSymbol? =
    symbol.staticMemberScope.classifiers.toList().firstOrNull { (it as? KaClassSymbol)?.isCompanion == true }

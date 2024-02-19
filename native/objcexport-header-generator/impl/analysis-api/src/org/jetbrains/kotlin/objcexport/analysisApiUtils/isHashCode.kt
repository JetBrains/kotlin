/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

private val hashCodeCallableId = CallableId(StandardClassIds.Any, Name.identifier("hashCode"))

context(KtAnalysisSession)
internal val KtCallableSymbol.isHashCode: Boolean
    get() = this.callableIdIfNonLocal == hashCodeCallableId ||
        getAllOverriddenSymbols().any { overriddenSymbol -> overriddenSymbol.callableIdIfNonLocal == hashCodeCallableId }

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.extensions

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.sir.lightclasses.SirFromKtSymbol

internal inline fun <R> SirFromKtSymbol.lazyWithSessions(
    crossinline block: context(SirSession, KtAnalysisSession) () -> R
): Lazy<R> {
    return lazy { block(sirSession, analysisApiSession) }
}

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.sir.SirDeclarationParent
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.utils.withSirAnalyse

internal interface SirFromKtSymbol {
    val ktSymbol: KtDeclarationSymbol
    val analysisApiSession: KtAnalysisSession
    val sirSession: SirSession

//    var parent: SirDeclarationParent
//        get() = withSirAnalyse(sirSession, analysisApiSession) {
//            ktSymbol.getSirParent()
//        }
//        set(value) {
//            // do nothing. It is impossible to change this parent from outside
//        }
}
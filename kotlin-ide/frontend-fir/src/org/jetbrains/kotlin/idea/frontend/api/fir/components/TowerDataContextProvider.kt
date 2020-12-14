/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.components

import org.jetbrains.kotlin.fir.resolve.FirTowerDataContext
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSessionContext
import org.jetbrains.kotlin.psi.KtElement

internal class TowerDataContextProvider(private val analysisSession: KtFirAnalysisSession) {
    fun getTowerDataContext(statement: KtElement): FirTowerDataContext {
        val fakeContext =  analysisSession.context as? KtFirAnalysisSessionContext.FakeFileContext
            ?: error("Getting data context for non context-dependent session is not supported yet")
        return fakeContext.completionContext.getTowerDataContext(statement)
    }
}
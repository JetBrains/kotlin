/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.scopes.KtScope
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithVisibility
import org.jetbrains.kotlin.sir.SirDeclaration
import org.jetbrains.kotlin.sir.SirVisibility
import org.jetbrains.kotlin.sir.providers.SirChildrenProvider
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.utils.withSirAnalyse

public class SirDeclarationChildrenProviderImpl(
    private val ktAnalysisSession: KtAnalysisSession,
    private val sirSession: SirSession,
) : SirChildrenProvider {
    override fun KtScope.extractDeclarations(): Sequence<SirDeclaration> = withSirAnalyse(sirSession, ktAnalysisSession) {
        getAllSymbols()
            .filter { (it as? KtSymbolWithVisibility)?.sirVisibility() == SirVisibility.PUBLIC }
            .map { it.sirDeclaration() }
    }

}

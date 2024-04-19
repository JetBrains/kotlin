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

public class SirDeclarationChildrenProviderImpl(private val sirSession: SirSession) : SirChildrenProvider {

    override fun KtScope.extractDeclarations(ktAnalysisSession: KtAnalysisSession): Sequence<SirDeclaration> =
        getAllSymbols()
            .filter {
                with(sirSession) { (it as? KtSymbolWithVisibility)?.sirVisibility(ktAnalysisSession) == SirVisibility.PUBLIC }
            }
            .map { with(sirSession) { it.sirDeclaration() } }

}

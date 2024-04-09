/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.session

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.sir.providers.*
import org.jetbrains.kotlin.sir.providers.impl.*
import org.jetbrains.sir.lightclasses.SirDeclarationFromKtSymbolProvider

internal class StandaloneSirSession(
    ktAnalysisSession: KtAnalysisSession,
    override val bridgeModuleName: String,
) : SirSession {

    override val declarationNamer = SirDeclarationNamerImpl()
    override val enumGenerator = SirEnumGeneratorImpl()
    override val moduleProvider = SirModuleProviderImpl(
        ktAnalysisSession = ktAnalysisSession,
        sirSession = sirSession,
    )
    override val declarationProvider = CachingSirDeclarationProvider(
        declarationsProvider = SirDeclarationFromKtSymbolProvider(
            ktAnalysisSession = ktAnalysisSession,
            sirSession = sirSession,
        )
    )
    override val parentProvider = SirParentProviderImpl(
        ktAnalysisSession = ktAnalysisSession,
        sirSession = sirSession,
    )
    override val typeProvider = SirTypeProviderImpl(
        ktAnalysisSession = ktAnalysisSession,
        sirSession = sirSession,
    )
    override val visibilityChecker = SirVisibilityCheckerImpl(
        ktAnalysisSession = ktAnalysisSession,
        sirSession = sirSession,
    )
    override val childrenProvider = SirDeclarationChildrenProviderImpl(
        ktAnalysisSession = ktAnalysisSession,
        sirSession = sirSession,
    )
}

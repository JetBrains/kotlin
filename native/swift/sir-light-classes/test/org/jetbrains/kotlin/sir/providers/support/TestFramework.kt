/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.support

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.sir.SirDeclaration
import org.jetbrains.kotlin.sir.builder.buildModule
import org.jetbrains.kotlin.sir.providers.SirBridgeProvider
import org.jetbrains.kotlin.sir.providers.SirChildrenProvider
import org.jetbrains.kotlin.sir.providers.SirDeclarationNamer
import org.jetbrains.kotlin.sir.providers.SirDeclarationProvider
import org.jetbrains.kotlin.sir.providers.SirEnumGenerator
import org.jetbrains.kotlin.sir.providers.SirModuleProvider
import org.jetbrains.kotlin.sir.providers.SirParentProvider
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.SirTrampolineDeclarationsProvider
import org.jetbrains.kotlin.sir.providers.SirTypeNamer
import org.jetbrains.kotlin.sir.providers.SirTypeProvider
import org.jetbrains.kotlin.sir.providers.SirVisibilityChecker
import org.jetbrains.kotlin.sir.providers.impl.*
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.SirBridgeProviderImpl
import org.jetbrains.kotlin.sir.providers.utils.SilentUnsupportedDeclarationReporter
import org.jetbrains.sir.lightclasses.SirDeclarationFromKtSymbolProvider

class TestSirSession(
    kaModule: KaModule,
    referencedTypeHandler: SirKaClassReferenceHandler? = null,
) : SirSession {
    override val useSiteModule: KaModule = kaModule
    override val declarationNamer: SirDeclarationNamer = SirDeclarationNamerImpl()
    override val moduleProvider: SirModuleProvider = SirOneToOneModuleProvider(emptyList())
    override val declarationProvider: SirDeclarationProvider = CachingSirDeclarationProvider(
        declarationsProvider = ObservingSirDeclarationProvider(
            declarationsProvider = SirDeclarationFromKtSymbolProvider(
                sirSession = sirSession,
            ),
            kaClassReferenceHandler = referencedTypeHandler
        )
    )
    override val enumGenerator: SirEnumGenerator = SirEnumGeneratorImpl(buildModule { name = "Packages" })
    override val parentProvider: SirParentProvider = SirParentProviderImpl(
        sirSession = sirSession,
        packageEnumGenerator = enumGenerator,
    )
    override val typeProvider: SirTypeProvider = SirTypeProviderImpl(
        errorTypeStrategy = SirTypeProvider.ErrorTypeStrategy.ErrorType,
        unsupportedTypeStrategy = SirTypeProvider.ErrorTypeStrategy.ErrorType,
        sirSession = sirSession,
    )
    override val visibilityChecker: SirVisibilityChecker = SirVisibilityCheckerImpl(
        sirSession = sirSession,
        unsupportedDeclarationReporter = SilentUnsupportedDeclarationReporter,
        enableCoroutinesSupport = true,
    )
    override val childrenProvider: SirChildrenProvider = SirDeclarationChildrenProviderImpl(
        sirSession = sirSession,
    )

    override val trampolineDeclarationsProvider: SirTrampolineDeclarationsProvider =
        SirTrampolineDeclarationsProviderImpl(sirSession, null)

    override val bridgeProvider: SirBridgeProvider
        get() = SirBridgeProviderImpl(this, SirTypeNamer())
}

inline fun <R> translate(
    file: KtFile,
    sirSessionBuilder: (KaModule) -> SirSession = { TestSirSession(it) },
    action: (List<SirDeclaration>) -> R
) {
    analyze(file) {
        with(sirSessionBuilder(useSiteModule)) {
            action(file.symbol.fileScope.extractDeclarations().toList())
        }
    }
}

inline fun <R> withAnalysisSession(
    file: KtFile,
    action: KaSession.(KtFile) -> R
) {
    analyze(file) {
        action(file)
    }
}
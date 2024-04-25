/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.session

import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.sir.providers.*
import org.jetbrains.kotlin.sir.providers.impl.*
import org.jetbrains.sir.lightclasses.SirDeclarationFromKtSymbolProvider

internal class StandaloneSirSession(
    unsupportedTypeStrategy: UnsupportedTypeStrategy,
    unknownTypeStrategy: UnknownTypeStrategy,
    useSiteModule: KtModule,
    moduleProviderBuilder: () -> SirModuleProvider
) : SirSession {

    override val declarationNamer = SirDeclarationNamerImpl()
    override val enumGenerator = SirEnumGeneratorImpl()
    override val moduleProvider = moduleProviderBuilder()
    override val declarationProvider = CachingSirDeclarationProvider(
        declarationsProvider = SirDeclarationFromKtSymbolProvider(
            ktModule = useSiteModule,
            sirSession = sirSession,
        )
    )
    override val parentProvider = SirParentProviderImpl(sirSession)
    override val typeProvider = SirTypeProviderImpl(unsupportedTypeStrategy, unknownTypeStrategy, sirSession)
    override val visibilityChecker = SirVisibilityCheckerImpl()
    override val childrenProvider = SirDeclarationChildrenProviderImpl(sirSession)
}

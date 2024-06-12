/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.playground

import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.SirTrampolineDeclarationsProvider
import org.jetbrains.kotlin.sir.providers.SirTypeProvider
import org.jetbrains.kotlin.sir.providers.impl.*
import org.jetbrains.kotlin.sir.providers.utils.UnsupportedDeclarationReporter
import org.jetbrains.sir.lightclasses.SirDeclarationFromKtSymbolProvider

internal class PlaygroundSirSession(
    ktModule: KaModule,
    moduleForPackageEnums: SirModule,
    unsupportedDeclarationReporter: UnsupportedDeclarationReporter,
    targetPackageFqName: FqName?,
) : SirSession {
    override val declarationNamer = SirDeclarationNamerImpl()
    override val moduleProvider = SirSingleModuleProvider("Playground")
    override val declarationProvider = CachingSirDeclarationProvider(
        declarationsProvider = SirDeclarationFromKtSymbolProvider(
            ktModule = ktModule,
            sirSession = sirSession,
        )
    )
    private val enumGenerator = SirEnumGeneratorImpl(moduleForPackageEnums)
    override val parentProvider = SirParentProviderImpl(
        sirSession = sirSession,
        packageEnumGenerator = enumGenerator,
    )
    override val typeProvider = SirTypeProviderImpl(
        errorTypeStrategy = SirTypeProvider.ErrorTypeStrategy.ErrorType,
        unsupportedTypeStrategy = SirTypeProvider.ErrorTypeStrategy.ErrorType,
        sirSession = sirSession,
    )
    override val visibilityChecker = SirVisibilityCheckerImpl(unsupportedDeclarationReporter)
    override val childrenProvider = SirDeclarationChildrenProviderImpl(
        sirSession = sirSession,
    )

    override val trampolineDeclarationsProvider: SirTrampolineDeclarationsProvider =
        SirTrampolineDeclarationsProviderImpl(sirSession, targetPackageFqName, enumGenerator)
}
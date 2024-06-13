/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.session

import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.sir.providers.SirModuleProvider
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.SirTypeProvider
import org.jetbrains.kotlin.sir.providers.impl.*
import org.jetbrains.kotlin.sir.providers.utils.UnsupportedDeclarationReporter
import org.jetbrains.sir.lightclasses.SirDeclarationFromKtSymbolProvider

internal class StandaloneSirSession(
    useSiteModule: KtModule,
    moduleToTranslate: KtModule,
    override val errorTypeStrategy: SirTypeProvider.ErrorTypeStrategy,
    override val unsupportedTypeStrategy: SirTypeProvider.ErrorTypeStrategy,
    moduleForPackageEnums: SirModule,
    unsupportedDeclarationReporter: UnsupportedDeclarationReporter,
    moduleProviderBuilder: () -> SirModuleProvider,
    val targetPackageFqName: FqName? = null,
) : SirSession {

    override val declarationNamer = SirDeclarationNamerImpl()

    override val moduleProvider = moduleProviderBuilder()

    override val declarationProvider = CachingSirDeclarationProvider(
        declarationsProvider = SirDeclarationFromKtSymbolProvider(
            ktModule = useSiteModule,
            sirSession = sirSession,
        )
    )

    private val enumGenerator = targetPackageFqName?.let {
        PackageFlatteningSirEnumGenerator(
            sirSession = this,
            enumGenerator = SirEnumGeneratorImpl(moduleForPackageEnums),
            moduleForTrampolines = moduleToTranslate.sirModule(),
        )
    } ?: SirEnumGeneratorImpl(moduleForPackageEnums)

    override val parentProvider = SirParentProviderImpl(sirSession, enumGenerator)

    override val trampolineDeclarationsProvider = SirTrampolineDeclarationsProviderImpl(sirSession, targetPackageFqName, enumGenerator)

    override val typeProvider = SirTypeProviderImpl(
        sirSession,
        errorTypeStrategy = errorTypeStrategy,
        unsupportedTypeStrategy = unsupportedTypeStrategy
    )
    override val visibilityChecker = SirVisibilityCheckerImpl(unsupportedDeclarationReporter)
    override val childrenProvider = SirDeclarationChildrenProviderImpl(sirSession)
}

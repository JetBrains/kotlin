/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.ide.session

import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.sir.providers.*
import org.jetbrains.kotlin.sir.providers.impl.*
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.SirBridgeProviderImpl
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.SirCustomTypeTranslatorImpl
import org.jetbrains.kotlin.sir.providers.utils.UnsupportedDeclarationReporter
import org.jetbrains.sir.lightclasses.SirDeclarationFromKtSymbolProvider
import org.jetbrains.sir.lightclasses.StubbingSirDeclarationProvider

public class IdeSirSession(
    kaModule: KaModule,
    moduleForPackageEnums: SirModule,
    platformLibs: Collection<KaLibraryModule>,
    unsupportedDeclarationReporter: UnsupportedDeclarationReporter,
    targetPackageFqName: FqName?,
) : SirSession {
    override val useSiteModule: KaModule = kaModule

    override val declarationNamer: SirDeclarationNamer = SirDeclarationNamerImpl()
    override val moduleProvider: SirModuleProvider = SirOneToOneModuleProvider(
        platformLibs = platformLibs,
    )
    override val declarationProvider: SirDeclarationProvider = CachingSirDeclarationProvider(
        declarationsProvider = StubbingSirDeclarationProvider(
            sirSession = sirSession,
            declarationsProvider = SirDeclarationFromKtSymbolProvider(
                sirSession = sirSession,
            )
        )
    )
    override val enumGenerator: SirEnumGenerator = SirEnumGeneratorImpl(moduleForPackageEnums)
    override val parentProvider: SirParentProvider = SirParentProviderImpl(
        sirSession = sirSession,
        packageEnumGenerator = enumGenerator,
    )
    override val typeProvider: SirTypeProvider = SirTypeProviderImpl(
        errorTypeStrategy = SirTypeProvider.ErrorTypeStrategy.ErrorType,
        unsupportedTypeStrategy = SirTypeProvider.ErrorTypeStrategy.ErrorType,
        sirSession = sirSession,
    )
    override val customTypeTranslator: SirCustomTypeTranslator = SirCustomTypeTranslatorImpl(sirSession)
    override val visibilityChecker: SirVisibilityChecker = SirVisibilityCheckerImpl(
        sirSession = sirSession,
        unsupportedDeclarationReporter = unsupportedDeclarationReporter,
        enableCoroutinesSupport = false,
    )
    override val childrenProvider: SirChildrenProvider = SirDeclarationChildrenProviderImpl(
        sirSession = sirSession,
    )

    override val trampolineDeclarationsProvider: SirTrampolineDeclarationsProvider =
        SirTrampolineDeclarationsProviderImpl(sirSession, targetPackageFqName)

    override val bridgeProvider: SirBridgeProvider = SirBridgeProviderImpl(this, SirTypeNamer())
}

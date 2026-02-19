/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.session

import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.sir.providers.SirBridgeProvider
import org.jetbrains.kotlin.sir.providers.SirCustomTypeTranslator
import org.jetbrains.kotlin.sir.providers.SirEnumGenerator
import org.jetbrains.kotlin.sir.providers.SirModuleProvider
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.SirTypeNamer
import org.jetbrains.kotlin.sir.providers.SirTypeProvider
import org.jetbrains.kotlin.sir.providers.impl.*
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.SirBridgeProviderImpl
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.SirCustomTypeTranslatorImpl
import org.jetbrains.kotlin.sir.providers.utils.UnsupportedDeclarationReporter
import org.jetbrains.sir.lightclasses.SirDeclarationFromKtSymbolProvider
import org.jetbrains.sir.lightclasses.StubbingSirDeclarationProvider

internal class StandaloneSirSession(
    override val useSiteModule: KaModule,
    override val moduleToTranslate: KaModule,
    override val errorTypeStrategy: SirTypeProvider.ErrorTypeStrategy,
    override val unsupportedTypeStrategy: SirTypeProvider.ErrorTypeStrategy,
    moduleForPackageEnums: SirModule,
    unsupportedDeclarationReporter: UnsupportedDeclarationReporter,
    enableCoroutinesSupport: Boolean,
    override val moduleProvider: SirModuleProvider,
    val targetPackageFqName: FqName? = null,
    val referencedTypeHandler: SirKaClassReferenceHandler? = null,
) : SirSession {

    override val declarationNamer = SirDeclarationNamerImpl()

    override val declarationProvider = CachingSirDeclarationProvider(
        declarationsProvider = ObservingSirDeclarationProvider(
            declarationsProvider = StubbingSirDeclarationProvider(
                sirSession = sirSession,
                declarationsProvider = SirDeclarationFromKtSymbolProvider(
                    sirSession = sirSession,
                )
            ),
            kaClassReferenceHandler = referencedTypeHandler
        )
    )

    override val enumGenerator: SirEnumGenerator = targetPackageFqName?.let {
        PackageFlatteningSirEnumGenerator(
            sirSession = this,
            enumGenerator = SirEnumGeneratorImpl(moduleForPackageEnums),
            moduleForTrampolines = moduleToTranslate.sirModule(),
        )
    } ?: SirEnumGeneratorImpl(moduleForPackageEnums)

    override val parentProvider = SirParentProviderImpl(sirSession, enumGenerator)

    override val trampolineDeclarationsProvider = SirTrampolineDeclarationsProviderImpl(sirSession, targetPackageFqName)

    override val typeProvider = SirTypeProviderImpl(
        sirSession,
        errorTypeStrategy = errorTypeStrategy,
        unsupportedTypeStrategy = unsupportedTypeStrategy
    )
    override val customTypeTranslator: SirCustomTypeTranslator = SirCustomTypeTranslatorImpl(sirSession)
    override val visibilityChecker = SirVisibilityCheckerImpl(
        sirSession,
        unsupportedDeclarationReporter = unsupportedDeclarationReporter,
        enableCoroutinesSupport = enableCoroutinesSupport,
    )
    override val childrenProvider = SirDeclarationChildrenProviderImpl(sirSession)

    override val bridgeProvider: SirBridgeProvider
        get() = SirBridgeProviderImpl(this, SirTypeNamer())
}

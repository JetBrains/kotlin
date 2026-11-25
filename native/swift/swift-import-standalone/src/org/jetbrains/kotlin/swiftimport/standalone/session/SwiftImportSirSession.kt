/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftimport.standalone.session

import org.jetbrains.kotlin.analysis.api.klib.reader.KaModules
import org.jetbrains.kotlin.analysis.api.klib.reader.createKaModulesForStandaloneAnalysis
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.library.metadata.KlibInputModule
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.sir.providers.SirBridgeProvider
import org.jetbrains.kotlin.sir.providers.SirCustomTypeTranslator
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
import org.jetbrains.kotlin.sir.providers.SirChildrenProvider
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.SirBridgeProviderImpl
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.SirCustomTypeTranslatorImpl

/**
 * A [SirSession] that only knows how to generate bridges.
 *
 * Swift import has no Kotlin sources, but Swift Export's bridge generation ([SirBridgeProviderImpl]) is built on top of [SirSession],
 * which needs a Kotlin module to run Analysis API in. The module is a fake empty one, exactly like the one Swift Export
 * uses as a use-site module. All other providers are not needed to generate bridges, and fail if requested.
 *
 * @property sirModule The module that is being imported. It is also what [moduleProvider] returns for any Kotlin module.
 */
internal class SwiftImportSirSession(
    private val kaModules: KaModules<Unit>,
    private val sirModule: SirModule,
) : SirSession, AutoCloseable {
    override val useSiteModule: KaModule get() = kaModules.useSiteModule
    override val moduleToTranslate: KaModule get() = kaModules.useSiteModule

    override val moduleProvider: SirModuleProvider = object : SirModuleProvider {
        override fun KaModule.sirModule(): SirModule = sirModule
    }

    override val customTypeTranslator: SirCustomTypeTranslator = SirCustomTypeTranslatorImpl(this)

    override val bridgeProvider: SirBridgeProvider by lazy { SirBridgeProviderImpl(this, SirTypeNamer()) }

    override val enumGenerator: SirEnumGenerator get() = unsupported("enumGenerator")
    override val declarationNamer: SirDeclarationNamer get() = unsupported("declarationNamer")
    override val declarationProvider: SirDeclarationProvider get() = unsupported("declarationProvider")
    override val parentProvider: SirParentProvider get() = unsupported("parentProvider")
    override val trampolineDeclarationsProvider: SirTrampolineDeclarationsProvider get() = unsupported("trampolineDeclarationsProvider")
    override val typeProvider: SirTypeProvider get() = unsupported("typeProvider")
    override val visibilityChecker: SirVisibilityChecker get() = unsupported("visibilityChecker")
    override val childrenProvider: SirChildrenProvider get() = unsupported("childrenProvider")

    /**
     * Disposes the underlying Analysis API project. The session must not be used afterwards.
     */
    override fun close() = kaModules.close()

    private fun unsupported(provider: String): Nothing =
        error("$provider is not available in Swift import session: it is only used to generate bridges")

    companion object {
        fun create(sirModule: SirModule): SwiftImportSirSession = SwiftImportSirSession(
            kaModules = createKaModulesForStandaloneAnalysis(
                inputs = emptyList<KlibInputModule<Unit>>(),
                targetPlatform = NativePlatforms.unspecifiedNativePlatform,
            ),
            sirModule = sirModule,
        )
    }
}

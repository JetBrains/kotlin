/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.builders

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.sir.kt.*
import org.jetbrains.kotlin.swiftexport.standalone.*
import org.jetbrains.kotlin.swiftexport.standalone.SirDeclarationLayoutStrategy
import org.jetbrains.kotlin.swiftexport.standalone.SirDeclarationNamerImpl
import org.jetbrains.kotlin.swiftexport.standalone.SirDeclarationProviderImpl
import org.jetbrains.kotlin.swiftexport.standalone.SirEnumGeneratorImpl
import org.jetbrains.kotlin.swiftexport.standalone.SirModuleProviderImpl
import org.jetbrains.kotlin.swiftexport.standalone.SirParentProviderImpl
import org.jetbrains.kotlin.swiftexport.standalone.SirTypeProviderImpl
import org.jetbrains.kotlin.swiftexport.standalone.SirVisibilityCheckerImpl

internal class StandaloneSirSession(
    private val ktAnalysisSession: KtAnalysisSession,
    private val layoutStrategy: SirDeclarationLayoutStrategy,
    predefinedModules: Map<KtModule, SirModule> = emptyMap(),
) : SirSession {
    override val declarationNamer: SirDeclarationNamer = SirDeclarationNamerImpl(layoutStrategy, ktAnalysisSession, sirSession)
    override val parentProvider: SirParentProvider = SirParentProviderImpl(layoutStrategy, ktAnalysisSession, sirSession)
    override val declarationProvider: SirDeclarationProvider = SirDeclarationProviderImpl(ktAnalysisSession, sirSession)
    override val moduleProvider: SirModuleProvider = SirModuleProviderImpl(predefinedModules)
    override val enumGenerator: SirEnumGenerator = SirEnumGeneratorImpl()
    override val typeProvider: SirTypeProvider = SirTypeProviderImpl(ktAnalysisSession, sirSession)
    override val visibilityChecker: SirVisibilityChecker = SirVisibilityCheckerImpl(ktAnalysisSession)
}
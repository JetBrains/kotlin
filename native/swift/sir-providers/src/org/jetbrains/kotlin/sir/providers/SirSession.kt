/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.scopes.KtScope
import org.jetbrains.kotlin.analysis.api.symbols.KtDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithVisibility
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.sir.*


/**
 * A single entry point for all facilities that are required for running Analysis API -> SIR translation.
 *
 * Similar classes:
 * 1. [KtAnalysisSession][org.jetbrains.kotlin.analysis.api.KtAnalysisSession] from Analysis API.
 * 2. [FirSession][org.jetbrains.kotlin.fir.FirSession] from K2.
 */
public interface SirSession :
    SirDeclarationNamer,
    SirDeclarationProvider,
    SirParentProvider,
    SirModuleProvider,
    SirEnumGenerator,
    SirTypeProvider,
    SirVisibilityChecker,
    SirChildrenProvider
{
    public val sirSession: SirSession
        get() = this

    public val declarationNamer: SirDeclarationNamer
    public val declarationProvider: SirDeclarationProvider
    public val parentProvider: SirParentProvider
    public val moduleProvider: SirModuleProvider
    public val enumGenerator: SirEnumGenerator
    public val typeProvider: SirTypeProvider
    public val visibilityChecker: SirVisibilityChecker
    public val childrenProvider: SirChildrenProvider

    override val errorTypeStrategy: SirTypeProvider.ErrorTypeStrategy
        get() = typeProvider.errorTypeStrategy
    override val unsupportedTypeStrategy: SirTypeProvider.ErrorTypeStrategy
        get() = typeProvider.unsupportedTypeStrategy

    override fun FqName.sirPackageEnum(module: SirModule): SirEnum = with(enumGenerator) { this@sirPackageEnum.sirPackageEnum(module) }

    override fun KtDeclarationSymbol.sirDeclarationName(): String = with(declarationNamer) { this@sirDeclarationName.sirDeclarationName() }

    override fun KtDeclarationSymbol.sirDeclaration(): SirDeclaration = with(declarationProvider) { this@sirDeclaration.sirDeclaration() }

    override fun KtDeclarationSymbol.getSirParent(ktAnalysisSession: KtAnalysisSession): SirDeclarationParent =
        with(parentProvider) { this@getSirParent.getSirParent(ktAnalysisSession) }

    override fun KtModule.sirModule(): SirModule = with(moduleProvider) { this@sirModule.sirModule() }

    override fun KtType.translateType(
        ktAnalysisSession: KtAnalysisSession,
        reportErrorType: (String) -> Nothing,
        reportUnsupportedType: () -> Nothing,
        processTypeImports: (List<SirImport>) -> Unit,
    ): SirType =
        with(typeProvider) { this@translateType.translateType(ktAnalysisSession, reportErrorType, reportUnsupportedType, processTypeImports) }

    override fun KtSymbolWithVisibility.sirVisibility(ktAnalysisSession: KtAnalysisSession): SirVisibility? =
        with(visibilityChecker) { this@sirVisibility.sirVisibility(ktAnalysisSession) }

    override fun KtScope.extractDeclarations(ktAnalysisSession: KtAnalysisSession): Sequence<SirDeclaration> =
        with(childrenProvider) { this@extractDeclarations.extractDeclarations(ktAnalysisSession) }
}

/**
 * Provides methods to create [SirEnum] which emulates Kotlin packages.
 */
public interface SirEnumGenerator {
    public fun FqName.sirPackageEnum(module: SirModule): SirEnum
}

/**
 * Names SIR declarations that are constructed from [KtSymbol].
 */
public interface SirDeclarationNamer {
    public fun KtDeclarationSymbol.sirDeclarationName(): String
}


/**
 * A single entry point to create a lazy wrapper around the given [KtSymbol].
 */
public interface SirDeclarationProvider {
    public fun KtDeclarationSymbol.sirDeclaration(): SirDeclaration
}

/**
 * Given [KtDeclarationSymbol] will produce [SirDeclarationParent], representing the parent for corresponding sir node.
 *
 * For example, given the top level function without a package - will return SirModule that should declare that declarations.
 * Or, given the top level function with a package - will return SirExtension for that package.
 *
 */
public interface SirParentProvider {
    public fun KtDeclarationSymbol.getSirParent(ktAnalysisSession: KtAnalysisSession): SirDeclarationParent
}

/**
 * Translates the given [KtModule] to the corresponding [SirModule].
 * Note that it is not always a 1-1 mapping.
 */
public interface SirModuleProvider {

    public fun KtModule.sirModule(): SirModule
}

public interface SirChildrenProvider {
    public fun KtScope.extractDeclarations(ktAnalysisSession: KtAnalysisSession): Sequence<SirDeclaration>
}

public interface SirTypeProvider {

    public val errorTypeStrategy: ErrorTypeStrategy
    public val unsupportedTypeStrategy: ErrorTypeStrategy

    public enum class ErrorTypeStrategy {
        Fail, ErrorType
    }

    /**
     * Translates the given [KtType] to [SirType].
     * Calls [reportErrorType] / [reportUnsupportedType] if error/unsupported type
     * is encountered and [errorTypeStrategy] / [unsupportedTypeStrategy] instructs to fail.
     *
     * [processTypeImports] is called with the imports required to use the resulting type properly.
     */
    public fun KtType.translateType(
        ktAnalysisSession: KtAnalysisSession,
        reportErrorType: (String) -> Nothing,
        reportUnsupportedType: () -> Nothing,
        processTypeImports: (List<SirImport>) -> Unit,
    ): SirType
}

public interface SirVisibilityChecker {
    /**
     * Determines visibility of the given [KtSymbolWithVisibility].
     * @return null if symbol should not be exposed to SIR completely.
     */
    public fun KtSymbolWithVisibility.sirVisibility(ktAnalysisSession: KtAnalysisSession): SirVisibility?
}

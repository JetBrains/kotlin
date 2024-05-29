/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaSymbolWithVisibility
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.sir.*


/**
 * A single entry point for all facilities that are required for running Analysis API -> SIR translation.
 *
 * Similar classes:
 * 1. [KaSession][org.jetbrains.kotlin.analysis.api.KaSession] from Analysis API.
 * 2. [FirSession][org.jetbrains.kotlin.fir.FirSession] from K2.
 */
public interface SirSession :
    SirDeclarationNamer,
    SirDeclarationProvider,
    SirParentProvider,
    SirTrampolineDeclarationsProvider,
    SirModuleProvider,
    SirTypeProvider,
    SirVisibilityChecker,
    SirChildrenProvider
{
    public val sirSession: SirSession
        get() = this

    public val declarationNamer: SirDeclarationNamer
    public val declarationProvider: SirDeclarationProvider
    public val parentProvider: SirParentProvider
    public val trampolineDeclarationsProvider: SirTrampolineDeclarationsProvider
    public val moduleProvider: SirModuleProvider
    public val typeProvider: SirTypeProvider
    public val visibilityChecker: SirVisibilityChecker
    public val childrenProvider: SirChildrenProvider

    override val errorTypeStrategy: SirTypeProvider.ErrorTypeStrategy
        get() = typeProvider.errorTypeStrategy
    override val unsupportedTypeStrategy: SirTypeProvider.ErrorTypeStrategy
        get() = typeProvider.unsupportedTypeStrategy

    override fun KaDeclarationSymbol.sirDeclarationName(): String = with(declarationNamer) { this@sirDeclarationName.sirDeclarationName() }

    override fun KaDeclarationSymbol.sirDeclaration(): SirDeclaration = with(declarationProvider) { this@sirDeclaration.sirDeclaration() }

    override fun KaDeclarationSymbol.getSirParent(ktAnalysisSession: KaSession): SirDeclarationParent =
        with(parentProvider) { this@getSirParent.getSirParent(ktAnalysisSession) }

    override fun SirDeclaration.trampolineDeclarations(): List<SirDeclaration> = with (trampolineDeclarationsProvider) {
        this@trampolineDeclarations.trampolineDeclarations()
    }

    override fun KtModule.sirModule(): SirModule = with(moduleProvider) { this@sirModule.sirModule() }

    override fun KaType.translateType(
        ktAnalysisSession: KaSession,
        reportErrorType: (String) -> Nothing,
        reportUnsupportedType: () -> Nothing,
        processTypeImports: (List<SirImport>) -> Unit,
    ): SirType =
        with(typeProvider) { this@translateType.translateType(ktAnalysisSession, reportErrorType, reportUnsupportedType, processTypeImports) }

    override fun KaSymbolWithVisibility.sirVisibility(ktAnalysisSession: KaSession): SirVisibility? =
        with(visibilityChecker) { this@sirVisibility.sirVisibility(ktAnalysisSession) }

    override fun KaScope.extractDeclarations(ktAnalysisSession: KaSession): Sequence<SirDeclaration> =
        with(childrenProvider) { this@extractDeclarations.extractDeclarations(ktAnalysisSession) }
}

/**
 * Provides methods to create [SirEnum] which emulates Kotlin packages.
 */
public interface SirEnumGenerator {
    public fun FqName.sirPackageEnum(): SirEnum
}

/**
 * Names SIR declarations that are constructed from [KaDeclarationSymbol].
 */
public interface SirDeclarationNamer {
    public fun KaDeclarationSymbol.sirDeclarationName(): String
}


/**
 * A single entry point to create a lazy wrapper around the given [KaDeclarationSymbol].
 */
public interface SirDeclarationProvider {
    public fun KaDeclarationSymbol.sirDeclaration(): SirDeclaration
}

/**
 * Given [KaDeclarationSymbol] will produce [SirDeclarationParent], representing the parent for corresponding sir node.
 *
 * For example, given the top level function without a package - will return SirModule that should declare that declarations.
 * Or, given the top level function with a package - will return SirExtension for that package.
 *
 */
public interface SirParentProvider {
    public fun KaDeclarationSymbol.getSirParent(ktAnalysisSession: KaSession): SirDeclarationParent
}

/**
 *  Provides trampoline declarations for a given [SirDeclaration], if any.
 */
public interface SirTrampolineDeclarationsProvider {
    public fun SirDeclaration.trampolineDeclarations(): List<SirDeclaration>
}

/**
 * Translates the given [KtModule] to the corresponding [SirModule].
 * Note that it is not always a 1-1 mapping.
 */
public interface SirModuleProvider {

    public fun KtModule.sirModule(): SirModule
}

public interface SirChildrenProvider {
    public fun KaScope.extractDeclarations(ktAnalysisSession: KaSession): Sequence<SirDeclaration>
}

public interface SirTypeProvider {

    public val errorTypeStrategy: ErrorTypeStrategy
    public val unsupportedTypeStrategy: ErrorTypeStrategy

    public enum class ErrorTypeStrategy {
        Fail, ErrorType
    }

    /**
     * Translates the given [KaType] to [SirType].
     * Calls [reportErrorType] / [reportUnsupportedType] if error/unsupported type
     * is encountered and [errorTypeStrategy] / [unsupportedTypeStrategy] instructs to fail.
     *
     * [processTypeImports] is called with the imports required to use the resulting type properly.
     */
    public fun KaType.translateType(
        ktAnalysisSession: KaSession,
        reportErrorType: (String) -> Nothing,
        reportUnsupportedType: () -> Nothing,
        processTypeImports: (List<SirImport>) -> Unit,
    ): SirType
}

public interface SirVisibilityChecker {
    /**
     * Determines visibility of the given [KaSymbolWithVisibility].
     * @return null if symbol should not be exposed to SIR completely.
     */
    public fun KaSymbolWithVisibility.sirVisibility(ktAnalysisSession: KaSession): SirVisibility?
}

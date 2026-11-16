/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kir.providers

import org.jetbrains.kotlin.kir.KirAvailability
import org.jetbrains.kotlin.kir.KirDeclaration
import org.jetbrains.kotlin.kir.KirDeclarationParent
import org.jetbrains.kotlin.kir.KirFunction
import org.jetbrains.kotlin.kir.KirModule
import org.jetbrains.kotlin.sir.SirDeclaration
import org.jetbrains.kotlin.sir.SirModule

/**
 * A single entry point for all facilities that are required for running SIR -> KIR translation.
 *
 * Similar classes:
 * 1. [SirSession][org.jetbrains.kotlin.sir.providers.SirSession] from Swift export.
 * 2. [KaSession][org.jetbrains.kotlin.analysis.api.KaSession] from Analysis API.
 */
public interface KirSession :
    KirDeclarationNamer,
    KirDeclarationProvider,
    KirParentProvider,
    KirModuleProvider,
    KirVisibilityChecker,
    KirChildrenProvider
{
    public val kirSession: KirSession
        get() = this

    public val moduleToTranslate: SirModule

    public val declarationNamer: KirDeclarationNamer
    public val declarationProvider: KirDeclarationProvider
    public val parentProvider: KirParentProvider
    public val moduleProvider: KirModuleProvider
    public val visibilityChecker: KirVisibilityChecker
    public val childrenProvider: KirChildrenProvider

    override fun SirDeclaration.kirDeclarationName(): String =
        with(declarationNamer) { this@kirDeclarationName.kirDeclarationName() }

    override fun SirDeclaration.toKir(): KirTranslationResult =
        with(declarationProvider) { this@toKir.toKir() }

    override fun SirDeclaration.getKirParent(): KirDeclarationParent =
        with(parentProvider) { this@getKirParent.getKirParent() }

    override fun SirModule.kirModule(): KirModule =
        with(moduleProvider) { this@kirModule.kirModule() }

    override fun SirDeclaration.kirAvailability(): KirAvailability =
        with(visibilityChecker) { this@kirAvailability.kirAvailability() }

    override fun Sequence<SirDeclaration>.extractDeclarations(): Sequence<KirDeclaration> =
        with(childrenProvider) { this@extractDeclarations.extractDeclarations() }
}

/**
 * Names KIR declarations that are constructed from [SirDeclaration].
 */
public interface KirDeclarationNamer {
    public fun SirDeclaration.kirDeclarationName(): String
}

context(kir: KirSession)
public fun SirDeclaration.kirDeclarationName(): String = with(kir) { kirDeclarationName() }

public sealed interface KirTranslationResult {
    public val allDeclarations: List<KirDeclaration>

    public data class Untranslatable(public val origin: SirDeclaration) : KirTranslationResult {
        override val allDeclarations: List<KirDeclaration> get() = emptyList()
    }

    public data class RegularFunction(public val declaration: KirFunction) : KirTranslationResult {
        override val allDeclarations: List<KirDeclaration> = listOf(declaration)
    }
}

/**
 * A single entry point to create a lazy wrapper around the given [SirDeclaration].
 */
public interface KirDeclarationProvider {
    public fun SirDeclaration.toKir(): KirTranslationResult
}

context(kir: KirSession)
public fun SirDeclaration.toKir(): KirTranslationResult = with(kir) { toKir() }

/**
 * Given [SirDeclaration] will produce [KirDeclarationParent], representing the parent for corresponding KIR node.
 *
 * For example, given the top level function - will return the [KirModule] that should declare that function.
 */
public interface KirParentProvider {
    public fun SirDeclaration.getKirParent(): KirDeclarationParent
}

context(kir: KirSession)
public fun SirDeclaration.getKirParent(): KirDeclarationParent = with(kir) { getKirParent() }

/**
 * Translates the given [SirModule] to the corresponding [KirModule].
 */
public interface KirModuleProvider {
    public fun SirModule.kirModule(): KirModule
}

context(kir: KirSession)
public fun SirModule.kirModule(): KirModule = with(kir) { kirModule() }

public interface KirVisibilityChecker {
    /**
     * Determines availability of the given [SirDeclaration].
     */
    public fun SirDeclaration.kirAvailability(): KirAvailability
}

context(kir: KirSession)
public fun SirDeclaration.kirAvailability(): KirAvailability = with(kir) { kirAvailability() }

public interface KirChildrenProvider {
    public fun Sequence<SirDeclaration>.extractDeclarations(): Sequence<KirDeclaration>
}

context(kir: KirSession)
public fun Sequence<SirDeclaration>.extractDeclarations(): Sequence<KirDeclaration> = with(kir) { extractDeclarations() }

/**
 * Runs [block] with [this] session available as a context parameter.
 */
public inline fun <T> KirSession.withSession(block: context(KirSession) () -> T): T = block()

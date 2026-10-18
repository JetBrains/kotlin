/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.generators

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.extensions.declarationGenerators
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.plugin.createCompanionObject
import org.jetbrains.kotlin.fir.plugin.createDefaultPrivateConstructor
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.lombok.generators.kotlin.isCompanionNeeded
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.name.SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT
import org.jetbrains.kotlin.utils.addToStdlib.runIf

object LombokCompanionObjectKey : LombokDeclarationKey()

/**
 * A generator that puts members into the companion object of the class it generates for - `@Log` its logger,
 * `@NoArgsConstructor` its static factory, `@Builder` its `builder()` - and so needs one where the class declares
 * none.
 *
 * Implementors are [FirDeclarationGenerationExtension]s. They must not generate the companion object themselves:
 * [LombokCompanionObjectGenerator] generates the single one they share.
 */
interface LombokCompanionObjectContributor {
    /** Whether [owner] needs a generated companion object for what this generator would put into it. */
    fun needsCompanionObject(owner: FirClassSymbol<*>): Boolean
}

/**
 * Generates the one companion object every Lombok feature that needs one shares, and the members of it.
 *
 * Letting every generator create its own made the platform fail outright with "Multiple plugins
 * generated nested class with same name Companion" (KT-86915) if multiple annotations on the same class are used.
 * So exactly one extension may produce it.
 */
class LombokCompanionObjectGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    /**
     * Resolved lazily: the extensions are still being constructed while this one is, so asking the session for
     * them in an initializer would come back short.
     */
    private val contributors: List<LombokCompanionObjectContributor> by lazy {
        session.extensionService.declarationGenerators.filterIsInstance<LombokCompanionObjectContributor>()
    }

    private val companionObjectsCache: FirCache<FirClassSymbol<*>, FirRegularClassSymbol?, NestedClassGenerationContext> =
        session.firCachesFactory.createCache { owner: FirClassSymbol<*>, context: NestedClassGenerationContext ->
            runIf(isCompanionNeeded(owner, context) && contributors.any { it.needsCompanionObject(owner) }) {
                createCompanionObject(owner, LombokCompanionObjectKey).symbol
            }
        }

    override fun getNestedClassifiersNames(classSymbol: FirClassSymbol<*>, context: NestedClassGenerationContext): Set<Name> {
        return runIf(companionObjectsCache.getValue(classSymbol, context) != null) {
            setOf(DEFAULT_NAME_FOR_COMPANION_OBJECT)
        }.orEmpty()
    }

    override fun generateNestedClassLikeDeclaration(
        owner: FirClassSymbol<*>,
        name: Name,
        context: NestedClassGenerationContext,
    ): FirClassLikeSymbol<*>? {
        return runIf(name == DEFAULT_NAME_FOR_COMPANION_OBJECT) { companionObjectsCache.getValue(owner, context) }
    }

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        if (!classSymbol.isGeneratedLombokCompanionObject) return emptySet()

        return buildSet {
            // The companion object is generated, so its constructor has to be too.
            add(SpecialNames.INIT)
        }
    }

    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
        if (!context.owner.isGeneratedLombokCompanionObject) return emptyList()

        return buildList {
            add(createDefaultPrivateConstructor(context.owner, LombokCompanionObjectKey).symbol)
        }
    }
}

val FirClassSymbol<*>.isGeneratedLombokCompanionObject: Boolean
    get() = (origin as? FirDeclarationOrigin.Plugin)?.key == LombokCompanionObjectKey

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
import org.jetbrains.kotlin.fir.extensions.UnsafePluginApi
import org.jetbrains.kotlin.fir.extensions.declarationGenerators
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.plugin.createCompanionObject
import org.jetbrains.kotlin.fir.plugin.createDefaultPrivateConstructor
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.lombok.generators.kotlin.isCompanionNeeded
import org.jetbrains.kotlin.name.CallableId
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
 * Several annotations on the same class each need a companion object to hold what they generate, and a class has
 * only one. Letting every generator create its own made the platform fail outright with "Multiple plugins
 * generated nested class with same name Companion" (KT-86915), so exactly one extension may produce it.
 *
 * That extension also has to produce its members, hence the fan-out below: a generated class is served by its
 * owner generator alone - `FirGeneratedScopes` answers `getExtensionsForClass` with `listOf(ownerGenerator)` for
 * one - so the contributors are never asked about a companion object this generator owns. They are asked here
 * instead, with the very symbol and context the platform would have passed them, and each still decides what to
 * put in on its own: they all key on the annotations of the class containing the companion object.
 *
 * Nested classifiers are not fanned out. Nothing generates a class into a companion object - a `@Builder` class is
 * nested under the entity itself, and a generated companion object carries no annotation to generate from - so
 * there is nothing to forward.
 */
class LombokCompanionObjectGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    /**
     * Resolved lazily: the extensions are still being constructed while this one is, so asking the session for
     * them in an initializer would come back short.
     */
    private val contributors: List<FirDeclarationGenerationExtension> by lazy {
        session.extensionService.declarationGenerators.filter { it is LombokCompanionObjectContributor }
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
            // The companion object is generated, so its constructor has to be too, and only this generator can:
            // the contributors no longer see a key of their own on it.
            add(SpecialNames.INIT)
            contributors.flatMapTo(this) { it.getCallableNamesForClass(classSymbol, context) }
        }
    }

    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
        if (!context.owner.isGeneratedLombokCompanionObject) return emptyList()

        return buildList {
            add(createDefaultPrivateConstructor(context.owner, LombokCompanionObjectKey).symbol)
            contributors.flatMapTo(this) { it.generateConstructors(context) }
        }
    }

    override fun generateFunctions(callableId: CallableId, context: MemberGenerationContext?): List<FirNamedFunctionSymbol> {
        return fanOut(callableId, context) { generateFunctions(callableId, context) }
    }

    override fun generateProperties(callableId: CallableId, context: MemberGenerationContext?): List<FirPropertySymbol> {
        return fanOut(callableId, context) { generateProperties(callableId, context) }
    }

    @UnsafePluginApi
    override fun generateFields(callableId: CallableId, context: MemberGenerationContext?): List<FirFieldSymbol> {
        return fanOut(callableId, context) { generateFields(callableId, context) }
    }

    /**
     * Everything the contributors generate for [callableId], and nothing else.
     *
     * The name filter is what makes the fan-out safe: a contributor asked for one name hands back everything it has
     * for the owner, [callableId] being of no interest to a generator the platform only ever asks about names it
     * reported itself. Fanned out, each of them is asked for every other one's names too, and without the filter
     * `LoggerGenerator` answered the request for the static factory with its logger - the same symbol then landing
     * under both names, which fir2ir met as "IrSimpleFunctionSymbolImpl is already bound".
     */
    private inline fun <T : FirCallableSymbol<*>> fanOut(
        callableId: CallableId,
        context: MemberGenerationContext?,
        generate: FirDeclarationGenerationExtension.() -> List<T>,
    ): List<T> {
        if (context?.owner?.isGeneratedLombokCompanionObject != true) return emptyList()
        return contributors.flatMap { contributor -> contributor.generate().filter { it.name == callableId.callableName } }
    }

    private fun FirDeclarationGenerationExtension.needsCompanionObject(owner: FirClassSymbol<*>): Boolean =
        (this as LombokCompanionObjectContributor).needsCompanionObject(owner)
}

/** Whether [this] is a companion object [LombokCompanionObjectGenerator] generated. */
val FirClassSymbol<*>.isGeneratedLombokCompanionObject: Boolean
    get() = (origin as? FirDeclarationOrigin.Plugin)?.key == LombokCompanionObjectKey

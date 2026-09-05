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
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.lombok.LombokNames
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

object ConstructorGeneratorKey : LombokDeclarationKey()

/**
 * Checks whether the current [FirDeclarationOrigin] instance is of type [FirDeclarationOrigin.Plugin] and carries
 * [ConstructorGeneratorKey], that is, whether the declaration is a constructor or a static factory this generator
 * produced.
 */
val FirDeclarationOrigin.isGeneratedConstructor: Boolean
    get() = this is FirDeclarationOrigin.Plugin && key == ConstructorGeneratorKey

class LombokConstructorsGenerator(session: FirSession) :
    FirDeclarationGenerationExtension(session), LombokCompanionObjectContributor {
    companion object {
        private val PREDICATE = DeclarationPredicate.create {
            annotated(listOf(LombokNames.NO_ARGS_CONSTRUCTOR))
        }
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(PREDICATE)
    }

    private val parts: List<AbstractConstructorGeneratorPart<*>> = listOf(
        AllArgsConstructorGeneratorPart(session),
        NoArgsConstructorGeneratorPart(session),
        RequiredArgsConstructorGeneratorPart(session)
    )

    /**
     * A static factory is generated into the companion object, so a class that gets one needs a companion object to
     * hold it. A factory that would only be shadowed isn't generated, so it must not ask for an empty one either.
     */
    override fun needsCompanionObject(owner: FirClassSymbol<*>): Boolean =
        parts.any { part -> part.generatesStaticFactory(owner) }

    private val cache: FirCache<FirClassSymbol<*>, Collection<FirFunctionSymbol<*>>?, MemberGenerationContext?> =
        session.firCachesFactory.createCache(::createConstructors)

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        return buildSet {
            cache.getValue(classSymbol, context)?.forEach {
                when (it) {
                    is FirConstructorSymbol -> add(SpecialNames.INIT)
                    else -> add(it.callableId.callableName)
                }
            }
        }
    }

    override fun generateFunctions(callableId: CallableId, context: MemberGenerationContext?): List<FirNamedFunctionSymbol> {
        val owner = context?.owner ?: return emptyList()
        return cache.getValue(owner, context)?.filterIsInstance<FirNamedFunctionSymbol>().orEmpty()
    }

    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
        return cache.getValue(context.owner, context)?.filterIsInstance<FirConstructorSymbol>().orEmpty()
    }

    private fun createConstructors(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext?): Collection<FirFunctionSymbol<*>>? {
        return buildList {
            parts.forEach {
                with(it) {
                    addIfNonClashing(classSymbol, context?.declaredScope)
                }
            }
        }.takeIf { it.isNotEmpty() }?.map { it.symbol }
    }
}

/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2.generators

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.lombok.k2.generators.kotlin.createConstructorIfGeneratedCompanion
import org.jetbrains.kotlin.lombok.k2.generators.kotlin.initializeCompanionObjectIfNeeded
import org.jetbrains.kotlin.lombok.k2.generators.kotlin.needsConstructorIfGeneratedCompanion
import org.jetbrains.kotlin.lombok.utils.LombokNames
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.name.SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT

object ConstructorGeneratorKey : LombokDeclarationKey()

class LombokConstructorsGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
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

    private val companionObjectsCache: FirCache<FirClassSymbol<*>, FirRegularClassSymbol?, NestedClassGenerationContext> =
        session.firCachesFactory.createCache { owner: FirClassSymbol<*>, context: NestedClassGenerationContext ->
            initializeCompanionObjectIfNeeded(owner, context) {
                // Generate companion object only if there is at least one constructor with visibility and a specified static name
                // Because static constructors are being generated inside companion objects.
                if (parts.none { part ->
                        part.getConstructorInfo(owner)?.let { it.visibility != null && it.staticName != null } ?: false
                    }
                ) {
                    null
                } else {
                    ConstructorGeneratorKey
                }
            }
        }

    private val cache: FirCache<FirClassSymbol<*>, Collection<FirFunctionSymbol<*>>?, MemberGenerationContext?> =
        session.firCachesFactory.createCache(::createConstructors)

    override fun getNestedClassifiersNames(classSymbol: FirClassSymbol<*>, context: NestedClassGenerationContext): Set<Name> {
        if (companionObjectsCache.getValue(classSymbol, context) != null) {
            return setOf(DEFAULT_NAME_FOR_COMPANION_OBJECT)
        }
        return emptySet()
    }

    override fun generateNestedClassLikeDeclaration(
        owner: FirClassSymbol<*>,
        name: Name,
        context: NestedClassGenerationContext
    ): FirClassLikeSymbol<*>? {
        return companionObjectsCache.getValue(owner, context)
    }

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        return buildSet {
            if (classSymbol.needsConstructorIfGeneratedCompanion<ConstructorGeneratorKey>()) {
                add(SpecialNames.INIT)
            }

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
        return buildList {
            val owner = context.owner
            createConstructorIfGeneratedCompanion<ConstructorGeneratorKey>(context.owner)?.let {
                add(it)
            }
            addAll(cache.getValue(owner, context)?.filterIsInstance<FirConstructorSymbol>().orEmpty())
        }
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

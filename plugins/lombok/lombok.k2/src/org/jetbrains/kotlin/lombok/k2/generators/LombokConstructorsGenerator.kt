/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2.generators

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

class LombokConstructorsGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    private val parts: List<AbstractConstructorGeneratorPart<*>> = listOf(
        AllArgsConstructorGeneratorPart(session),
        NoArgsConstructorGeneratorPart(session),
        RequiredArgsConstructorGeneratorPart(session)
    )

    private val cache: FirCache<FirClassSymbol<*>, Collection<FirFunctionSymbol<*>>?, MemberGenerationContext?> =
        session.firCachesFactory.createCache(::createConstructors)

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        if (!classSymbol.isSuitableJavaClass()) return emptySet()
        return cache.getValue(classSymbol, context)?.mapTo(mutableSetOf()) {
            when (it) {
                is FirConstructorSymbol -> SpecialNames.INIT
                else -> it.callableId.callableName
            }
        } ?: emptySet()
    }

    override fun generateFunctions(callableId: CallableId, context: MemberGenerationContext?): List<FirNamedFunctionSymbol> {
        val owner = context?.owner ?: return emptyList()
        if (!owner.isSuitableJavaClass()) return emptyList()
        return cache.getValue(owner, context)?.filterIsInstance<FirNamedFunctionSymbol>().orEmpty()
    }

    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
        val owner = context.owner
        if (!owner.isSuitableJavaClass()) return emptyList()
        return cache.getValue(owner, context)?.filterIsInstance<FirConstructorSymbol>().orEmpty()
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

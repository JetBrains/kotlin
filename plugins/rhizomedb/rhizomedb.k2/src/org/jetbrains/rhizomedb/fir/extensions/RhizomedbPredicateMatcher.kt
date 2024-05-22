/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.rhizomedb.fir.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.constructors
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.resolve.getSuperTypes
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.rhizomedb.fir.RhizomedbFirPredicates
import org.jetbrains.rhizomedb.fir.RhizomedbSymbolNames

class RhizomedbPredicateMatcher(session: FirSession) : FirExtensionSessionComponent(session) {
    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(RhizomedbFirPredicates.annotatedWithEntityType)
        register(RhizomedbFirPredicates.annotatedWithAttribute)
        register(RhizomedbFirPredicates.annotatedWithMany)
    }

    fun isEntity(symbol: FirClassSymbol<*>): Boolean {
        return entityCache.getValue(symbol)
    }

    fun hasFromEidConstructor(symbol: FirClassSymbol<*>): Boolean {
        return constructorCache.getValue(symbol)
    }

    fun isEntityType(symbol: FirClassSymbol<*>): Boolean {
        return entityTypeCache.getValue(symbol)
    }

    fun isEntityTypeAnnotated(symbol: FirClassLikeSymbol<*>): Boolean {
        return entityTypeAnnotatedCache.getValue(symbol)
    }

    fun isAttributeAnnotated(symbol: FirPropertySymbol): Boolean {
        return attributeAnnotatedCache.getValue(symbol)
    }

    fun isManyAnnotated(symbol: FirPropertySymbol): Boolean {
        return manyAnnotatedCache.getValue(symbol)
    }

    private val entityCache: FirCache<FirClassSymbol<*>, Boolean, Nothing?> = session.firCachesFactory.createCache { symbol, _ ->
        if (symbol.classId == RhizomedbSymbolNames.entityClassId) {
            return@createCache true
        }
        symbol.getSuperTypes(session).any {
            it.lookupTag.classId == RhizomedbSymbolNames.entityClassId
        }
    }

    private val constructorCache: FirCache<FirClassSymbol<*>, Boolean, Nothing?> = session.firCachesFactory.createCache { symbol, _ ->
        @OptIn(SymbolInternals::class)
        symbol.fir.constructors(session).any {
            val valueParam = it.valueParameterSymbols.singleOrNull() ?: return@any false
            val type = valueParam.resolvedReturnType.type.classId
            type == RhizomedbSymbolNames.eidClassId
        }
    }

    private val entityTypeCache: FirCache<FirClassSymbol<*>, Boolean, Nothing?> = session.firCachesFactory.createCache { symbol, _ ->
        if (symbol.classId == RhizomedbSymbolNames.entityTypeClassId) {
            return@createCache true
        }
        symbol.getSuperTypes(session).any {
            it.lookupTag.classId == RhizomedbSymbolNames.entityTypeClassId
        }
    }

    private val entityTypeAnnotatedCache: FirCache<FirClassLikeSymbol<*>, Boolean, Nothing?> =
        session.firCachesFactory.createCache { symbol, _ ->
            session.predicateBasedProvider.matches(RhizomedbFirPredicates.annotatedWithEntityType, symbol)
        }

    private val attributeAnnotatedCache: FirCache<FirPropertySymbol, Boolean, Nothing?> =
        session.firCachesFactory.createCache { symbol, _ ->
            session.predicateBasedProvider.matches(RhizomedbFirPredicates.annotatedWithAttribute, symbol)
        }

    private val manyAnnotatedCache: FirCache<FirPropertySymbol, Boolean, Nothing?> =
        session.firCachesFactory.createCache { symbol, _ ->
            session.predicateBasedProvider.matches(RhizomedbFirPredicates.annotatedWithMany, symbol)
        }
}

val FirSession.rhizomedbPredicateMatcher: RhizomedbPredicateMatcher by FirSession.sessionComponentAccessor()
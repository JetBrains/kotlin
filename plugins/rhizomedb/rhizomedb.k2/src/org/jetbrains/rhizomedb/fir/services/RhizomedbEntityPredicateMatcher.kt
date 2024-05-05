/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.rhizomedb.fir.services

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.resolve.getSuperTypes
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.rhizomedb.fir.RhizomedbFirPredicates
import org.jetbrains.rhizomedb.fir.resolve.RhizomedbSymbolNames

class RhizomedbEntityPredicateMatcher(session: FirSession) : FirExtensionSessionComponent(session) {
    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(RhizomedbFirPredicates.annotatedWithEntityType)
        register(RhizomedbFirPredicates.annotatedWithAttribute)
    }

    fun isEntity(symbol: FirClassSymbol<*>): Boolean {
        return entityCache.getValue(symbol)
    }

    fun isEntityType(symbol: FirClassSymbol<*>): Boolean {
        return entityTypeCache.getValue(symbol)
    }

    fun isEntityTypeAnnotated(symbol: FirClassSymbol<*>): Boolean {
        return entityTypeAnnotatedCache.getValue(symbol)
    }

    fun isAttributeAnnotated(symbol: FirPropertySymbol): Boolean {
        return attributeAnnotatedCache.getValue(symbol)
    }

    private val entityCache: FirCache<FirClassSymbol<*>, Boolean, Nothing?> = session.firCachesFactory.createCache { symbol, _ ->
        symbol.getSuperTypes(session).any {
            it.lookupTag.classId == RhizomedbSymbolNames.entityClassId
        }
    }

    private val entityTypeCache: FirCache<FirClassSymbol<*>, Boolean, Nothing?> = session.firCachesFactory.createCache { symbol, _ ->
        symbol.getSuperTypes(session).any {
            it.lookupTag.classId == RhizomedbSymbolNames.entityTypeClassId
        }
    }

    private val entityTypeAnnotatedCache: FirCache<FirClassSymbol<*>, Boolean, Nothing?> =
        session.firCachesFactory.createCache { symbol, _ ->
            session.predicateBasedProvider.matches(RhizomedbFirPredicates.annotatedWithEntityType, symbol)
        }

    private val attributeAnnotatedCache: FirCache<FirPropertySymbol, Boolean, Nothing?> =
        session.firCachesFactory.createCache { symbol, _ ->
            session.predicateBasedProvider.matches(RhizomedbFirPredicates.annotatedWithAttribute, symbol)
        }
}

val FirSession.rhizomedbEntityPredicateMatcher: RhizomedbEntityPredicateMatcher by FirSession.sessionComponentAccessor()
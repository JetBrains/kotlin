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
    }

    fun isEntityTypeAnnotated(symbol: FirClassLikeSymbol<*>): Boolean {
        return entityTypeAnnotatedCache.getValue(symbol)
    }

    private val entityTypeAnnotatedCache: FirCache<FirClassLikeSymbol<*>, Boolean, Nothing?> =
        session.firCachesFactory.createCache { symbol, _ ->
            session.predicateBasedProvider.matches(RhizomedbFirPredicates.annotatedWithEntityType, symbol)
        }
}

val FirSession.rhizomedbPredicateMatcher: RhizomedbPredicateMatcher by FirSession.sessionComponentAccessor()
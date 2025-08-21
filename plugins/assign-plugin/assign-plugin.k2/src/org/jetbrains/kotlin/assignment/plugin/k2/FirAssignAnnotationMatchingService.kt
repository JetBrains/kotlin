/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.assignment.plugin.k2

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent.Factory
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.name.FqName

internal class FirAssignAnnotationMatchingService(
    session: FirSession,
    private val annotationClassIds: List<FqName>
) : FirExtensionSessionComponent(session) {

    companion object {
        fun getFactory(annotations: List<String>): Factory {
            return Factory { session -> FirAssignAnnotationMatchingService(session, annotations.map { FqName(it) }) }
        }
    }

    private val cache: FirCache<FirRegularClassSymbol, Boolean, Nothing?> = session.firCachesFactory.createCache { symbol, _ ->
        symbol.annotated()
    }

    fun isAnnotated(symbol: FirRegularClassSymbol?): Boolean {
        if (symbol == null) {
            return false
        }
        return cache.getValue(symbol)
    }

    @OptIn(SymbolInternals::class)
    private fun FirRegularClassSymbol.annotated(): Boolean {
        if (this.annotations.any { it.toAnnotationClassId(session)?.asSingleFqName() in annotationClassIds }) return true
        return resolvedSuperTypeRefs.any { superTypeRef ->
            val symbol = superTypeRef.coneType.fullyExpandedType(session).toRegularClassSymbol(session) ?: return@any false
            symbol.annotated()
        }
    }
}

internal val FirSession.annotationMatchingService: FirAssignAnnotationMatchingService by FirSession.sessionComponentAccessor()

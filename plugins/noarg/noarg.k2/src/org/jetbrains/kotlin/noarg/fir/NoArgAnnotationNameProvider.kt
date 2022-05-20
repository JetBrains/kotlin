/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.noarg.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.extensions.predicate.has
import org.jetbrains.kotlin.fir.extensions.predicate.metaHas
import org.jetbrains.kotlin.fir.extensions.predicate.or
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.extensions.utils.AbstractSimpleClassPredicateMatchingService
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol
import org.jetbrains.kotlin.name.FqName

class FirNoArgPredicateMatcher(
    session: FirSession,
    noArgAnnotationFqNames: List<String>
) : AbstractSimpleClassPredicateMatchingService(session) {
    companion object {
        fun getFactory(noArgAnnotationFqNames: List<String>): Factory {
            return Factory { session -> FirNoArgPredicateMatcher(session, noArgAnnotationFqNames) }
        }
    }

    override val predicate = run {
        val annotationFqNames = noArgAnnotationFqNames.map { FqName(it) }
        has(annotationFqNames) or metaHas(annotationFqNames)
    }
}

val FirSession.noArgPredicateMatcher: FirNoArgPredicateMatcher by FirSession.sessionComponentAccessor()

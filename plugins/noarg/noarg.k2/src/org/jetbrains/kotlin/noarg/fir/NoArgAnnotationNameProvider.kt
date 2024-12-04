/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.noarg.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.utils.AbstractSimpleClassPredicateMatchingService
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

    override val predicate = DeclarationPredicate.create {
        val annotationFqNames = noArgAnnotationFqNames.map { FqName(it) }
        annotated(annotationFqNames) or metaAnnotated(annotationFqNames, includeItself = true)
    }
}

val FirSession.noArgPredicateMatcher: FirNoArgPredicateMatcher by FirSession.sessionComponentAccessor()

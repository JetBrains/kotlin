/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.assignment.plugin.k2

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.utils.AbstractSimpleClassPredicateMatchingService
import org.jetbrains.kotlin.name.FqName

class FirAssignAnnotationMatchingService(
    session: FirSession,
    annotationClassIds: Set<FqName>,
) : AbstractSimpleClassPredicateMatchingService(session) {
    companion object {
        fun getFactory(annotations: List<String>): Factory {
            return Factory { session ->
                FirAssignAnnotationMatchingService(session, annotations.map { FqName(it) }.toSet())
            }
        }
    }

    override val predicate: DeclarationPredicate = DeclarationPredicate.create {
        // TODO KT-83571 Reconsider meta-annotation support for assign plugin
        annotated(annotationClassIds) or metaAnnotated(annotationClassIds, includeItself = true)
    }
}

internal val FirSession.annotationMatchingService: FirAssignAnnotationMatchingService by FirSession.sessionComponentAccessor()

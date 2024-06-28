/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.copy
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.extensions.FirStatusTransformerExtension
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.utils.AbstractSimpleClassPredicateMatchingService
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.FqName

class AllOpenMatcherBasedStatusTransformer(session: FirSession) : FirStatusTransformerExtension(session) {
    override fun needTransformStatus(declaration: FirDeclaration): Boolean {
        return when (declaration) {
            is FirRegularClass -> declaration.classKind == ClassKind.CLASS && session.allOpenPredicateMatcher.isAnnotated(declaration.symbol)
            is FirCallableDeclaration -> {
                val parentClassId = declaration.symbol.callableId.classId ?: return false
                if (parentClassId.isLocal) return false
                val parentClassSymbol = session.symbolProvider.getClassLikeSymbolByClassId(parentClassId) as? FirRegularClassSymbol
                    ?: return false
                session.allOpenPredicateMatcher.isAnnotated(parentClassSymbol)
            }
            else -> false
        }
    }

    override fun transformStatus(status: FirDeclarationStatus, declaration: FirDeclaration): FirDeclarationStatus {
        return if (status.modality == null) {
            status.copy(modality = Modality.OPEN)
        } else {
            status
        }
    }
}

class AllOpenPredicateMatcher(session: FirSession) : AbstractSimpleClassPredicateMatchingService(session) {
    companion object {
        private val ALL_OPEN2 = FqName("org.jetbrains.kotlin.fir.plugin.AllOpen2")
    }

    override val predicate = DeclarationPredicate.create {
        annotated(ALL_OPEN2)
    }
}

private val FirSession.allOpenPredicateMatcher: AllOpenPredicateMatcher by FirSession.sessionComponentAccessor()

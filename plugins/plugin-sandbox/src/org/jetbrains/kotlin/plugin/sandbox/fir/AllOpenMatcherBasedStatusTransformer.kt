/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.plugin.sandbox.fir

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.copy
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.extensions.FirStatusTransformerExtension
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.utils.AbstractSimpleClassPredicateMatchingService
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.FqName

class AllOpenMatcherBasedStatusTransformer(session: FirSession) : FirStatusTransformerExtension(session) {
    override fun needTransformStatus(declaration: FirDeclaration): Boolean {
        return when (declaration) {
            is FirRegularClass -> declaration.classKind == ClassKind.CLASS && session.allOpenPredicateMatcher.isAnnotated(declaration.symbol)
            is FirCallableDeclaration -> {
                val parentClassSymbol = declaration.symbol.getContainingClassSymbol() as? FirRegularClassSymbol ?: return false
                if (parentClassSymbol.isLocal) return false
                session.allOpenPredicateMatcher.isAnnotated(parentClassSymbol)
            }
            else -> false
        }
    }

    override fun transformCallableStatus(
        status: FirCallableDeclarationStatus,
        declaration: FirCallableDeclaration,
    ): FirCallableDeclarationStatus {
        if (status.modality != null) return status
        return status.copy(modality = OPEN)
    }

    override fun transformClassLikeStatus(
        status: FirClassLikeDeclarationStatus,
        declaration: FirClassLikeDeclaration,
    ): FirClassLikeDeclarationStatus {
        if (status.modality != null) return status
        return status.copy(modality = OPEN)
    }
}

class AllOpenPredicateMatcher(session: FirSession) : AbstractSimpleClassPredicateMatchingService(session) {
    companion object {
        private val ALL_OPEN2 = FqName("org.jetbrains.kotlin.plugin.sandbox.AllOpen2")
    }

    override val predicate = DeclarationPredicate.create {
        annotated(ALL_OPEN2)
    }
}

private val FirSession.allOpenPredicateMatcher: AllOpenPredicateMatcher by FirSession.sessionComponentAccessor()

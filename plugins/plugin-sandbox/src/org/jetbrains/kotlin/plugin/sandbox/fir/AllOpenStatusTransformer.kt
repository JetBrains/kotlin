/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.plugin.sandbox.fir

import org.jetbrains.kotlin.descriptors.Modality.OPEN
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.copy
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.FirStatusTransformerExtension
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

class AllOpenStatusTransformer(session: FirSession) : FirStatusTransformerExtension(session) {
    companion object {
        private val ALL_OPEN = FqName("org.jetbrains.kotlin.plugin.sandbox.AllOpen")
        private val PREDICATE = DeclarationPredicate.create {
            annotatedOrUnder(ALL_OPEN) or metaAnnotated(ALL_OPEN, includeItself = true)
        }
    }

    override fun transformStatus(status: FirDeclarationStatus, declaration: FirDeclaration): FirDeclarationStatus {
        if (status.modality != null) return status
        return when (status) {
            is FirCallableDeclarationStatus -> status.copy(modality = OPEN)
            is FirClassLikeDeclarationStatus -> status.copy(modality = OPEN)
            else -> shouldNotBeCalled()
        }
    }

    override fun needTransformStatus(declaration: FirDeclaration): Boolean {
        return session.predicateBasedProvider.matches(PREDICATE, declaration)
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(PREDICATE)
    }
}

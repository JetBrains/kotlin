/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.fir

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.FirSupertypeGenerationExtension
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.parcelize.ParcelizeNames.ADD_PARCELABLE_SUPERTYPE_FQ_NAME
import org.jetbrains.kotlin.parcelize.ParcelizeNames.PARCELABLE_ID

class FirParcelizeSupertypeGenerator(session: FirSession) : FirSupertypeGenerationExtension(session) {
    companion object {
        private val PREDICATE = DeclarationPredicate.create { annotated(ADD_PARCELABLE_SUPERTYPE_FQ_NAME) }
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(PREDICATE)
    }

    override fun needTransformSupertypes(declaration: FirClassLikeDeclaration): Boolean {
        if (declaration !is FirRegularClass) return false
        if (declaration.classKind == ClassKind.ANNOTATION_CLASS) return false
        return session.predicateBasedProvider.matches(PREDICATE, declaration)
    }

    override fun computeAdditionalSupertypes(
        classLikeDeclaration: FirClassLikeDeclaration,
        resolvedSupertypes: List<FirResolvedTypeRef>,
        typeResolver: TypeResolveService,
    ): List<ConeKotlinType> = listOf(PARCELABLE_ID.constructClassLikeType(emptyArray(), isMarkedNullable = false))
}

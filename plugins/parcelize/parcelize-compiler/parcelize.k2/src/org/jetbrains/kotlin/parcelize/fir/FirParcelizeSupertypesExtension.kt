/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.fir

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isSealed
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.FirSupertypeGenerationExtension
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.isMetadataCompilation
import org.jetbrains.kotlin.fir.resolve.toClassLikeSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.parcelize.ParcelizeNames.PARCELABLE_ID

class FirParcelizeSupertypesExtension(
    session: FirSession,
    parcelizeAnnotationFqNames: List<FqName>,
) : FirSupertypeGenerationExtension(session) {
    private val predicate = DeclarationPredicate.create { annotated(parcelizeAnnotationFqNames) }

    override fun needTransformSupertypes(declaration: FirClassLikeDeclaration): Boolean {
        if (session.isMetadataCompilation || declaration !is FirRegularClass) return false
        when (declaration.classKind) {
            ClassKind.CLASS,
            ClassKind.OBJECT,
            ClassKind.ENUM_CLASS -> Unit

            ClassKind.INTERFACE -> if (!declaration.isSealed) return false

            ClassKind.ENUM_ENTRY,
            ClassKind.ANNOTATION_CLASS -> return false
        }
        return session.predicateBasedProvider.matches(predicate, declaration)
    }

    override fun computeAdditionalSupertypes(
        classLikeDeclaration: FirClassLikeDeclaration,
        resolvedSupertypes: List<FirResolvedTypeRef>,
        typeResolver: TypeResolveService,
    ): List<ConeKotlinType> {
        // An expect supertype may actualize to Parcelable, but that mapping is not available during
        // supertype generation.
        val hasParcelableOrExpectSupertype = resolvedSupertypes.any { supertype ->
            supertype.coneType.classId == PARCELABLE_ID ||
                supertype.coneType.toClassLikeSymbol(session)?.isExpect == true
        }
        if (hasParcelableOrExpectSupertype) return emptyList()
        return listOf(PARCELABLE_ID.constructClassLikeType(emptyArray(), isMarkedNullable = false))
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(predicate)
    }
}

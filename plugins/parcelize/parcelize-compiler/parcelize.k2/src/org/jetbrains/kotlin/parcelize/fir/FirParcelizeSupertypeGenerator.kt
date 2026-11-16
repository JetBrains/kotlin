/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.fir

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.declarations.utils.isSealed
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.FirSupertypeGenerationExtension
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.toClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.parcelize.ParcelizeNames.INTERNAL_PARCELABLE_ID
import org.jetbrains.kotlin.parcelize.ParcelizeNames.PARCELABLE_CLASS_IDS

class FirParcelizeSupertypeGenerator(
    session: FirSession,
    annotations: List<FqName>,
) : FirSupertypeGenerationExtension(session) {
    private val predicate = DeclarationPredicate.create { annotated(annotations) }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(predicate)
    }

    override fun needTransformSupertypes(declaration: FirClassLikeDeclaration): Boolean {
        if (declaration !is FirRegularClass) return false
        if (declaration.isLocal) return false
        if (declaration.classKind == ClassKind.ANNOTATION_CLASS) return false
        if (declaration.isInterface && !declaration.isSealed) return false
        return session.predicateBasedProvider.matches(predicate, declaration)
    }

    override fun computeAdditionalSupertypes(
        classLikeDeclaration: FirClassLikeDeclaration,
        resolvedSupertypes: List<FirResolvedTypeRef>,
        typeResolver: TypeResolveService,
    ): List<ConeKotlinType> {
        if (resolvedSupertypes.any { it.coneType.isParcelableSupertype() }) return emptyList()
        return listOf(INTERNAL_PARCELABLE_ID.constructClassLikeType())
    }

    /**
     * Tells if a direct supertype already makes the class `Parcelable`.
     *
     * The check does not look at the supertypes of a class or expand typealiases, because FIR resolves them only after this extension runs.
     * Thus, a class that inherits `Parcelable` through another supertype or a typealias gets the generated supertype too.
     * This is valid, because the generated supertype is a separate interface that extends `android.os.Parcelable` on Android.
     * A direct supertype that the plugin also transforms counts as `Parcelable`, because it gets the supertype too.
     */
    @OptIn(SymbolInternals::class)
    private fun ConeKotlinType.isParcelableSupertype(): Boolean {
        val id = classId ?: return false
        if (id in PARCELABLE_CLASS_IDS) return true
        val fir = toClassLikeSymbol(session)?.fir as? FirClass ?: return false
        return fir.moduleData.session.kind == FirSession.Kind.Source && needTransformSupertypes(fir)
    }
}

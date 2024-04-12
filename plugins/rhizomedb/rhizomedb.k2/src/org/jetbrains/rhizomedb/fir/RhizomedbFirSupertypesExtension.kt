/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.rhizomedb.fir

import org.jetbrains.kotlin.descriptors.isObject
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.FirSupertypeGenerationExtension
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.references.impl.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.getContainingDeclaration
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.isWasm
import org.jetbrains.kotlin.platform.konan.isNative
import org.jetbrains.rhizomedb.fir.RhizomedbFirPredicates.annotatedWithEntityType

class RhizomedbFirSupertypesExtension(session: FirSession) : FirSupertypeGenerationExtension(session) {

    private val isJvmOrMetadata = !session.moduleData.platform.run { isNative() || isJs() || isWasm() }

    override fun needTransformSupertypes(declaration: FirClassLikeDeclaration): Boolean {
        return false
//        declaration.getContainingDeclaration(session)?.hasAttributeAnnotation()
    }

    private fun isSerializableObjectAndNeedsFactory(declaration: FirClassLikeDeclaration): Boolean {
        if (isJvmOrMetadata) return false
        return declaration is FirClass && declaration.classKind.isObject
                && session.predicateBasedProvider.matches(annotatedWithEntityType, declaration)
    }

    private fun isCompanionAndNeedsFactory(declaration: FirClassLikeDeclaration): Boolean {
        if (isJvmOrMetadata) return false
        if (declaration !is FirRegularClass) return false
        if (!declaration.isCompanion) return false
        val parentSymbol = declaration.symbol.getContainingDeclaration(session) as FirClassSymbol<*>
        return session.predicateBasedProvider.matches(annotatedWithEntityType, parentSymbol)
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
//        register(serializerFor)
    }

    override fun computeAdditionalSupertypes(
        classLikeDeclaration: FirClassLikeDeclaration,
        resolvedSupertypes: List<FirResolvedTypeRef>,
        typeResolver: TypeResolveService,
    ): List<FirResolvedTypeRef> {
        return emptyList()
    }

    // Function helps to resolve class call from annotation argument to `ConeKotlinType`
    private val FirPropertyAccessExpression.qualifierName: Name?
        get() = (calleeReference as? FirSimpleNamedReference)?.name
}

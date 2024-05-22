/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.rhizomedb.fir.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.extensions.FirSupertypeGenerationExtension
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.fir.types.toFirResolvedTypeRef
import org.jetbrains.rhizomedb.fir.RhizomedbSymbolNames
import org.jetbrains.rhizomedb.fir.getContainingClass

class RhizomedbFirSupertypesExtension(session: FirSession) : FirSupertypeGenerationExtension(session) {
    override fun needTransformSupertypes(declaration: FirClassLikeDeclaration): Boolean {
        val symbol = declaration.symbol as? FirClassSymbol<*> ?: return false
        if (!symbol.isCompanion) {
            return false
        }

        val entity = symbol.getContainingClass(session) ?: return false
        return session.rhizomedbPredicateMatcher.isEntityTypeAnnotated(entity)
    }

    override fun computeAdditionalSupertypes(
        classLikeDeclaration: FirClassLikeDeclaration,
        resolvedSupertypes: List<FirResolvedTypeRef>,
        typeResolver: TypeResolveService,
    ): List<FirResolvedTypeRef> {
        val symbol = classLikeDeclaration.symbol as? FirClassSymbol<*> ?: return emptyList()
        val entity = symbol.getContainingClass(session) ?: return emptyList()
        val def = RhizomedbSymbolNames.entityTypeClassId.constructClassLikeType(arrayOf(entity.defaultType()), false)
        return listOf(def.toFirResolvedTypeRef())
    }
}

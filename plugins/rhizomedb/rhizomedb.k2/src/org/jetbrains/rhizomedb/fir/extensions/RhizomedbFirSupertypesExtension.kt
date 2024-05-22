/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.rhizomedb.fir.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.utils.expandedConeType
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.extensions.ExperimentalSupertypesGenerationApi
import org.jetbrains.kotlin.fir.extensions.FirSupertypeGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirSupertypeGenerationExtension.TypeResolveService
import org.jetbrains.kotlin.fir.resolve.SupertypeSupplier
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.getSuperTypes
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.ClassId
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
        if (resolvedSupertypes.any { it.isEntityType(typeResolver) }) {
            // Already EntityType subclass
            return emptyList()
        }
        return computeEntityTypeSupertypes(classLikeDeclaration, typeResolver)
    }

    @ExperimentalSupertypesGenerationApi
    override fun computeAdditionalSupertypesForGeneratedNestedClass(
        klass: FirRegularClass,
        typeResolver: TypeResolveService,
    ): List<FirResolvedTypeRef> = computeEntityTypeSupertypes(klass, typeResolver)

    private fun computeEntityTypeSupertypes(
        classLikeDeclaration: FirClassLikeDeclaration,
        typeResolver: TypeResolveService,
    ): List<FirResolvedTypeRef> {
        val symbol = classLikeDeclaration.symbol as? FirClassSymbol<*> ?: return emptyList()
        val entity = symbol.getContainingClass(session) ?: return emptyList()

        if (!entity.isEntity(typeResolver)) {
            // Do not modify a non-entity companion
            return emptyList()
        }

        val def = RhizomedbSymbolNames.entityTypeClassId.constructClassLikeType(arrayOf(entity.defaultType()), false)
        return listOf(def.toFirResolvedTypeRef())
    }

    private fun FirClassLikeSymbol<*>.isEntity(typeResolver: TypeResolveService): Boolean {
        return isSubclassOf(RhizomedbSymbolNames.entityClassId, typeResolver)
    }

    private fun FirResolvedTypeRef.isEntityType(typeResolver: TypeResolveService): Boolean {
        return isSubclassOf(RhizomedbSymbolNames.entityTypeClassId, typeResolver)
    }

    private fun FirClassLikeSymbol<*>.isSubclassOf(classId: ClassId, typeResolver: TypeResolveService): Boolean {
        return getSuperTypes(session, supertypeSupplier = typeResolveSupplier(typeResolver)).any {
            it.classId == classId
        }
    }

    private fun FirResolvedTypeRef.isSubclassOf(classId: ClassId, typeResolver: TypeResolveService): Boolean {
        return type.classId == classId || type.toClassSymbol(session)?.isSubclassOf(classId, typeResolver) ?: false
    }
}

private fun typeResolveSupplier(typeResolver: TypeResolveService): SupertypeSupplier = object : SupertypeSupplier() {
    override fun forClass(firClass: FirClass, useSiteSession: FirSession): List<ConeClassLikeType> {
        return firClass.superTypeRefs.mapNotNull { ref ->
            ref.coneTypeSafe<ConeClassLikeType>() ?: (ref as? FirUserTypeRef)?.let {
                typeResolver.resolveUserType(it).type as? ConeClassLikeType
            }
        }
    }

    override fun expansionForTypeAlias(typeAlias: FirTypeAlias, useSiteSession: FirSession): ConeClassLikeType? {
        return typeAlias.expandedConeType ?: (typeAlias.expandedTypeRef as? FirUserTypeRef)?.let {
            typeResolver.resolveUserType(it).type as? ConeClassLikeType
        }
    }
}

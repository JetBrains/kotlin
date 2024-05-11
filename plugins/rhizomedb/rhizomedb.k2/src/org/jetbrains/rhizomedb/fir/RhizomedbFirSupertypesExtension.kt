/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.rhizomedb.fir

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.utils.expandedConeType
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.extensions.FirSupertypeGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirSupertypeGenerationExtension.TypeResolveService
import org.jetbrains.kotlin.fir.references.impl.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.SupertypeSupplier
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.getContainingDeclaration
import org.jetbrains.kotlin.fir.resolve.getSuperTypes
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.rhizomedb.fir.resolve.RhizomedbSymbolNames
import org.jetbrains.rhizomedb.fir.services.rhizomedbEntityPredicateMatcher

class RhizomedbFirSupertypesExtension(session: FirSession) : FirSupertypeGenerationExtension(session) {
    override fun needTransformSupertypes(declaration: FirClassLikeDeclaration): Boolean {
        val symbol = declaration.symbol as? FirClassSymbol<*> ?: return false
        if (!symbol.isCompanion) {
            return false
        }

        val entity = symbol.getContainingClass(session) ?: return false
        return entity.classKind == ClassKind.CLASS && session.rhizomedbEntityPredicateMatcher.isEntityTypeAnnotated(entity)
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

    private val FirPropertyAccessExpression.qualifierName: Name?
        get() = (calleeReference as? FirSimpleNamedReference)?.name
}

fun typeResolveSupplier(typeResolver: TypeResolveService): SupertypeSupplier = object : SupertypeSupplier() {
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

fun FirClassLikeSymbol<*>.getContainingClass(session: FirSession): FirClassSymbol<*>? =
    getContainingDeclaration(session) as? FirClassSymbol

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.rhizomedb.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.typeParameterSymbols
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.utils.expandedConeType
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.FirSupertypeGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirSupertypeGenerationExtension.TypeResolveService
import org.jetbrains.kotlin.fir.extensions.buildUserTypeFromQualifierParts
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.references.impl.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.SupertypeSupplier
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.getContainingDeclaration
import org.jetbrains.kotlin.fir.resolve.getSuperTypes
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.rhizomedb.fir.resolve.RhizomedbSymbolNames

class RhizomedbFirSupertypesExtension(session: FirSession) : FirSupertypeGenerationExtension(session) {
    override fun needTransformSupertypes(declaration: FirClassLikeDeclaration): Boolean {
        val symbol = declaration.symbol as? FirClassSymbol<*> ?: return false
        if (!symbol.isCompanion) {
            return false
        }

        val entity = symbol.getContainingDeclaration(session) as? FirClassSymbol ?: return false
        return session.predicateBasedProvider.matches(RhizomedbFirPredicates.annotatedWithEntityType, entity)
    }

    override fun computeAdditionalSupertypes(
        classLikeDeclaration: FirClassLikeDeclaration,
        resolvedSupertypes: List<FirResolvedTypeRef>,
        typeResolver: TypeResolveService,
    ): List<FirResolvedTypeRef> {
        if (resolvedSupertypes.any { it.type.classId == RhizomedbSymbolNames.entityTypeClassId }) {
            return emptyList()
        }

        val symbol = classLikeDeclaration.symbol as? FirClassSymbol<*> ?: return emptyList()
        val entity = symbol.getContainingDeclaration(session) as? FirClassSymbol ?: return emptyList()

        val supertypes = entity.getSuperTypes(session, supertypeSupplier = typeResolveSupplier(typeResolver))
        if (supertypes.all { it.lookupTag.classId != RhizomedbSymbolNames.entityClassId }) {
            return emptyList()
        }

        val def = RhizomedbSymbolNames.entityTypeClassId.constructClassLikeType(arrayOf(entity.defaultType()), false)
        val annotation = entity.entityTypeAnnotation(session) ?: return emptyList()

        val getClassArgument = (annotation as? FirAnnotationCall)?.arguments?.singleOrNull() as? FirGetClassCall

        val typeToResolve = getClassArgument?.let {
            buildUserTypeFromQualifierParts(isMarkedNullable = false) {
                fun visitQualifiers(expression: FirExpression) {
                    if (expression !is FirPropertyAccessExpression) return
                    expression.explicitReceiver?.let { visitQualifiers(it) }
                    expression.qualifierName?.let { part(it) }
                }
                visitQualifiers(getClassArgument.argument)
            }
        }

        val resolvedArgument = typeToResolve?.let {
            typeResolver.resolveUserType(typeToResolve).type
        } ?: def

        val entityTypeSymbol = resolvedArgument.toSymbol(session)!!
        val finalType = if (entityTypeSymbol.typeParameterSymbols?.isNotEmpty() == true) {
            resolvedArgument.classId!!.constructClassLikeType(arrayOf(entity.defaultType()))
        } else {
            resolvedArgument
        }

        return listOf(finalType.toFirResolvedTypeRef())
    }

    private val FirPropertyAccessExpression.qualifierName: Name?
        get() = (calleeReference as? FirSimpleNamedReference)?.name

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(RhizomedbFirPredicates.annotatedWithEntityType)
    }
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

/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.containingClass
import org.jetbrains.kotlin.fir.declarations.FirAnnotatedDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirPluginKey
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.extensions.FirStatusTransformerExtension
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.predicate.hasOrUnder
import org.jetbrains.kotlin.fir.extensions.transform
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class AllOpenVisibilityTransformer(session: FirSession) : FirStatusTransformerExtension(session) {
    companion object {
        private val AllPublicClassId = ClassId(FqName("org.jetbrains.kotlin.fir.plugin"), Name.identifier("AllPublic"))
        private val VisibilityClassId = ClassId(FqName("org.jetbrains.kotlin.fir.plugin"), Name.identifier("Visibility"))

        private val PublicName = Name.identifier("Public")
        private val InternalName = Name.identifier("Internal")
        private val PrivateName = Name.identifier("Private")
        private val ProtectedName = Name.identifier("Protected")
    }

    override fun transformStatus(
        declaration: FirDeclaration,
        owners: List<FirAnnotatedDeclaration>,
        status: FirDeclarationStatus
    ): FirDeclarationStatus {
        val visibility = findVisibility(declaration, owners) ?: return status
        if (visibility == status.visibility) return status
        return status.transform(visibility = visibility)
    }

    private fun findVisibility(declaration: FirDeclaration, owners: List<FirAnnotatedDeclaration>): Visibility? {
        (declaration as? FirAnnotatedDeclaration)?.visibilityFromAnnotation()?.let { return it }
        for (owner in owners) {
            owner.visibilityFromAnnotation()?.let { return it }
        }
        return null
    }

    private fun FirAnnotatedDeclaration.visibilityFromAnnotation(): Visibility? {
        val annotation = annotations.firstOrNull {
            it.annotationTypeRef.coneTypeSafe<ConeClassLikeType>()?.lookupTag?.classId == AllPublicClassId
        } ?: return null
        val argument = annotation.arguments.firstOrNull() as? FirQualifiedAccessExpression ?: return null
        val symbol = (argument.calleeReference as? FirResolvedNamedReference)?.resolvedSymbol as? FirVariableSymbol<*> ?: return null
        val name = symbol.takeIf { it.containingClass()?.classId == VisibilityClassId }?.callableId?.callableName ?: return null
        return when (name) {
            PublicName -> Visibilities.Public
            InternalName -> Visibilities.Internal
            PrivateName -> Visibilities.Private
            ProtectedName -> Visibilities.Protected
            else -> null
        }
    }

    override val predicate: DeclarationPredicate = hasOrUnder(AllPublicClassId.asSingleFqName())

    override val key: FirPluginKey
        get() = AllOpenPluginKey
}

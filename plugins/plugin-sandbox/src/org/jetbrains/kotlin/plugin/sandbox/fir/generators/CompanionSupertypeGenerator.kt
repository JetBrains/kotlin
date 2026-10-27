/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.plugin.sandbox.fir.generators

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.FirSupertypeGenerationExtension
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.resolve.getContainingDeclaration
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.name.ClassId

/**
 * The [FirSupertypeGenerationExtension] version of [CompanionInfoGenerator].
 * Adds the supertypes from the `spec` argument of the [annotationClassId] annotation
 * to the source-declared companion object of the annotated class.
 * Annotations from the `spec` argument are ignored.
 */
abstract class CompanionSupertypeGenerator(
    session: FirSession,
    private val annotationClassId: ClassId,
) : FirSupertypeGenerationExtension(session) {
    private val annotationPredicate = LookupPredicate.create { annotated(annotationClassId.asSingleFqName()) }

    override fun needTransformSupertypes(declaration: FirClassLikeDeclaration): Boolean =
        findAnnotatedOwner(declaration) != null

    override fun computeAdditionalSupertypes(
        classLikeDeclaration: FirClassLikeDeclaration,
        resolvedSupertypes: List<FirResolvedTypeRef>,
        typeResolver: TypeResolveService,
    ): List<ConeKotlinType> {
        val owner = findAnnotatedOwner(classLikeDeclaration) ?: return emptyList()
        return parseCompanionSpecification(owner, annotationClassId).superTypes
    }

    private fun findAnnotatedOwner(declaration: FirClassLikeDeclaration): FirClassSymbol<*>? {
        if (declaration !is FirRegularClass || !declaration.isCompanion) return null
        val owner = declaration.getContainingDeclaration(session)?.symbol as? FirClassSymbol<*> ?: return null
        return owner.takeIf { session.predicateBasedProvider.matches(annotationPredicate, it) }
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(annotationPredicate)
    }
}

class AddToCompanionSupertypeGenerator(session: FirSession) : CompanionSupertypeGenerator(
    session,
    ClassId.fromString("org/jetbrains/kotlin/plugin/sandbox/AddToCompanion"),
)

class AlsoAddToCompanionSupertypeGenerator(session: FirSession) : CompanionSupertypeGenerator(
    session,
    ClassId.fromString("org/jetbrains/kotlin/plugin/sandbox/AlsoAddToCompanion"),
)

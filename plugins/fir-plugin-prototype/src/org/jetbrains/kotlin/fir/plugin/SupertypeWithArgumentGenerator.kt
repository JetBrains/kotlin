/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.FirSupertypeGenerationExtension
import org.jetbrains.kotlin.fir.extensions.buildUserTypeFromQualifierParts
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.references.impl.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/*
 * Adds MyInterface supertype for all classes annotated with @MyInterfaceSupertype
 */
class SupertypeWithArgumentGenerator(session: FirSession) : FirSupertypeGenerationExtension(session) {
    companion object {
        private val supertypeClassId = ClassId(FqName("foo"), Name.identifier("InterfaceWithArgument"))
        private val annotationClassId = ClassId.topLevel("SupertypeWithTypeArgument".fqn())
        private val PREDICATE = DeclarationPredicate.create { annotated(annotationClassId.asSingleFqName()) }

    }

    context(TypeResolveServiceContainer)
    @Suppress("IncorrectFormatting") // KTIJ-22227
    override fun computeAdditionalSupertypes(
        classLikeDeclaration: FirClassLikeDeclaration,
        resolvedSupertypes: List<FirResolvedTypeRef>
    ): List<FirResolvedTypeRef> {
        if (resolvedSupertypes.any { it.type.classId == supertypeClassId }) return emptyList()

        val annotation = classLikeDeclaration.getAnnotationByClassId(annotationClassId, session) ?: return emptyList()
        val getClassArgument = (annotation as? FirAnnotationCall)?.argument as? FirGetClassCall ?: return emptyList()

        val typeToResolve = buildUserTypeFromQualifierParts(isMarkedNullable = false) {
            fun visitQualifiers(expression: FirExpression) {
                if (expression !is FirPropertyAccessExpression) return
                expression.explicitReceiver?.let { visitQualifiers(it) }
                expression.qualifierName?.let { part(it) }
            }
            visitQualifiers(getClassArgument.argument)
        }

        val resolvedArgument = typeResolver.resolveUserType(typeToResolve).type

        return listOf(
            buildResolvedTypeRef {
                type = supertypeClassId.constructClassLikeType(arrayOf(resolvedArgument), isNullable = false)
            }
        )
    }

    private val FirPropertyAccessExpression.qualifierName: Name?
        get() = (calleeReference as? FirSimpleNamedReference)?.name

    override fun needTransformSupertypes(declaration: FirClassLikeDeclaration): Boolean {
        return session.predicateBasedProvider.matches(PREDICATE, declaration)
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(PREDICATE)
    }
}

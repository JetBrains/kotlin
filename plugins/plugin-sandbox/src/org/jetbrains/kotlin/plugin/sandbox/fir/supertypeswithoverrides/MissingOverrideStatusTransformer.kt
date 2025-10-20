/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.plugin.sandbox.fir.supertypeswithoverrides

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.copy
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirClassReferenceExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirGetClassCall
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.FirStatusTransformerExtension
import org.jetbrains.kotlin.fir.extensions.FirSupertypeGenerationExtension
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.extensions.typeFromQualifierParts
import org.jetbrains.kotlin.fir.references.impl.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.getSuperTypes
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeOrNull
import org.jetbrains.kotlin.fir.types.isAny
import org.jetbrains.kotlin.fir.types.isResolved
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Adds a missing `isOverride` to non-overridden functions that match a member function of a contributed supertype.
 *
 */
class MissingOverrideStatusTransformer(session: FirSession) : FirStatusTransformerExtension(session) {
    // For convenience, we just run this in on properties in
    override fun needTransformStatus(declaration: FirDeclaration): Boolean {
        return when (val symbol = declaration.symbol) {
            is FirNamedFunctionSymbol -> {
                symbol.getContainingClassSymbol()?.let { classSymbol ->
                    if (classSymbol !is FirRegularClassSymbol) return false
                    session.predicateBasedProvider.matches(SimpleAddSupertypeExtension.PREDICATE, classSymbol)
                } ?: false
            }
            else -> false
        }
    }

    override fun transformStatus(
        status: FirDeclarationStatus,
        function: FirNamedFunction,
        containingClass: FirClassLikeSymbol<*>?,
        isLocal: Boolean,
    ): FirDeclarationStatus {
        containingClass ?: return status

        // Already declared as an override
        if (status.isOverride) return status

        // Read functions from contributed supertypes
        for (supertype in containingClass.getSuperTypes(session)) {
            if (supertype.isAny) continue
            var needsOverride = false
            supertype.toClassSymbol(session)?.processAllDeclaredCallables(session) { callable ->
                if (needsOverride) return@processAllDeclaredCallables
                // For the sake of this test's simplicity, we only check for matching names
                if (callable is FirNamedFunctionSymbol && callable.name == function.name) {
                    needsOverride = true
                }
            }
            if (needsOverride) {
                return status.copy(isOverride = true)
            }
        }

        return super.transformStatus(status, function, containingClass, isLocal)
    }
}

/**
 * A simple extension that adds supertypes as designated by the test `@AddSupertype` annotation.
 *
 * @see MissingOverrideStatusTransformer
 */
class SimpleAddSupertypeExtension(session: FirSession): FirSupertypeGenerationExtension(session) {
    companion object {
        private val FQ_NAME = FqName("org.jetbrains.kotlin.plugin.sandbox.AddSupertype")
        internal val PREDICATE = DeclarationPredicate.create {
            annotated(FQ_NAME)
        }
    }

    override fun needTransformSupertypes(declaration: FirClassLikeDeclaration): Boolean {
        return when (declaration.symbol) {
            is FirRegularClassSymbol -> session.predicateBasedProvider.matches(PREDICATE, declaration)
            else -> false
        }
    }

    override fun computeAdditionalSupertypes(
        classLikeDeclaration: FirClassLikeDeclaration,
        resolvedSupertypes: List<FirResolvedTypeRef>,
        typeResolver: TypeResolveService,
    ): List<ConeKotlinType> {
        val annotation = classLikeDeclaration.annotations.singleOrNull() ?: return emptyList()
        val argument = annotation.findArgumentByName(StandardNames.DEFAULT_VALUE_PARAMETER) ?: return emptyList()
        check(argument is FirGetClassCall)
        val typeToAdd = argument.resolvedClassArgumentTarget(typeResolver) ?: return emptyList()
        return listOf(typeToAdd)
    }

    private fun FirGetClassCall.resolvedClassArgumentTarget(
        typeResolver: TypeResolveService
    ): ConeKotlinType? {
        if (isResolved) {
            return (argument as? FirClassReferenceExpression?)?.classTypeRef?.coneTypeOrNull
        }
        val source = source ?: return null

        return typeFromQualifierParts(isMarkedNullable = false, typeResolver, source) {
            fun visitQualifiers(expression: FirExpression) {
                if (expression !is FirPropertyAccessExpression) return
                expression.explicitReceiver?.let { visitQualifiers(it) }
                expression.qualifierName?.let { part(it) }
            }
            visitQualifiers(argument)
        }
    }

    private val FirPropertyAccessExpression.qualifierName: Name?
        get() = (calleeReference as? FirSimpleNamedReference)?.name


    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(PREDICATE)
    }
}

/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.common

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.predicate.annotated
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

abstract class AbstractMemberGenerationFirExtension(session: FirSession) : FirDeclarationGenerationExtension(session) {
    protected abstract val classAnnotationFqn: AnnotationFqn
    protected abstract val generatedFunctionNames: List<Name>
    protected abstract val generatedPropertyNames: List<Name>

    private val predicate: DeclarationPredicate by lazy {
        annotated(classAnnotationFqn)
    }

    private val matchedClasses by lazy {
        session.predicateBasedProvider.getSymbolsByPredicate(predicate).filterIsInstance<FirRegularClassSymbol>()
    }

    final override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>): Set<Name> {
        return when (classSymbol) {
            in matchedClasses -> (generatedFunctionNames + generatedPropertyNames).toSet()
            else -> setOf()
        }
    }

    open fun generateFunctions(owner: FirClassSymbol<*>, annotationCall: FirAnnotation): List<FirNamedFunctionSymbol> = emptyList()

    open fun generateProperties(owner: FirClassSymbol<*>, annotationCall: FirAnnotation): List<FirPropertySymbol> = emptyList()

    open fun FirDeclarationPredicateRegistrar.registerAdditionalPredicated() {}

    final override fun generateFunctions(callableId: CallableId, context: MemberGenerationContext?): List<FirNamedFunctionSymbol> {
        if (callableId.callableName !in generatedFunctionNames) return emptyList()
        require(context != null)

        val annotationCall = context.owner.getAnnotationByClassId(ClassId.topLevel(classAnnotationFqn))!!
        return generateFunctions(context.owner, annotationCall)
    }

    final override fun generateProperties(callableId: CallableId, context: MemberGenerationContext?): List<FirPropertySymbol> {
        if (callableId.callableName !in generatedPropertyNames) return emptyList()
        require(context != null)

        val annotationCall = context.owner.getAnnotationByClassId(ClassId.topLevel(classAnnotationFqn))!!
        return generateProperties(context.owner, annotationCall)
    }

    final override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(predicate)
    }
}

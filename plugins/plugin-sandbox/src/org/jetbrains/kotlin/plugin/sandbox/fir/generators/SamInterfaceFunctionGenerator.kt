/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.plugin.sandbox.fir.generators

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.plugin.sandbox.fir.fqn

class SamInterfaceFunctionGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    companion object {
        private val FOO_NAME = Name.identifier("foo")

        private val PREDICATE = LookupPredicate.create {
            annotated("GenerateSamInterfaceFunction".fqn())
        }
    }

    private val predicateBasedProvider = session.predicateBasedProvider

    private val matchedClasses by lazy {
        predicateBasedProvider.getSymbolsByPredicate(PREDICATE).filterIsInstance<FirRegularClassSymbol>()
    }

    override fun generateFunctions(callableId: CallableId, context: MemberGenerationContext?): List<FirNamedFunctionSymbol> {
        if (context == null) return emptyList()
        if (callableId.callableName != FOO_NAME) return emptyList()
        if (context.owner !in matchedClasses) return emptyList()

        val function = createMemberFunction(
            context.owner,
            SamInterfaceFunctionGeneratorKey,
            callableId.callableName,
            session.builtinTypes.unitType.coneType,
        ) {
            modality = Modality.ABSTRACT
        }

        return listOf(function.symbol)
    }

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        return if (classSymbol in matchedClasses) setOf(FOO_NAME) else emptySet()
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(PREDICATE)
    }

    data object SamInterfaceFunctionGeneratorKey : GeneratedDeclarationKey()
}

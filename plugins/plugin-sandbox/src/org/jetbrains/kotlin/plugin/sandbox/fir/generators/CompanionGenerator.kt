/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.plugin.sandbox.fir.generators

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.StandardTypes
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.plugin.sandbox.fir.fqn

/*
 * Generates companion object with fun foo(): Int for each class annotated with @CompanionWithFoo
 */
class CompanionGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    companion object {
        private val PREDICATE = LookupPredicate.create { annotated("CompanionWithFoo".fqn()) }
        private val FOO_NAME = Name.identifier("foo")
    }

    override fun generateFunctions(callableId: CallableId, context: MemberGenerationContext?): List<FirNamedFunctionSymbol> {
        val owner = context?.owner ?: return emptyList()
        if (callableId.callableName != FOO_NAME) return emptyList()
        val function = createMemberFunction(owner, CompanionGeneratorKey, callableId.callableName, StandardTypes.Int) {
            withGeneratedDefaultBody()
        }
        return listOf(function.symbol)
    }

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        if (classSymbol.classKind != ClassKind.OBJECT) return emptySet()
        if (classSymbol !is FirRegularClassSymbol) return emptySet()
        val classId = classSymbol.classId
        if (!classId.isNestedClass || classId.shortClassName != SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT) return emptySet()
        return setOf(FOO_NAME)
    }

    override fun needsCompanionObject(classSymbol: FirClassSymbol<*>, context: NestedClassGenerationContext): Boolean =
        session.predicateBasedProvider.matches(PREDICATE, classSymbol)

    data object CompanionGeneratorKey : GeneratedDeclarationKey()

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(PREDICATE)
    }
}

/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin.generators

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.plugin.createConstructor
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.plugin.createNestedClass
import org.jetbrains.kotlin.fir.plugin.fqn
import org.jetbrains.kotlin.fir.types.constructStarProjectedType
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

/*
 * For each class annotated with @NestedClassAndMaterializeMember generates
 *  - member fun materialize(): ClassName
 *  - nested class Nested with default constructor
 */
class AdditionalMembersGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    companion object {
        private val MATERIALIZE_NAME = Name.identifier("materialize")
        private val NESTED_NAME = Name.identifier("Nested")

        private val PREDICATE = LookupPredicate.create { annotated("NestedClassAndMaterializeMember".fqn()) }
    }

    private val predicateBasedProvider = session.predicateBasedProvider
    private val matchedClasses by lazy {
        predicateBasedProvider.getSymbolsByPredicate(PREDICATE).filterIsInstance<FirRegularClassSymbol>()
    }

    private val nestedClassIds by lazy {
        matchedClasses.map { it.classId.createNestedClassId(NESTED_NAME) }
    }

    override fun generateFunctions(callableId: CallableId, context: MemberGenerationContext?): List<FirNamedFunctionSymbol> {
        if (callableId.callableName != MATERIALIZE_NAME) return emptyList()
        if (context == null) return emptyList()
        val matchedClassSymbol = matchedClasses.firstOrNull { it == context.owner } ?: return emptyList()
        val function = createMemberFunction(context.owner, Key, callableId.callableName, matchedClassSymbol.constructStarProjectedType())
        return listOf(function.symbol)
    }

    override fun generateNestedClassLikeDeclaration(
        owner: FirClassSymbol<*>,
        name: Name,
        context: NestedClassGenerationContext
    ): FirClassLikeSymbol<*>? {
        if (matchedClasses.none { it == owner }) return null
        return createNestedClass(owner, name, Key).symbol
    }

    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
        val createConstructor = createConstructor(context.owner, Key, generateDelegatedNoArgConstructorCall = true)
        return listOf(createConstructor.symbol)
    }

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        return when {
            classSymbol in matchedClasses -> setOf(MATERIALIZE_NAME)
            classSymbol.classId in nestedClassIds -> setOf(SpecialNames.INIT)
            else -> emptySet()
        }
    }

    override fun getNestedClassifiersNames(classSymbol: FirClassSymbol<*>, context: NestedClassGenerationContext): Set<Name> {
        return if (classSymbol in matchedClasses) setOf(NESTED_NAME) else emptySet()
    }

    object Key : GeneratedDeclarationKey() {
        override fun toString(): String {
            return "AllOpenMembersGeneratorKey"
        }
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(PREDICATE)
    }
}

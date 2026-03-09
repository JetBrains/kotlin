/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.plugin.sandbox.fir.generators

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.plugin.createCompanionObject
import org.jetbrains.kotlin.fir.plugin.createDefaultPrivateConstructor
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.symbols.impl.*
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

    override fun generateNestedClassLikeDeclaration(
        owner: FirClassSymbol<*>, name: Name, context: NestedClassGenerationContext,
    ): FirClassLikeSymbol<*>? {
        if (name != SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT) return null
        return createCompanionObject(owner, CompanionGeneratorKey).symbol
    }

    override fun generateFunctions(callableId: CallableId, context: MemberGenerationContext?): List<FirNamedFunctionSymbol> {
        val owner = context?.owner ?: return emptyList()
        val ownerKey = (owner.origin as? FirDeclarationOrigin.Plugin)?.key ?: return emptyList()
        if (ownerKey != CompanionGeneratorKey) return emptyList()
        if (callableId.callableName != FOO_NAME) return emptyList()
        val function = createMemberFunction(owner, CompanionGeneratorKey, callableId.callableName, session.builtinTypes.intType.coneType) {
            withGeneratedDefaultBody()
        }
        return listOf(function.symbol)
    }

    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
        val constructor = createDefaultPrivateConstructor(context.owner, CompanionGeneratorKey)
        return listOf(constructor.symbol)
    }

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        if (classSymbol.classKind != ClassKind.OBJECT) return emptySet()
        if (classSymbol !is FirRegularClassSymbol) return emptySet()
        val classId = classSymbol.classId
        if (!classId.isNestedClass || classId.shortClassName != SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT) return emptySet()
        val origin = classSymbol.origin as? FirDeclarationOrigin.Plugin
        return if (origin?.key == CompanionGeneratorKey) {
            setOf(FOO_NAME, SpecialNames.INIT)
        } else {
            setOf(FOO_NAME)
        }
    }

    override fun getNestedClassifiersNames(classSymbol: FirClassSymbol<*>, context: NestedClassGenerationContext): Set<Name> {
        return if (session.predicateBasedProvider.matches(PREDICATE, classSymbol)) {
            setOf(SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT)
        } else {
            emptySet()
        }
    }

    data object CompanionGeneratorKey : GeneratedDeclarationKey()

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(PREDICATE)
    }
}

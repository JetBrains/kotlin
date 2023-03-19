/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin.generators

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.plugin.*
import org.jetbrains.kotlin.fir.resolve.providers.getRegularClassSymbolByClassId
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.types.constructStarProjectedType
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.*

/*
 * Generates class /foo.AllOpenGenerated with
 *  - empty public constructor
 *  - testClassName() functions for all classes annotated with @ExternalClassWithNested
 *  - NestedClassName nested classes for all classes annotated with @ExternalClassWithNested
 *  - function `materialize: ClassName` in those nested classes
 *
 * If there are no annotated classes then AllOpenGenerated class is not generated
 */
class ExternalClassGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    companion object {
        private val FOO_PACKAGE = FqName.topLevel(Name.identifier("foo"))
        private val GENERATED_CLASS_ID = ClassId(FOO_PACKAGE, Name.identifier("AllOpenGenerated"))
        private val MATERIALIZE_NAME = Name.identifier("materialize")

        private val PREDICATE = LookupPredicate.create { annotated("ExternalClassWithNested".fqn()) }
    }

    object Key : GeneratedDeclarationKey() {
        override fun toString(): String {
            return "AllOpenClassGeneratorKey"
        }
    }

    private val predicateBasedProvider = session.predicateBasedProvider
    private val matchedClasses by lazy {
        predicateBasedProvider.getSymbolsByPredicate(PREDICATE).filterIsInstance<FirRegularClassSymbol>()
    }
    private val classIdsForMatchedClasses: Map<ClassId, FirRegularClassSymbol> by lazy {
        matchedClasses.associateBy {
            GENERATED_CLASS_ID.createNestedClassId(Name.identifier("Nested${it.classId.shortClassName}"))
        }
    }

    override fun generateTopLevelClassLikeDeclaration(classId: ClassId): FirClassLikeSymbol<*>? {
        if (classId != GENERATED_CLASS_ID) return null
        if (matchedClasses.isEmpty()) return null
        return createTopLevelClass(classId, Key).symbol
    }

    override fun generateNestedClassLikeDeclaration(
        owner: FirClassSymbol<*>,
        name: Name,
        context: NestedClassGenerationContext
    ): FirClassLikeSymbol<*>? {
        return when (val origin = owner.origin) {
            is FirDeclarationOrigin.Plugin -> when (origin.key) {
                Key -> generateNestedClass(owner.classId.createNestedClassId(name), owner)
                else -> null
            }
            else -> null
        }
    }

    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
        val classId = context.owner.classId
        if (classId != GENERATED_CLASS_ID && classId !in classIdsForMatchedClasses) return emptyList()
        return listOf(createConstructor(context.owner, Key).symbol)
    }

    private fun generateNestedClass(classId: ClassId, owner: FirClassSymbol<*>): FirClassLikeSymbol<*>? {
        if (owner.classId != GENERATED_CLASS_ID) return null
        val matchedClass = classIdsForMatchedClasses[classId] ?: return null

        return createNestedClass(owner, classId.shortClassName, Key).also {
            it.matchedClass = matchedClass.classId
        }.symbol
    }

    override fun generateFunctions(callableId: CallableId, context: MemberGenerationContext?): List<FirNamedFunctionSymbol> {
        if (callableId.classId !in classIdsForMatchedClasses || callableId.callableName != MATERIALIZE_NAME) return emptyList()
        val owner = context?.owner
        require(owner is FirRegularClassSymbol)
        val matchedClassId = owner.matchedClass ?: return emptyList()
        val matchedClassSymbol = session.symbolProvider.getRegularClassSymbolByClassId(matchedClassId) ?: return emptyList()
        val function = createMemberFunction(owner, Key, callableId.callableName, matchedClassSymbol.constructStarProjectedType())
        return listOf(function.symbol)
    }

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        return when (classSymbol.classId) {
            in classIdsForMatchedClasses -> setOf(MATERIALIZE_NAME, SpecialNames.INIT)
            GENERATED_CLASS_ID -> setOf(SpecialNames.INIT)
            else -> emptySet()
        }
    }

    override fun getNestedClassifiersNames(classSymbol: FirClassSymbol<*>, context: NestedClassGenerationContext): Set<Name> {
        return if (classSymbol.classId == GENERATED_CLASS_ID) {
            return classIdsForMatchedClasses.keys.mapTo(mutableSetOf()) { it.shortClassName }
        } else {
            emptySet()
        }
    }

    override fun getTopLevelClassIds(): Set<ClassId> {
        return if (matchedClasses.isEmpty()) emptySet() else setOf(GENERATED_CLASS_ID)
    }

    override fun hasPackage(packageFqName: FqName): Boolean {
        return packageFqName == FOO_PACKAGE
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(PREDICATE)
    }
}

private object MatchedClassAttributeKey : FirDeclarationDataKey()

private var FirRegularClass.matchedClass: ClassId? by FirDeclarationDataRegistry.data(MatchedClassAttributeKey)
private val FirRegularClassSymbol.matchedClass: ClassId? by FirDeclarationDataRegistry.symbolAccessor(MatchedClassAttributeKey)


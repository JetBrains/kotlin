/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin.generators

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirPluginKey
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.predicate.has
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.plugin.fqn
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

/*
 * For each class annotated with @C generates
 *  - member fun materialize(): ClassName
 *  - nested class Nested with default constructor
 */
class AdditionalMembersGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    companion object {
        private val MATERIALIZE_NAME = Name.identifier("materialize")
        private val NESTED_NAME = Name.identifier("Nested")

        private val PREDICATE: DeclarationPredicate = has("C".fqn())
    }

    private val predicateBasedProvider = session.predicateBasedProvider
    private val matchedClasses by lazy {
        predicateBasedProvider.getSymbolsByPredicate(PREDICATE).filterIsInstance<FirRegularClassSymbol>()
    }

    override fun generateFunctions(callableId: CallableId, owner: FirClassSymbol<*>?): List<FirNamedFunctionSymbol> {
        if (callableId.callableName != MATERIALIZE_NAME) return emptyList()
        val classId = callableId.classId ?: return emptyList()
        val matchedClassSymbol = matchedClasses.firstOrNull { it.classId == classId } ?: return emptyList()
        return listOf(buildMaterializeFunction(matchedClassSymbol, callableId).symbol)
    }

    override fun generateClassLikeDeclaration(classId: ClassId): FirClassLikeSymbol<*>? {
        if (classId.shortClassName != NESTED_NAME) return null
        val parentClassId = classId.parentClassId ?: return null
        if (matchedClasses.none { it.classId == parentClassId }) return null
        return buildRegularClass {
            moduleData = session.moduleData
            origin = key.origin
            classKind = ClassKind.CLASS
            scopeProvider = session.kotlinScopeProvider
            status = FirResolvedDeclarationStatusImpl(Visibilities.Public, Modality.FINAL, EffectiveVisibility.Public)
            name = classId.shortClassName
            symbol = FirRegularClassSymbol(classId)
            superTypeRefs += session.builtinTypes.anyType
        }.symbol
    }

    override fun generateConstructors(callableId: CallableId): List<FirConstructorSymbol> {
        val classId = callableId.classId ?: return emptyList()
        if (callableId.callableName != NESTED_NAME) return emptyList()
        if (matchedClasses.none { it.classId == classId }) return emptyList()
        return listOf(buildConstructor(classId.createNestedClassId(NESTED_NAME), callableId, isInner = false).symbol)
    }

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>): Set<Name> {
        return if (classSymbol in matchedClasses) setOf(MATERIALIZE_NAME) else emptySet()
    }

    override fun getNestedClassifiersNames(classSymbol: FirClassSymbol<*>): Set<Name> {
        return if (classSymbol in matchedClasses) setOf(NESTED_NAME) else emptySet()
    }

    object Key : FirPluginKey() {
        override fun toString(): String {
            return "AllOpenMembersGeneratorKey"
        }
    }

    override val key: FirPluginKey
        get() = Key

    override fun needToGenerateAdditionalMembersInClass(klass: FirClass): Boolean {
        return session.predicateBasedProvider.matches(PREDICATE, klass)
    }

    override fun needToGenerateNestedClassifiersInClass(klass: FirClass): Boolean {
        return session.predicateBasedProvider.matches(PREDICATE, klass)
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(PREDICATE)
    }
}

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
import org.jetbrains.kotlin.fir.declarations.FirPluginKey
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunction
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.predicate.has
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.plugin.fqn
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

/*
 * Generates companion object with fun foo(): Int for each class annotated with @E
 */
class CompanionGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    companion object {
        private val PREDICATE = has("E".fqn())
        private val FOO_NAME = Name.identifier("foo")
    }

    private val matchedClasses by lazy {
        session.predicateBasedProvider.getSymbolsByPredicate(PREDICATE)
            .filterIsInstance<FirRegularClassSymbol>()
    }

    override fun generateClassLikeDeclaration(classId: ClassId): FirClassLikeSymbol<*>? {
        if (classId.shortClassName != SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT) return null
        val owner = matchedClasses.firstOrNull { it.classId == classId.outerClassId } ?: return null
        if (owner.companionObjectSymbol != null) return null
        val regularClass = buildRegularClass {
            moduleData = session.moduleData
            origin = Key.origin
            classKind = ClassKind.OBJECT
            scopeProvider = session.kotlinScopeProvider
            status = FirResolvedDeclarationStatusImpl(
                Visibilities.Public,
                Modality.FINAL,
                EffectiveVisibility.Public
            )
            name = SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT
            symbol = FirRegularClassSymbol(classId)
            superTypeRefs += session.builtinTypes.anyType
        }
        return regularClass.symbol
    }

    override fun generateFunctions(callableId: CallableId, owner: FirClassSymbol<*>?): List<FirNamedFunctionSymbol> {
        if (owner == null || owner.origin != Key.origin) return emptyList()
        if (callableId.callableName != FOO_NAME) return emptyList()
        val function = buildSimpleFunction {
            moduleData = session.moduleData
            origin = Key.origin
            status = FirResolvedDeclarationStatusImpl(
                Visibilities.Public,
                Modality.FINAL,
                EffectiveVisibility.Public
            )
            name = FOO_NAME
            symbol = FirNamedFunctionSymbol(callableId)
            returnTypeRef = session.builtinTypes.intType
            dispatchReceiverType = owner.defaultType()
        }
        return listOf(function.symbol)
    }

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>): Set<Name> {
        return setOf(FOO_NAME)
    }

    @OptIn(SymbolInternals::class)
    override fun getNestedClassifiersNames(classSymbol: FirClassSymbol<*>): Set<Name> {
        return if (session.predicateBasedProvider.matches(PREDICATE, classSymbol.fir)) {
            setOf(SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT)
        } else {
            emptySet()
        }
    }

    override val key: FirPluginKey
        get() = Key

    object Key : FirPluginKey() {
        override fun toString(): String {
            return "CompanionGeneratorKey"
        }
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(PREDICATE)
    }
}

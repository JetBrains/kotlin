/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin.generators

import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.origin
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.predicate.annotated
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.plugin.fqn
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

/*
 * Generates `dummyClassName(value: ClassName): String` function for each class annotated with @DummyFunction
 */
class TopLevelDeclarationsGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    companion object {
        private val PREDICATE: DeclarationPredicate = annotated("DummyFunction".fqn())
    }

    private val predicateBasedProvider = session.predicateBasedProvider
    private val matchedClasses by lazy {
        predicateBasedProvider.getSymbolsByPredicate(PREDICATE).filterIsInstance<FirRegularClassSymbol>()
    }

    override fun generateFunctions(callableId: CallableId, context: MemberGenerationContext?): List<FirNamedFunctionSymbol> {
        if (context != null) return emptyList()
        val matchedClassSymbol = findMatchedClassForFunction(callableId) ?: return emptyList()
        val function = buildSimpleFunction {
            resolvePhase = FirResolvePhase.BODY_RESOLVE
            moduleData = session.moduleData
            origin = Key.origin
            status = FirResolvedDeclarationStatusImpl(
                Visibilities.Public,
                Modality.FINAL,
                EffectiveVisibility.Public
            )
            returnTypeRef = session.builtinTypes.stringType
            valueParameters += buildValueParameter {
                resolvePhase = FirResolvePhase.BODY_RESOLVE
                moduleData = session.moduleData
                origin = Key.origin
                returnTypeRef = buildResolvedTypeRef {
                    type = ConeClassLikeTypeImpl(ConeClassLikeLookupTagImpl(matchedClassSymbol.classId), emptyArray(), isNullable = false)
                }
                name = Name.identifier("value")
                symbol = FirValueParameterSymbol(name)
                isCrossinline = false
                isNoinline = false
                isVararg = false
            }
            symbol = FirNamedFunctionSymbol(callableId)
            name = callableId.callableName
        }
        return listOf(function.symbol)
    }

    private fun findMatchedClassForFunction(callableId: CallableId): FirRegularClassSymbol? {
        // We generate only top-level functions
        if (callableId.classId != null) return null
        return matchedClasses
            .filter { it.classId.packageFqName == callableId.packageName }
            .firstOrNull { callableId.callableName.identifier == it.classId.toDummyCallableName() }
    }

    private fun ClassId.toDummyCallableName(): String {
        return "dummy${shortClassName.identifier}"
    }

    override fun getTopLevelCallableIds(): Set<CallableId> {
        return matchedClasses.mapTo(mutableSetOf()) {
            val classId = it.classId
            CallableId(classId.packageFqName, Name.identifier(classId.toDummyCallableName()))
        }
    }

    object Key : GeneratedDeclarationKey()

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(PREDICATE)
    }
}

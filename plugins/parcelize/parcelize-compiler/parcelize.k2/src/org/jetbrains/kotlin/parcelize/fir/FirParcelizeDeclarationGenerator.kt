/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.fir

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.plugin.SimpleFunctionBuildingContext
import org.jetbrains.kotlin.fir.plugin.createConeType
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.parcelize.ParcelizeNames.DESCRIBE_CONTENTS_NAME
import org.jetbrains.kotlin.parcelize.ParcelizeNames.DEST_NAME
import org.jetbrains.kotlin.parcelize.ParcelizeNames.FLAGS_NAME
import org.jetbrains.kotlin.parcelize.ParcelizeNames.OLD_PARCELIZE_FQN
import org.jetbrains.kotlin.parcelize.ParcelizeNames.PARCELIZE_FQN
import org.jetbrains.kotlin.parcelize.ParcelizeNames.PARCEL_ID
import org.jetbrains.kotlin.parcelize.ParcelizeNames.WRITE_TO_PARCEL_NAME
import org.jetbrains.kotlin.utils.addToStdlib.runIf

class FirParcelizeDeclarationGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    companion object {
        private val PREDICATE = LookupPredicate.create { annotated(PARCELIZE_FQN, OLD_PARCELIZE_FQN) }
        private val parcelizeMethodsNames = setOf(DESCRIBE_CONTENTS_NAME, WRITE_TO_PARCEL_NAME)
    }

    private val matchedClasses by lazy {
        session.predicateBasedProvider.getSymbolsByPredicate(PREDICATE)
            .filterIsInstance<FirRegularClassSymbol>()
    }

    override fun generateFunctions(callableId: CallableId, context: MemberGenerationContext?): List<FirNamedFunctionSymbol> {
        val owner = context?.owner ?: return emptyList()
        require(owner is FirRegularClassSymbol)
        val function = when (callableId.callableName) {
            DESCRIBE_CONTENTS_NAME -> {
                val hasDescribeContentImplementation = owner.hasDescribeContentsImplementation() ||
                        lookupSuperTypes(owner, lookupInterfaces = false, deep = true, session).any {
                            it.fullyExpandedType(session).toRegularClassSymbol(session)?.hasDescribeContentsImplementation() ?: false
                        }
                runIf(!hasDescribeContentImplementation) {
                    createMemberFunctionForParcelize(owner, callableId.callableName, session.builtinTypes.intType.type)
                }
            }
            WRITE_TO_PARCEL_NAME -> {
                val declaredFunctions = owner.declarationSymbols.filterIsInstance<FirNamedFunctionSymbol>()
                runIf(declaredFunctions.none { it.isWriteToParcel() }) {
                    createMemberFunctionForParcelize(owner, callableId.callableName, session.builtinTypes.unitType.type) {
                        valueParameter(DEST_NAME, PARCEL_ID.createConeType(session))
                        valueParameter(FLAGS_NAME, session.builtinTypes.intType.type)
                    }
                }
            }
            else -> null
        } ?: return emptyList()
        return listOf(function.symbol)
    }

    private fun FirRegularClassSymbol.hasDescribeContentsImplementation(): Boolean {
        return declarationSymbols.filterIsInstance<FirNamedFunctionSymbol>().any { it.isDescribeContentsImplementation() }
    }

    private fun FirNamedFunctionSymbol.isDescribeContentsImplementation(): Boolean {
        if (name != DESCRIBE_CONTENTS_NAME) return false
        return valueParameterSymbols.isEmpty()
    }

    private fun FirNamedFunctionSymbol.isWriteToParcel(): Boolean {
        if (name != WRITE_TO_PARCEL_NAME) return false
        val parameterSymbols = valueParameterSymbols
        if (parameterSymbols.size != 2) return false
        val (destSymbol, flagsSymbol) = parameterSymbols
        if (destSymbol.resolvedReturnTypeRef.coneType.classId != PARCEL_ID) return false
        if (!flagsSymbol.resolvedReturnTypeRef.type.isInt) return false
        return true
    }

    private inline fun createMemberFunctionForParcelize(
        owner: FirRegularClassSymbol,
        name: Name,
        returnType: ConeKotlinType,
        crossinline init: SimpleFunctionBuildingContext.() -> Unit = {}
    ): FirSimpleFunction {
        return createMemberFunction(owner, key, name, returnType) {
            modality = if (owner.modality == Modality.FINAL) Modality.FINAL else Modality.OPEN
            status { isOverride = true }
            init()
        }
    }

    @OptIn(SymbolInternals::class)
    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        return when {
            classSymbol.fir.modality == Modality.ABSTRACT -> emptySet()
            classSymbol in matchedClasses && classSymbol.fir.modality != Modality.SEALED -> parcelizeMethodsNames
            else -> {
                val hasAnnotatedSealedSuperType = classSymbol.resolvedSuperTypeRefs.any {
                    val superSymbol = it.type.fullyExpandedType(session).toRegularClassSymbol(session) ?: return@any false
                    superSymbol.fir.modality == Modality.SEALED && superSymbol in matchedClasses
                }
                if (hasAnnotatedSealedSuperType) {
                    parcelizeMethodsNames
                } else {
                    emptySet()
                }
            }
        }
    }

    private val key: GeneratedDeclarationKey
        get() = ParcelizePluginKey

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(PREDICATE)
    }
}

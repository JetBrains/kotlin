/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.fir

import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.declarations.FirPluginKey
import org.jetbrains.kotlin.fir.declarations.builder.FirSimpleFunctionBuilder
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.predicate.has
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.isInt
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.parcelize.ParcelizeSyntheticComponent.ComponentKind
import org.jetbrains.kotlin.utils.addToStdlib.runIf

class FirParcelizeDeclarationGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    companion object {
        private val PARCELIZE_ID = ClassId(FqName("kotlinx.parcelize"), Name.identifier("Parcelize"))
        private val OLD_PARCELIZE_ID = ClassId(FqName("kotlinx.android.parcel"), Name.identifier("Parcelize"))
        private val PREDICATE = has(PARCELIZE_ID.asSingleFqName(), OLD_PARCELIZE_ID.asSingleFqName())

        private val DESCRIBE_CONTENTS_NAME = Name.identifier(ComponentKind.DESCRIBE_CONTENTS.methodName)
        private val WRITE_TO_PARCEL_NAME = Name.identifier(ComponentKind.WRITE_TO_PARCEL.methodName)
        private val DEST_NAME = Name.identifier("dest")

        private val FLAGS_NAME = Name.identifier("flags")

        private val PARCEL_ID = ClassId(FqName("android.os"), Name.identifier("Parcel"))
        private val parcelizeMethodsNames = setOf(DESCRIBE_CONTENTS_NAME, WRITE_TO_PARCEL_NAME)
    }

    private val matchedClasses by lazy {
        session.predicateBasedProvider.getSymbolsByPredicate(PREDICATE)
            .filterIsInstance<FirRegularClassSymbol>()
    }

    override fun generateFunctions(callableId: CallableId, owner: FirClassSymbol<*>?): List<FirNamedFunctionSymbol> {
        if (owner == null) return emptyList()
        require(owner is FirRegularClassSymbol)
        val functionSymbol = when (callableId.callableName) {
            DESCRIBE_CONTENTS_NAME -> {
                val hasDescribeContentImplementation = owner.hasDescribeContentsImplementation() ||
                        lookupSuperTypes(owner, lookupInterfaces = false, deep = true, session).any {
                            it.fullyExpandedType(session).toRegularClassSymbol(session)?.hasDescribeContentsImplementation() ?: false
                        }
                runIf(!hasDescribeContentImplementation) {
                    generateDescribeContents(owner, callableId)
                }
            }
            WRITE_TO_PARCEL_NAME -> {
                val declaredFunctions = owner.declarationSymbols.filterIsInstance<FirNamedFunctionSymbol>()
                runIf(declaredFunctions.none { it.isWriteToParcel() }) { generateWriteToParcel(owner, callableId) }
            }
            else -> null
        }
        return listOfNotNull(functionSymbol)
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

    private fun generateDescribeContents(owner: FirRegularClassSymbol, callableId: CallableId): FirNamedFunctionSymbol {
        return createFunction(owner, callableId) {
            returnTypeRef = session.builtinTypes.intType
        }
    }

    private fun generateWriteToParcel(owner: FirRegularClassSymbol, callableId: CallableId): FirNamedFunctionSymbol {
        return createFunction(owner, callableId) {
            returnTypeRef = session.builtinTypes.unitType

            valueParameters += buildValueParameter {
                moduleData = session.moduleData
                origin = key.origin
                name = DEST_NAME
                returnTypeRef = buildResolvedTypeRef {
                    type = ConeClassLikeTypeImpl(
                        ConeClassLikeLookupTagImpl(PARCEL_ID),
                        emptyArray(),
                        isNullable = false
                    )
                }
                symbol = FirValueParameterSymbol(name)
                isCrossinline = false
                isNoinline = false
                isVararg = false
            }

            valueParameters += buildValueParameter {
                moduleData = session.moduleData
                origin = key.origin
                name = FLAGS_NAME
                returnTypeRef = session.builtinTypes.intType
                symbol = FirValueParameterSymbol(name)
                isCrossinline = false
                isNoinline = false
                isVararg = false
            }
        }
    }

    private inline fun createFunction(
        owner: FirRegularClassSymbol,
        callableId: CallableId,
        init: FirSimpleFunctionBuilder.() -> Unit
    ): FirNamedFunctionSymbol {
        val function = buildSimpleFunction {
            moduleData = session.moduleData
            origin = key.origin
            status = FirResolvedDeclarationStatusImpl(
                Visibilities.Public,
                if (owner.modality == Modality.FINAL) Modality.FINAL else Modality.OPEN,
                EffectiveVisibility.Public
            ).apply {
                isOverride = true
            }
            name = callableId.callableName
            symbol = FirNamedFunctionSymbol(callableId)
            dispatchReceiverType = owner.defaultType()
            init()
        }
        return function.symbol
    }

    @OptIn(SymbolInternals::class)
    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>): Set<Name> {
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

    override val key: FirPluginKey
        get() = FirParcelizePluginKey

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(PREDICATE)
    }
}

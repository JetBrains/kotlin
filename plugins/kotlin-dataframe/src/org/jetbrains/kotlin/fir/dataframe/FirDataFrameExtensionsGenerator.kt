/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.dataframe

import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirPluginKey
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.builder.buildPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.predicate.has
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.constructType
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.fir.types.toTypeProjection
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

class FirDataFrameExtensionsGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    private val predicateBasedProvider = session.predicateBasedProvider
    private val matchedClasses by lazy {
        predicateBasedProvider.getSymbolsByPredicate(predicate).filterIsInstance<FirRegularClassSymbol>()
    }

    private val predicate: DeclarationPredicate = has(dataSchema)

    private val fields by lazy {
        matchedClasses.flatMap { classSymbol ->
            classSymbol.declarationSymbols.filterIsInstance<FirPropertySymbol>().map { propertySymbol ->
                val callableId = propertySymbol.callableId
                DataSchemaField(
                    classSymbol,
                    propertySymbol,
                    CallableId(packageName = callableId.packageName, className = null, callableName = callableId.callableName)
                )
            }
        }
    }

    private data class DataSchemaField(
        val classSymbol: FirRegularClassSymbol,
        val propertySymbol: FirPropertySymbol,
        val callableId: CallableId
    )

    override fun getTopLevelCallableIds(): Set<CallableId> {
        return fields.mapTo(mutableSetOf()) { it.callableId }
    }

    override fun generateProperties(callableId: CallableId, owner: FirClassSymbol<*>?): List<FirPropertySymbol> {
        if (owner != null) return emptyList()
        return fields.filter { it.callableId == callableId }.flatMap { (owner, property, callableId) ->
            val classTypeProjection = owner.constructType(arrayOf(), isNullable = false).toTypeProjection(Variance.INVARIANT)
            val rowExtension = buildProperty {
                val rowClassId = ClassId(FqName.fromSegments(listOf("org", "jetbrains", "dataframe")), Name.identifier("DataRowBase"))
                val receiverType =
                    ConeClassLikeTypeImpl(ConeClassLikeLookupTagImpl(rowClassId), arrayOf(classTypeProjection), isNullable = false)

                val typeRef = FirResolvedTypeRefImpl(null, mutableListOf(), receiverType, null)
                moduleData = session.moduleData
                resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
                origin = FirDeclarationOrigin.Plugin(DataFramePlugin)
                returnTypeRef = property.resolvedReturnTypeRef
                receiverTypeRef = typeRef
                status = FirResolvedDeclarationStatusImpl(
                    Visibilities.Public,
                    Modality.FINAL,
                    EffectiveVisibility.Public
                )
                getter = buildPropertyAccessor {
                    moduleData = session.moduleData
                    origin = FirDeclarationOrigin.Plugin(DataFramePlugin)
                    returnTypeRef = property.resolvedReturnTypeRef
                    dispatchReceiverType = receiverType
                    symbol = FirPropertyAccessorSymbol()
                    isGetter = true
                    status = FirResolvedDeclarationStatusImpl(
                        Visibilities.Public,
                        Modality.FINAL,
                        EffectiveVisibility.Public
                    )
                }
                name = property.name
                symbol = FirPropertySymbol(callableId)
                isVar = false
                isLocal = false
            }

            val frameExtension = buildProperty {
                val frameClassId = ClassId(FqName.fromSegments(listOf("org", "jetbrains", "dataframe")), Name.identifier("DataFrameBase"))
                val receiverType =
                    ConeClassLikeTypeImpl(ConeClassLikeLookupTagImpl(frameClassId), arrayOf(classTypeProjection), isNullable = false)
                val typeRef = FirResolvedTypeRefImpl(null, mutableListOf(), receiverType, null)

                val columnClassId = ClassId(
                    FqName.fromSegments(listOf("org", "jetbrains", "dataframe", "columns")),
                    Name.identifier("DataColumn")
                )
                val typeProjection = property.resolvedReturnTypeRef.coneType.toTypeProjection(Variance.INVARIANT)
                val returnType =
                    ConeClassLikeTypeImpl(ConeClassLikeLookupTagImpl(columnClassId), arrayOf(typeProjection), isNullable = false)
                val retTypeRef = FirResolvedTypeRefImpl(null, mutableListOf(), returnType, null)
                moduleData = session.moduleData
                resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
                origin = FirDeclarationOrigin.Plugin(DataFramePlugin)
                returnTypeRef = retTypeRef
                receiverTypeRef = typeRef
                status = FirResolvedDeclarationStatusImpl(
                    Visibilities.Public,
                    Modality.FINAL,
                    EffectiveVisibility.Public
                )
                getter = buildPropertyAccessor {
                    moduleData = session.moduleData
                    origin = FirDeclarationOrigin.Plugin(DataFramePlugin)
                    returnTypeRef = retTypeRef
                    dispatchReceiverType = receiverType
                    symbol = FirPropertyAccessorSymbol()
                    isGetter = true
                    status = FirResolvedDeclarationStatusImpl(
                        Visibilities.Public,
                        Modality.FINAL,
                        EffectiveVisibility.Public
                    )
                }
                name = property.name
                symbol = FirPropertySymbol(callableId)
                isVar = false
                isLocal = false
            }
            listOf(rowExtension.symbol, frameExtension.symbol)
        }
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(predicate)
    }

    object DataFramePlugin : FirPluginKey()

    private companion object {
        val dataSchema = "annotations.DataSchema".fqn()
    }
}
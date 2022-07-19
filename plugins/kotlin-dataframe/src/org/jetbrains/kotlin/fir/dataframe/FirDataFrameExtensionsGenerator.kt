/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.dataframe

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.builder.buildPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.predicate.annotated
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.constructType
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlinx.dataframe.annotations.DataSchema

class FirDataFrameExtensionsGenerator(
    session: FirSession,
    private val ids: Set<ClassId>,
    private val state: Map<ClassId, SchemaContext>
) :
    FirDeclarationGenerationExtension(session) {
    private val predicateBasedProvider = session.predicateBasedProvider
    private val matchedClasses by lazy {
        predicateBasedProvider.getSymbolsByPredicate(predicate).filterIsInstance<FirRegularClassSymbol>()
    }

    private val predicate: DeclarationPredicate = annotated(dataSchema)

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

    override fun generateProperties(callableId: CallableId, context: MemberGenerationContext?): List<FirPropertySymbol> {
        val owner = context?.owner
        return when (owner) {
            null -> fields.filter { it.callableId == callableId }.flatMap { (owner, property, callableId) ->

                val resolvedReturnTypeRef = property.resolvedReturnTypeRef
                val propertyName = property.name
                firPropertySymbols(
                    resolvedReturnTypeRef,
                    propertyName,
                    callableId,
                    owner.constructType(arrayOf(), isNullable = false).toTypeProjection(Variance.INVARIANT)
                )
            }
            else -> state
                .flatMap { (classId, schemaContext) ->
                    schemaContext.properties.filter { CallableId(classId, Name.identifier(it.name)) == callableId }
                }
                .flatMap { schemaProperty ->
                    firPropertySymbols(
                        schemaProperty.type.toFirResolvedTypeRef(),
                        Name.identifier(schemaProperty.name),
                        callableId,
                        schemaProperty.coneTypeProjection
                    )
                }
        }
    }

    fun firPropertySymbols(
        resolvedReturnTypeRef: FirResolvedTypeRef,
        propertyName: Name,
        callableId: CallableId,
        classTypeProjection: ConeTypeProjection
    ): List<FirPropertySymbol> {

        val firPropertySymbol = FirPropertySymbol(callableId)
        val rowExtension = buildProperty {
            val rowClassId = ClassId(FqName.fromSegments(listOf("org", "jetbrains", "kotlinx", "dataframe")), Name.identifier("DataRow"))
            val receiverType =
                ConeClassLikeTypeImpl(ConeClassLikeLookupTagImpl(rowClassId), arrayOf(classTypeProjection), isNullable = false)

            val typeRef = FirResolvedTypeRefImpl(null, mutableListOf(), receiverType, null, false)
            moduleData = session.moduleData
            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
            origin = FirDeclarationOrigin.Plugin(DataFramePlugin)
            returnTypeRef = resolvedReturnTypeRef
            receiverTypeRef = typeRef
            status = FirResolvedDeclarationStatusImpl(
                Visibilities.Public,
                Modality.FINAL,
                EffectiveVisibility.Public
            )
            val firPropertyAccessorSymbol = FirPropertyAccessorSymbol()
            getter = buildPropertyAccessor {
                moduleData = session.moduleData
                origin = FirDeclarationOrigin.Plugin(DataFramePlugin)
                returnTypeRef = resolvedReturnTypeRef
                dispatchReceiverType = receiverType
                symbol = firPropertyAccessorSymbol
                propertySymbol = firPropertySymbol
                isGetter = true
                status = FirResolvedDeclarationStatusImpl(
                    Visibilities.Public,
                    Modality.FINAL,
                    EffectiveVisibility.Public
                )
            }.also { firPropertyAccessorSymbol.bind(it) }
            name = propertyName
            symbol = firPropertySymbol
            isVar = false
            isLocal = false
        }.also { firPropertySymbol.bind(it) }

        val firPropertySymbol1 = FirPropertySymbol(callableId)
        val frameExtension = buildProperty {
            val frameClassId =
                ClassId(FqName.fromSegments(listOf("org", "jetbrains", "kotlinx", "dataframe")), Name.identifier("ColumnsContainer"))
            val receiverType =
                ConeClassLikeTypeImpl(ConeClassLikeLookupTagImpl(frameClassId), arrayOf(classTypeProjection), isNullable = false)
            val typeRef = FirResolvedTypeRefImpl(null, mutableListOf(), receiverType, null, false)

            val columnClassId = ClassId(
                FqName.fromSegments(listOf("org", "jetbrains", "kotlinx", "dataframe")),
                Name.identifier("DataColumn")
            )
            val typeProjection = resolvedReturnTypeRef.coneType.toTypeProjection(Variance.INVARIANT)
            val returnType =
                ConeClassLikeTypeImpl(ConeClassLikeLookupTagImpl(columnClassId), arrayOf(typeProjection), isNullable = false)
            val retTypeRef = FirResolvedTypeRefImpl(null, mutableListOf(), returnType, null, false)
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
            val firPropertyAccessorSymbol = FirPropertyAccessorSymbol()
            getter = buildPropertyAccessor {
                moduleData = session.moduleData
                origin = FirDeclarationOrigin.Plugin(DataFramePlugin)
                returnTypeRef = retTypeRef
                dispatchReceiverType = receiverType
                symbol = firPropertyAccessorSymbol
                propertySymbol = firPropertySymbol1
                isGetter = true
                status = FirResolvedDeclarationStatusImpl(
                    Visibilities.Public,
                    Modality.FINAL,
                    EffectiveVisibility.Public
                )
            }.also { firPropertyAccessorSymbol.bind(it) }
            name = propertyName
            symbol = firPropertySymbol1
            isVar = false
            isLocal = false
        }.also { firPropertySymbol1.bind(it) }
        return listOf(rowExtension.symbol, frameExtension.symbol)
    }

    override fun getTopLevelClassIds(): Set<ClassId> {
        return ids
    }

    override fun generateClassLikeDeclaration(classId: ClassId): FirClassLikeSymbol<*>? {
        if (classId !in ids) return null
        val klass = buildRegularClass {
            moduleData = session.moduleData
            resolvePhase = FirResolvePhase.BODY_RESOLVE
            origin = FirDeclarationOrigin.Plugin(FirDataFrameReceiverInjector.DataFramePluginKey)
            status = FirResolvedDeclarationStatusImpl(Visibilities.Local, Modality.FINAL, EffectiveVisibility.Local)
            classKind = ClassKind.CLASS
            scopeProvider = FirKotlinScopeProvider()
            name = classId.shortClassName
            symbol = FirRegularClassSymbol(classId)
        }
        return klass.symbol
    }

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>): Set<Name> {
        return state[classSymbol.classId]?.let {
            it.properties.map { Name.identifier(it.name) }.toSet()
        } ?: emptySet()
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(predicate)
    }

    object DataFramePlugin : GeneratedDeclarationKey()

    private companion object {
        val dataSchema = FqName(DataSchema::class.qualifiedName!!)
    }
}
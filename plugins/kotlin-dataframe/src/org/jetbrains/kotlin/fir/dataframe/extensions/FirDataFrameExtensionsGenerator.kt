/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.dataframe.extensions

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.toClassLikeSymbol
import org.jetbrains.kotlin.fir.containingClassForStaticMemberAttr
import org.jetbrains.kotlin.fir.dataframe.GeneratedNames
import org.jetbrains.kotlin.fir.dataframe.Names
import org.jetbrains.kotlin.fir.dataframe.projectOverDataColumnType
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildPrimaryConstructor
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
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlinx.dataframe.annotations.DataSchema

fun GeneratedNames.FirDataFrameExtensionsGenerator(session: FirSession): FirDeclarationGenerationExtension {
    return FirDataFrameExtensionsGenerator(session, scopes, scopeState, callables, callableState)
}

class FirDataFrameExtensionsGenerator(
    session: FirSession,
    private val scopes: Set<ClassId>,
    private val scopeState: Map<ClassId, SchemaContext>,
    val callables: List<CallableId>,
    val callableState: MutableMap<Name, FirSimpleFunction>
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
        return buildSet {
            fields.mapTo(this) { it.callableId }
            addAll(callables)
        }
    }

    override fun generateProperties(callableId: CallableId, context: MemberGenerationContext?): List<FirPropertySymbol> {
        val owner = context?.owner
        return when (owner) {
            null -> fields.filter { it.callableId == callableId }.flatMap { (owner, property, callableId) ->

                var resolvedReturnTypeRef = property.resolvedReturnTypeRef
                val DATA_SCHEMA_CLASS_ID = ClassId(FqName("org.jetbrains.kotlinx.dataframe.annotations"), Name.identifier("DataSchema"))

                val propertyName = property.name
                val marker = owner.constructType(arrayOf(), isNullable = false).toTypeProjection(Variance.INVARIANT)

                val columnGroupProjection: ConeTypeProjection? = if (resolvedReturnTypeRef.coneType.classId?.equals(Names.DATA_ROW_CLASS_ID) == true) {
                    resolvedReturnTypeRef.coneType.typeArguments[0]
                } else if (resolvedReturnTypeRef.toClassLikeSymbol(session)?.hasAnnotation(DATA_SCHEMA_CLASS_ID) == true) {
                    resolvedReturnTypeRef.coneType
                } else {
                    null
                }

                val LIST = ClassId(FqName("kotlin.collections"), Name.identifier("List"))
                if (
                    resolvedReturnTypeRef.type.classId?.equals(LIST) == true &&
                    (resolvedReturnTypeRef.type.typeArguments[0] as? ConeClassLikeType)?.toSymbol(session)?.hasAnnotation(DATA_SCHEMA_CLASS_ID) == true
                ) {
                    require(columnGroupProjection == null)
                    resolvedReturnTypeRef = ConeClassLikeTypeImpl(
                        ConeClassLikeLookupTagImpl(Names.DF_CLASS_ID),
                        typeArguments = arrayOf(resolvedReturnTypeRef.type.typeArguments[0]),
                        isNullable = false
                    ).toFirResolvedTypeRef()
                }

                val rowExtension = generateExtensionProperty(
                    callableId = callableId,
                    receiverType = ConeClassLikeTypeImpl(
                        ConeClassLikeLookupTagImpl(Names.DATA_ROW_CLASS_ID),
                        typeArguments = arrayOf(marker),
                        isNullable = false
                    ), propertyName = propertyName,
                    returnTypeRef = resolvedReturnTypeRef
                )

                val columnReturnType = when {
                    columnGroupProjection != null -> {
                        ConeClassLikeTypeImpl(
                            ConeClassLikeLookupTagImpl(Names.COLUM_GROUP_CLASS_ID),
                            typeArguments = arrayOf(columnGroupProjection),
                            isNullable = false
                        ).toFirResolvedTypeRef()
                    }
                    else -> resolvedReturnTypeRef.projectOverDataColumnType().toFirResolvedTypeRef()
                }
                val columnsContainerExtension = generateExtensionProperty(
                    callableId = callableId,
                    receiverType = ConeClassLikeTypeImpl(
                        ConeClassLikeLookupTagImpl(Names.COLUMNS_CONTAINER_CLASS_ID),
                        typeArguments = arrayOf(marker),
                        isNullable = false
                    ),
                    propertyName = propertyName,
                    returnTypeRef = columnReturnType
                )
                listOf(rowExtension.symbol, columnsContainerExtension.symbol)
            }

            else -> scopeState
                .flatMap { (classId, schemaContext) ->
                    schemaContext.properties.filter { CallableId(classId, Name.identifier(it.name)) == callableId }
                }
                .flatMap { schemaProperty ->
                    val propertyName = Name.identifier(schemaProperty.name)
                    val dataRowExtension = generateExtensionProperty(
                        callableId = callableId,
                        receiverType = ConeClassLikeTypeImpl(
                            ConeClassLikeLookupTagImpl(Names.DATA_ROW_CLASS_ID),
                            typeArguments = arrayOf(schemaProperty.marker),
                            isNullable = false
                        ),
                        propertyName = propertyName,
                        returnTypeRef = schemaProperty.dataRowReturnType.toFirResolvedTypeRef()
                    )

                    val columnContainerExtension = generateExtensionProperty(
                        callableId = callableId,
                        receiverType = ConeClassLikeTypeImpl(
                            ConeClassLikeLookupTagImpl(Names.COLUMNS_CONTAINER_CLASS_ID),
                            typeArguments = arrayOf(schemaProperty.marker),
                            isNullable = false
                        ),
                        propertyName = propertyName,
                        returnTypeRef = schemaProperty.columnContainerReturnType.toFirResolvedTypeRef()
                    )
                    listOf(dataRowExtension.symbol, columnContainerExtension.symbol)
                }
        }
    }

    private fun generateExtensionProperty(
        callableId: CallableId,
        receiverType: ConeClassLikeTypeImpl,
        propertyName: Name,
        returnTypeRef: FirResolvedTypeRef
    ): FirProperty {
        val firPropertySymbol = FirPropertySymbol(callableId)
        return buildProperty {
            moduleData = session.moduleData
            resolvePhase = FirResolvePhase.BODY_RESOLVE
            origin = FirDeclarationOrigin.Plugin(DataFramePlugin)
            status = FirResolvedDeclarationStatusImpl(
                Visibilities.Public,
                Modality.FINAL,
                EffectiveVisibility.Public
            )
            this.returnTypeRef = returnTypeRef
            receiverTypeRef = receiverType.toFirResolvedTypeRef()
            val classId = callableId.classId
            if (classId != null) {
                dispatchReceiverType = ConeClassLikeTypeImpl(
                    ConeClassLikeLookupTagImpl(classId),
                    emptyArray(),
                    false
                )
            }
            val firPropertyAccessorSymbol = FirPropertyAccessorSymbol()
            getter = buildPropertyAccessor {
                moduleData = session.moduleData
                resolvePhase = FirResolvePhase.BODY_RESOLVE
                origin = FirDeclarationOrigin.Plugin(DataFramePlugin)
                this.returnTypeRef = returnTypeRef
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
    }

    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
        return listOf(buildConstructor(context.owner.classId).symbol)
    }

    fun buildConstructor(classId: ClassId): FirConstructor {
        val lookupTag = ConeClassLikeLookupTagImpl(classId)
        return buildPrimaryConstructor {
            resolvePhase = FirResolvePhase.BODY_RESOLVE
            moduleData = session.moduleData
            origin = FirDeclarationOrigin.Plugin(DataFramePlugin)
            returnTypeRef = buildResolvedTypeRef {
                type = ConeClassLikeTypeImpl(
                    lookupTag,
                    emptyArray(),
                    isNullable = false
                )
            }
            status = FirResolvedDeclarationStatusImpl(
                Visibilities.Public,
                Modality.FINAL,
                EffectiveVisibility.Public
            )
            symbol = FirConstructorSymbol(classId)
        }.also {
            it.containingClassForStaticMemberAttr = lookupTag
        }
    }


    override fun getTopLevelClassIds(): Set<ClassId> {
        return scopes
    }

    override fun generateClassLikeDeclaration(classId: ClassId): FirClassLikeSymbol<*>? {
        if (classId !in scopes) return null
        val klass = buildRegularClass {
            moduleData = session.moduleData
            resolvePhase = FirResolvePhase.BODY_RESOLVE
            origin = FirDeclarationOrigin.Plugin(FirDataFrameReceiverInjector.DataFramePluginKey)
            status = FirResolvedDeclarationStatusImpl(Visibilities.Internal, Modality.FINAL, EffectiveVisibility.Internal)
            classKind = ClassKind.CLASS
            scopeProvider = FirKotlinScopeProvider()
            name = classId.shortClassName
            symbol = FirRegularClassSymbol(classId)
        }
        return klass.symbol
    }

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>): Set<Name> {
        val names = mutableSetOf<Name>()
        scopeState[classSymbol.classId]?.let {
            it.properties.mapTo(names) { Name.identifier(it.name) }
            names.add(SpecialNames.INIT)
        }
        return names
    }

    override fun generateFunctions(callableId: CallableId, context: MemberGenerationContext?): List<FirNamedFunctionSymbol> {
        val state = callableState[callableId.callableName] ?: return emptyList()
        return listOf(state.symbol)
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(predicate)
    }

    object DataFramePlugin : GeneratedDeclarationKey()

    private companion object {
        val dataSchema = FqName(DataSchema::class.qualifiedName!!)
    }
}

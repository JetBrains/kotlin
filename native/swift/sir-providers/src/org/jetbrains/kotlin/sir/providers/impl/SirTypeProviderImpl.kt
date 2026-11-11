/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.providers.*
import org.jetbrains.kotlin.sir.providers.SirTypeProvider.ErrorTypeStrategy
import org.jetbrains.kotlin.sir.providers.source.KotlinRuntimeElement
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.source.KotlinType
import org.jetbrains.kotlin.sir.providers.utils.KotlinCoroutineSupportModule
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeModule
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeSupportModule
import org.jetbrains.kotlin.sir.util.SirCinteropModule
import org.jetbrains.kotlin.sir.util.SirSwiftModule
import org.jetbrains.kotlin.sir.util.expandedType
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

public class SirTypeProviderImpl(
    private val sirSession: SirSession,
    override val errorTypeStrategy: ErrorTypeStrategy,
    override val unsupportedTypeStrategy: ErrorTypeStrategy,
) : SirTypeProvider {

    @ConsistentCopyVisibility
    public data class TypeTranslationCtx internal constructor(
        val currentPosition: SirTypeVariance,
        val reportErrorType: (String) -> Nothing,
        val reportUnsupportedType: () -> Nothing,
        val processTypeImports: (List<SirImport>) -> Unit,
        val requiresHashableAsAny: Boolean,
        val typeParameterScope: Map<KaTypeParameterSymbol, SirTypeParameter> = emptyMap(),
        val inGenericTypeArgument: Boolean = false,
        val inContainerElement: Boolean = false,
    ) {
        public fun anyRepresentativeType(): SirType = when {
            requiresHashableAsAny -> SirType.anyHashable
            inGenericTypeArgument -> SirNominalType(KotlinRuntimeSupportModule.kotlinAny)
            else -> KotlinRuntimeSupportModule.kotlinBridgeableType
        }
    }

    override fun KaType.translateType(
        ktAnalysisSession: KaSession,
        position: SirTypeVariance,
        reportErrorType: (String) -> Nothing,
        reportUnsupportedType: () -> Nothing,
        processTypeImports: (List<SirImport>) -> Unit,
        requiresHashableAsAny: Boolean,
        typeParameterScope: Map<KaTypeParameterSymbol, SirTypeParameter>,
        inGenericTypeArgument: Boolean,
        inContainerElement: Boolean,
    ): SirType = translateType(
        TypeTranslationCtx(
            currentPosition = position,
            reportErrorType = reportErrorType,
            reportUnsupportedType = reportUnsupportedType,
            processTypeImports = processTypeImports,
            requiresHashableAsAny = requiresHashableAsAny,
            typeParameterScope = typeParameterScope,
            inGenericTypeArgument = inGenericTypeArgument,
            inContainerElement = inContainerElement,
        )
    )

    private fun KaType.translateType(
        ctx: TypeTranslationCtx,
    ): SirType =
        buildSirType(this@translateType, ctx)
            .handleErrors(ctx.reportErrorType, ctx.reportUnsupportedType)
            .handleImports(ctx.processTypeImports)

    @OptIn(KaNonPublicApi::class)
    private fun buildSirType(ktType: KaType, ctx: TypeTranslationCtx): SirType {
        fun buildRegularType(kaType: KaType): SirType = sirSession.withSessions {
            when (kaType) {
                is KaUsualClassType -> {
                    when {
                        kaType.classId == KaStandardTypeClassIds.NOTHING -> SirNominalType(SirSwiftModule.never)
                        kaType.classId == KaStandardTypeClassIds.ANY -> ctx.anyRepresentativeType()
                        kaType.classId == KaStandardTypeClassIds.UNIT && ctx.inGenericTypeArgument ->
                            SirNominalType(KotlinRuntimeSupportModule.kotlinUnit)

                        else -> {
                            if (sirSession.isClassIdSupported(kaType.classId)) {
                                val bridgeWrapper = kaType.toSirTypeBridge(ctx)
                                if (bridgeWrapper != null) return@withSessions bridgeWrapper.bridge.swiftType.optionalIfNeeded(kaType)
                                if (kaType.classId in COLLECTION_CLASS_IDS) return@withSessions SirUnsupportedType
                            }

                            // Intercept Flow<T> for typed generic wrapping in covariant position
                            if (kaType.classId in FLOW_CLASS_IDS) {
                                val protocol = kaType.symbol.toSir().primaryDeclaration as SirProtocol
                                val elementArg = kaType.typeArguments.singleOrNull()
                                if (elementArg is KaTypeArgumentWithVariance) {
                                    val elementType = elementArg.type
                                    val flowElementCtx = ctx.copy(inGenericTypeArgument = false, inContainerElement = true)
                                    val translatedElement = when {
                                        elementType.classId == KaStandardTypeClassIds.UNIT ->
                                            flowElementCtx.anyRepresentativeType().optionalIfNeeded(elementType)

                                        else -> elementType.translateType(flowElementCtx)
                                    }
                                    if (translatedElement !is SirErrorType && translatedElement !is SirUnsupportedType) {
                                        return@withSessions SirType.Origin.ReifiedType.Flow(
                                            typedProtocol = when (kaType.classId) {
                                                SHARED_FLOW_CLASS_ID -> KotlinCoroutineSupportModule.kotlinTypedSharedFlow
                                                MUTABLE_SHARED_FLOW_CLASS_ID -> KotlinCoroutineSupportModule.kotlinTypedMutableSharedFlow
                                                STATE_FLOW_CLASS_ID -> KotlinCoroutineSupportModule.kotlinTypedStateFlow
                                                MUTABLE_STATE_FLOW_CLASS_ID -> KotlinCoroutineSupportModule.kotlinTypedMutableStateFlow
                                                else -> KotlinCoroutineSupportModule.kotlinTypedFlow
                                            },
                                            typedStruct = when (kaType.classId) {
                                                SHARED_FLOW_CLASS_ID -> KotlinCoroutineSupportModule.kotlinTypedSharedFlowImpl
                                                MUTABLE_SHARED_FLOW_CLASS_ID -> KotlinCoroutineSupportModule.kotlinTypedMutableSharedFlowImpl
                                                STATE_FLOW_CLASS_ID -> KotlinCoroutineSupportModule.kotlinTypedStateFlowImpl
                                                MUTABLE_STATE_FLOW_CLASS_ID -> KotlinCoroutineSupportModule.kotlinTypedMutableStateFlowImpl
                                                else -> KotlinCoroutineSupportModule.kotlinTypedFlowImpl
                                            },
                                            elementType = translatedElement,
                                            erasedType = SirExistentialType(
                                                protocols = listOf(protocol to emptyList()),
                                                origin = KotlinType(kaType)
                                            ),
                                        ).reifiedType.optionalIfNeeded(kaType)
                                    }
                                }
                            }

                            val classSymbol = kaType.symbol
                            when (val availability = classSymbol.sirAvailability()) {
                                is SirAvailability.Available if availability.visibility < SirVisibility.PACKAGE -> null
                                is SirAvailability.Available, is SirAvailability.Hidden ->
                                    if (classSymbol is KaClassSymbol && classSymbol.classKind == KaClassKind.INTERFACE) {
                                        translateInterfaceType(kaType, classSymbol, ctx)
                                    } else {
                                        nominalTypeFromClassSymbol(kaType, classSymbol, ctx)
                                    }
                                is SirAvailability.Unavailable -> null
                            }
                        }
                    }
                        ?.optionalIfNeeded(kaType)
                        ?: SirUnsupportedType
                }
                is KaFunctionType -> {
                    if (ctx.inGenericTypeArgument) {
                        SirNominalType(KotlinRuntimeSupportModule.kotlinAny).optionalIfNeeded(kaType)
                    } else {
                        val fnCtx = ctx.copy(inGenericTypeArgument = false, inContainerElement = false, requiresHashableAsAny = false)
                        SirFunctionalType(
                            contextTypes = kaType.contextParameterTypes.map {
                                it.translateType(fnCtx.copy(currentPosition = ctx.currentPosition.flip())).withEscapingIfNeeded()
                            },
                            parameterTypes = listOfNotNull(
                                kaType.receiverType?.translateType(fnCtx.copy(currentPosition = ctx.currentPosition.flip()))
                                    ?.withEscapingIfNeeded()
                            ) + kaType.parameterTypes
                                .map { it.translateType(fnCtx.copy(currentPosition = ctx.currentPosition.flip())).withEscapingIfNeeded() },
                            isAsync = kaType.isSuspendFunctionType,
                            errorType = kaType.isSuspendFunctionType.ifTrue { SirType.any } ?: SirType.never,
                            returnType = kaType.returnType.translateType(fnCtx.copy(currentPosition = ctx.currentPosition)),
                        )
                            .withEscapingIfNeeded()
                            .optionalIfNeeded(kaType)
                    }
                }
                is KaTypeParameterType -> ctx.translateTypeParameterType(kaType)
                is KaErrorType
                    -> SirErrorType(kaType.errorMessage)
                else
                    -> SirErrorType("Unexpected type $kaType")
            }
        }

        return ktType.abbreviation?.let { buildRegularType(it) }
            ?: buildRegularType(ktType)
    }

    private fun TypeTranslationCtx.translateTypeParameterType(type: KaTypeParameterType): SirType = sirSession.withSessions {
        val symbol = type.symbol
        val fallbackType = SirUnsupportedType
        if (symbol.isReified) return@withSessions fallbackType
        typeParameterScope[symbol]?.let { sirTypeParam ->
            return@withSessions SirTypeParameterType(sirTypeParam).optionalIfNeeded(type)
        }
        return@withSessions when (symbol.upperBounds.size) {
            0 -> anyRepresentativeType().optional()
            1 -> {
                val ub = symbol.upperBounds.single()
                if (ub.containsTypeParameterSymbol(symbol)) {
                    anyRepresentativeType().optionalIfNeeded(type)
                } else {
                    val upperBound = ub.translateType(this@translateTypeParameterType)
                    if (type.isMarkedNullable) {
                        upperBound.optional()
                    } else {
                        upperBound
                    }
                }
            }
            else -> anyRepresentativeType().optionalIfNeeded(type)
        }
    }

    private fun KaType.containsTypeParameterSymbol(
        target: KaTypeParameterSymbol,
        visited: MutableSet<KaTypeParameterSymbol> = mutableSetOf(),
    ): Boolean = when (this) {
        is KaTypeParameterType -> {
            val sym = symbol
            sym == target || (visited.add(sym) && sym.upperBounds.any { it.containsTypeParameterSymbol(target, visited) })
        }
        is KaClassType -> typeArguments.any { proj ->
            (proj as? KaTypeArgumentWithVariance)?.type?.containsTypeParameterSymbol(target, visited) == true
        }
        is KaFunctionType -> contextParameterTypes.any { it.containsTypeParameterSymbol(target, visited) } ||
                (receiverType?.containsTypeParameterSymbol(target, visited) == true) ||
                parameterTypes.any { it.containsTypeParameterSymbol(target, visited) } ||
                returnType.containsTypeParameterSymbol(target, visited)
        else -> false
    }

    private fun SirType.handleErrors(
        reportErrorType: (String) -> Nothing,
        reportUnsupportedType: () -> Nothing,
    ): SirType {
        if (this is SirErrorType && sirSession.errorTypeStrategy == ErrorTypeStrategy.Fail) {
            reportErrorType(reason)
        }
        if (this is SirUnsupportedType && sirSession.unsupportedTypeStrategy == ErrorTypeStrategy.Fail) {
            reportUnsupportedType()
        }
        return this
    }

    private fun SirType.handleImports(
        processTypeImports: (List<SirImport>) -> Unit,
    ): SirType {
        fun SirDeclaration.extractImport() {
            when (val origin = this.origin) {
                is KotlinSource -> {
                    val ktModule = sirSession.withSessions {
                        origin.symbol.containingModule
                    }
                    val sirModule = with(sirSession) {
                        ktModule.sirModule()
                    }
                    // We're being lazy here importing all spi's preventively
                    val spi = this@extractImport.attributes.filterIsInstance<SirAttribute.SPI>()

                    val imports = when (sirModule) {
                        is SirCinteropModule -> sirModule.importNames.map {
                            SirImport(it, spi = spi, conditionallyAvailable = true)
                        }
                        else ->
                            listOf(SirImport(sirModule.name, spi = spi))
                    }
                    processTypeImports(imports)
                }
                is KotlinRuntimeElement -> {
                    processTypeImports(listOf(SirImport(KotlinRuntimeModule.name)))
                }
                else -> {}
            }
        }

        val origin = origin
        if (origin is SirType.Origin.ReifiedType) {
            origin.erasedType.handleImports(processTypeImports)
        }
        when (this) {
            is SirNominalType -> {
                generateSequence(this) { it.parent }.forEach { type ->
                    type.typeArguments.forEach { it.handleImports(processTypeImports) }
                    type.typeDeclaration.extractImport()
                }
            }
            is SirExistentialType -> this.protocols.forEach { [protocol, typeArguments] ->
                protocol.extractImport()
                typeArguments.forEach { it.handleImports(processTypeImports) }
            }
            is SirFunctionalType -> {
                contextTypes.forEach { it.handleImports(processTypeImports) }
                parameterTypes.forEach { it.handleImports(processTypeImports) }
                errorType.handleImports(processTypeImports)
                returnType.handleImports(processTypeImports)
            }
            is SirTupleType -> {
                types.forEach { [_, type] -> type.handleImports(processTypeImports) }
            }
            is SirType.Metatype -> {
                type.handleImports(processTypeImports)
            }
            is SirTypeParameterType -> {
                parameter.bounds.forEach { it.handleImports(processTypeImports) }
            }
            is SirSelfType -> {}
            is SirErrorType -> {}
            SirUnsupportedType -> {}
        }
        return this
    }

    private fun translateInterfaceType(
        kaType: KaUsualClassType,
        classSymbol: KaClassSymbol,
        ctx: TypeTranslationCtx,
    ): SirType? = sirSession.withSessions {
        val sirResult = classSymbol.toSir()
        val protocol = sirResult.primaryDeclaration as? SirProtocol ?: return@withSessions null
        if (protocol.typeParameters.isEmpty()) {
            val anyWrapper = (sirResult as? SirTranslationResult.RegularInterface)?.anyWrapperDeclaration
            if ((ctx.inGenericTypeArgument || ctx.requiresHashableAsAny) && sirResult is SirTranslationResult.RegularInterface) {
                if (anyWrapper != null) {
                    SirNominalType(anyWrapper)
                } else {
                    SirNominalType(
                        KotlinRuntimeSupportModule.kotlinExistential,
                        listOf(SirExistentialType(sirResult.markerDeclaration)),
                    )
                }
            } else {
                SirExistentialType(protocol)
            }
        } else {
            if (kaType.typeArguments.isNotEmpty() &&
                kaType.typeArguments.all { it is KaStarTypeProjection } &&
                !ctx.inGenericTypeArgument &&
                !ctx.inContainerElement &&
                !ctx.requiresHashableAsAny
            ) {
                SirExistentialType(listOf(protocol to emptyList()))
            } else {
                val translatedArgs = translateTypeArguments(kaType, classSymbol, protocol, ctx) ?: return@withSessions null
                if (ctx.inGenericTypeArgument || ctx.inContainerElement || ctx.requiresHashableAsAny) {
                    val anyWrapper = (sirResult as? SirTranslationResult.RegularInterface)?.anyWrapperDeclaration
                        ?: return@withSessions null
                    SirNominalType(anyWrapper, translatedArgs)
                } else {
                    SirExistentialType(listOf(protocol to translatedArgs))
                }
            }
        }
    }

    private fun nominalTypeFromClassSymbol(
        kaType: KaUsualClassType,
        symbol: KaClassLikeSymbol,
        ctx: TypeTranslationCtx,
    ): SirNominalType? = sirSession.withSessions {
        val decl = symbol.toSir().primaryDeclaration as? SirScopeDefiningDeclaration ?: return@withSessions null
        val genericDecl = decl as? SirGenericDeclaration
        if (genericDecl == null || genericDecl.typeParameters.isEmpty()) {
            SirNominalType(decl)
        } else {
            val translatedArgs = translateTypeArguments(kaType, symbol, genericDecl, ctx) ?: return@withSessions null
            val starOrigin = if (kaType.typeArguments.any { it is KaStarTypeProjection } && !kaType.containsAnyTypeParameter()) {
                KotlinType(kaType.withNullability(false))
            } else {
                SirType.Origin.Unknown
            }
            SirNominalType(decl, translatedArgs, origin = starOrigin)
        }
    }

    private fun KaType.containsAnyTypeParameter(): Boolean = when (this) {
        is KaTypeParameterType -> true
        is KaClassType -> typeArguments.any { (it as? KaTypeArgumentWithVariance)?.type?.containsAnyTypeParameter() == true }
        is KaFunctionType -> contextParameterTypes.any { it.containsAnyTypeParameter() } ||
                (receiverType?.containsAnyTypeParameter() == true) ||
                parameterTypes.any { it.containsAnyTypeParameter() } ||
                returnType.containsAnyTypeParameter()
        else -> false
    }

    private fun translateTypeArguments(
        kaType: KaUsualClassType,
        symbol: KaClassLikeSymbol,
        decl: SirGenericDeclaration,
        ctx: TypeTranslationCtx,
    ): List<SirType>? = sirSession.withSessions {
        val sirParams = decl.typeParameters
        if (kaType.typeArguments.size != sirParams.size) return@withSessions null
        buildList(sirParams.size) {
            for (i in sirParams.indices) {
                val sirParam = sirParams[i]
                val ktParam = symbol.typeParameters.getOrNull(i)
                val needsHashable = sirParam.bounds.any { (it as? SirNominalType)?.typeDeclaration == SirSwiftModule.hashable }
                val argCtx = ctx.copy(
                    inGenericTypeArgument = sirParam.bounds.isNotEmpty(),
                    inContainerElement = false,
                    requiresHashableAsAny = needsHashable,
                )
                val proj = kaType.typeArguments[i]
                val translatedArg = when (proj) {
                    is KaTypeArgumentWithVariance -> buildSirType(proj.type, argCtx)
                        .handleImports(ctx.processTypeImports)
                    is KaStarTypeProjection -> translateStarTypeArgument(sirParam, ktParam, symbol, argCtx)
                        ?.handleImports(ctx.processTypeImports)
                        ?: return@withSessions null
                }
                if (translatedArg is SirErrorType || translatedArg is SirUnsupportedType) {
                    return@withSessions null
                }
                add(translatedArg)
            }
        }
    }

    private fun translateStarTypeArgument(
        sirParam: SirTypeParameter,
        ktParam: KaTypeParameterSymbol?,
        ownerSymbol: KaClassLikeSymbol,
        argCtx: TypeTranslationCtx,
    ): SirType? = sirSession.withSessions {
        if (ktParam != null && ktParam.upperBounds.size > 1) return@withSessions null
        val ub = ktParam?.upperBounds?.singleOrNull()
        if (ub is KaClassType && ub.typeArguments.isNotEmpty() && ub.symbol != ownerSymbol) {
            val ubClassSymbol = ub.symbol as? KaClassSymbol
            if (ubClassSymbol?.classKind == KaClassKind.INTERFACE && ub.classId !in STDLIB_RELAXED_BOUND_CLASS_IDS) {
                return@withSessions null
            }
        }
        val nonTrivialSirBounds = sirParam.bounds.filter { bound ->
            val decl = (bound as? SirNominalType)?.typeDeclaration
            decl != KotlinRuntimeSupportModule.kotlinBridgeable && decl != SirSwiftModule.hashable
        }
        if (nonTrivialSirBounds.size > 1) return@withSessions null
        if (nonTrivialSirBounds.isEmpty()) {
            val isNullable = ub?.isMarkedNullable ?: true
            val base = argCtx.anyRepresentativeType()
            return@withSessions if (isNullable) base.optional() else base
        }
        if (ub == null) return@withSessions argCtx.anyRepresentativeType().optional()
        if (ub.containsTypeParameterSymbol(ktParam)) {
            return@withSessions nonTrivialSirBounds.single()
        }
        buildSirType(ub, argCtx)
    }

    private fun SirType.optionalIfNeeded(originalKtType: KaType): SirType = sirSession.withSessions {
        if (originalKtType.isMarkedNullable && !originalKtType.isTypealiasToNullableType) {
            optional()
        } else {
            this@optionalIfNeeded
        }
    }

    context(ka: KaSession)
    private val KaType.isTypealiasToNullableType: Boolean
        get() = (symbol as? KaTypeAliasSymbol)?.expandedType?.isMarkedNullable ?: false

    private fun SirType.withEscapingIfNeeded(): SirType = when (this) {
        is SirFunctionalType -> copyAppendingAttributes(SirAttribute.Escaping)
        is SirNominalType -> if (isTypealiasOntoFunctionalType) {
            copyAppendingAttributes(SirAttribute.Escaping)
        } else {
            this
        }
        else -> this
    }

    private val SirNominalType.isTypealiasOntoFunctionalType: Boolean
        get() = (typeDeclaration as? SirTypealias)?.let { it.expandedType is SirFunctionalType } == true

    public companion object {
        public val FLOW_CLASS_ID: ClassId = ClassId.fromString("kotlinx/coroutines/flow/Flow")
        public val SHARED_FLOW_CLASS_ID: ClassId = ClassId.fromString("kotlinx/coroutines/flow/SharedFlow")
        public val MUTABLE_SHARED_FLOW_CLASS_ID: ClassId = ClassId.fromString("kotlinx/coroutines/flow/MutableSharedFlow")
        public val STATE_FLOW_CLASS_ID: ClassId = ClassId.fromString("kotlinx/coroutines/flow/StateFlow")
        public val MUTABLE_STATE_FLOW_CLASS_ID: ClassId = ClassId.fromString("kotlinx/coroutines/flow/MutableStateFlow")

        public val FLOW_CLASS_IDS: List<ClassId> = listOf(
            FLOW_CLASS_ID,
            SHARED_FLOW_CLASS_ID, MUTABLE_SHARED_FLOW_CLASS_ID,
            STATE_FLOW_CLASS_ID, MUTABLE_STATE_FLOW_CLASS_ID,
        )

        public val COLLECTION_CLASS_IDS: Set<ClassId> = setOf(StandardClassIds.Set, StandardClassIds.Map, StandardClassIds.List)

        public val STDLIB_RELAXED_BOUND_CLASS_IDS: Set<ClassId> = setOf(
            StandardClassIds.Comparable,
            StandardClassIds.Enum,
            StandardClassIds.CharSequence,
            StandardClassIds.Number,
            StandardClassIds.Iterable,
            StandardClassIds.Collection,
            StandardClassIds.List,
            StandardClassIds.Set,
            StandardClassIds.Map,
            StandardClassIds.MutableIterable,
            StandardClassIds.MutableCollection,
            StandardClassIds.MutableList,
            StandardClassIds.MutableSet,
            StandardClassIds.MutableMap,
        )
    }
}

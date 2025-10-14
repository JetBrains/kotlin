/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.containingModule
import org.jetbrains.kotlin.analysis.api.components.isAnyType
import org.jetbrains.kotlin.analysis.api.components.isBooleanType
import org.jetbrains.kotlin.analysis.api.components.isByteType
import org.jetbrains.kotlin.analysis.api.components.isCharType
import org.jetbrains.kotlin.analysis.api.components.isClassType
import org.jetbrains.kotlin.analysis.api.components.isDoubleType
import org.jetbrains.kotlin.analysis.api.components.isFloatType
import org.jetbrains.kotlin.analysis.api.components.isFunctionType
import org.jetbrains.kotlin.analysis.api.components.isIntType
import org.jetbrains.kotlin.analysis.api.components.isLongType
import org.jetbrains.kotlin.analysis.api.components.isMarkedNullable
import org.jetbrains.kotlin.analysis.api.components.isNothingType
import org.jetbrains.kotlin.analysis.api.components.isShortType
import org.jetbrains.kotlin.analysis.api.components.isStringType
import org.jetbrains.kotlin.analysis.api.components.isSuspendFunctionType
import org.jetbrains.kotlin.analysis.api.components.isUByteType
import org.jetbrains.kotlin.analysis.api.components.isUIntType
import org.jetbrains.kotlin.analysis.api.components.isULongType
import org.jetbrains.kotlin.analysis.api.components.isUShortType
import org.jetbrains.kotlin.analysis.api.components.isUnitType
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeAliasSymbol
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.SirTypeProvider
import org.jetbrains.kotlin.sir.providers.SirTypeProvider.ErrorTypeStrategy
import org.jetbrains.kotlin.sir.providers.sirAvailability
import org.jetbrains.kotlin.sir.providers.source.KotlinRuntimeElement
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.toSir
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeModule
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeSupportModule
import org.jetbrains.kotlin.sir.providers.withSessions
import org.jetbrains.kotlin.sir.util.SirSwiftModule
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

public class SirTypeProviderImpl(
    private val sirSession: SirSession,
    override val errorTypeStrategy: ErrorTypeStrategy,
    override val unsupportedTypeStrategy: ErrorTypeStrategy,
) : SirTypeProvider {

    public data class TypeTranslationCtx(
        val currentPosition: SirTypeVariance,
        val reportErrorType: (String) -> Nothing,
        val reportUnsupportedType: () -> Nothing,
        val processTypeImports: (List<SirImport>) -> Unit,
        val requiresHashableAsAny: Boolean,
    ) {
        public fun anyRepresentativeType(): SirType =
            if (requiresHashableAsAny) SirType.anyHashable else KotlinRuntimeSupportModule.kotlinBridgeableType
    }

    override fun KaType.translateType(
        ktAnalysisSession: KaSession,
        position: SirTypeVariance,
        reportErrorType: (String) -> Nothing,
        reportUnsupportedType: () -> Nothing,
        processTypeImports: (List<SirImport>) -> Unit,
        requiresHashableAsAny: Boolean,
    ): SirType = translateType(
        TypeTranslationCtx(
            currentPosition = position,
            reportErrorType = reportErrorType,
            reportUnsupportedType = reportUnsupportedType,
            processTypeImports = processTypeImports,
            requiresHashableAsAny = requiresHashableAsAny,
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
        fun buildPrimitiveType(ktType: KaType): SirType? = sirSession.withSessions {
            when {
                ktType.isCharType -> SirSwiftModule.utf16CodeUnit
                ktType.isUnitType -> SirSwiftModule.void

                ktType.isByteType -> SirSwiftModule.int8
                ktType.isShortType -> SirSwiftModule.int16
                ktType.isIntType -> SirSwiftModule.int32
                ktType.isLongType -> SirSwiftModule.int64

                ktType.isUByteType -> SirSwiftModule.uint8
                ktType.isUShortType -> SirSwiftModule.uint16
                ktType.isUIntType -> SirSwiftModule.uint32
                ktType.isULongType -> SirSwiftModule.uint64

                ktType.isBooleanType -> SirSwiftModule.bool

                ktType.isDoubleType -> SirSwiftModule.double
                ktType.isFloatType -> SirSwiftModule.float

                else -> null
            }
                ?.let { SirNominalType(it) }
                ?.optionalIfNeeded(ktType)
        }

        fun buildRegularType(kaType: KaType): SirType = sirSession.withSessions {
            when (kaType) {
                is KaUsualClassType -> {
                    if (kaType.isTypealiasToFunctionalType && kaType.isUnsupportedFunctionalType(ctx)) {
                        return@withSessions SirUnsupportedType
                    }
                    when {
                        kaType.isNothingType -> SirNominalType(SirSwiftModule.never)
                        kaType.isAnyType -> ctx.anyRepresentativeType()

                        sirSession.customTypeTranslator.isClassIdSupported(kaType.classId) -> {
                            with(sirSession.customTypeTranslator) { kaType.toSirType(ctx) }
                        }

                        else -> {
                            val classSymbol = kaType.symbol
                            when (classSymbol.sirAvailability()) {
                                is SirAvailability.Available, is SirAvailability.Hidden ->
                                    if (classSymbol is KaClassSymbol && classSymbol.classKind == KaClassKind.INTERFACE) {
                                        SirExistentialType(classSymbol.toSir().allDeclarations.firstIsInstance<SirProtocol>())
                                    } else {
                                        nominalTypeFromClassSymbol(classSymbol)
                                    }
                                is SirAvailability.Unavailable -> null
                            }
                        }
                    }
                        ?.optionalIfNeeded(kaType)
                        ?: SirUnsupportedType
                }
                is KaFunctionType -> {
                    if (kaType.isUnsupportedFunctionalType(ctx)) {
                        return@withSessions SirUnsupportedType
                    } else {
                        SirFunctionalType(
                            parameterTypes = listOfNotNull(
                                kaType.receiverType?.translateType(
                                    ctx.copy(currentPosition = ctx.currentPosition.flip())
                                )
                            ) + kaType.parameterTypes.map { it.translateType(ctx.copy(currentPosition = ctx.currentPosition.flip())) },
                            returnType = kaType.returnType.translateType(ctx.copy(currentPosition = ctx.currentPosition)),
                        ).optionalIfNeeded(kaType)
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
            ?: buildPrimitiveType(ktType)
            ?: buildRegularType(ktType)
    }

    private fun TypeTranslationCtx.translateTypeParameterType(type: KaTypeParameterType): SirType = sirSession.withSessions {
        val symbol = type.symbol
        val fallbackType = SirUnsupportedType
        if (symbol.isReified) return@withSessions fallbackType
        return@withSessions when (symbol.upperBounds.size) {
            0 -> anyRepresentativeType().optional()
            1 -> {
                val upperBound = symbol.upperBounds.single().translateType(this@translateTypeParameterType)
                if (type.isMarkedNullable) {
                    upperBound.optional()
                } else {
                    upperBound
                }
            }
            else -> fallbackType
        }
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
                    processTypeImports(listOf(SirImport(sirModule.name)))
                }
                is KotlinRuntimeElement -> {
                    processTypeImports(listOf(SirImport(KotlinRuntimeModule.name)))
                }
                else -> {}
            }
        }

        when (this) {
            is SirNominalType -> {
                generateSequence(this) { this.parent }.forEach { _ ->
                    typeArguments.forEach { it.handleImports(processTypeImports) }
                    typeDeclaration.extractImport()
                }
            }
            is SirExistentialType -> this.protocols.forEach { it.extractImport() }
            is SirFunctionalType -> {
                parameterTypes.forEach { it.handleImports(processTypeImports) }
                returnType.handleImports(processTypeImports)
            }
            is SirErrorType -> {}
            SirUnsupportedType -> {}
        }
        return this
    }

    private fun nominalTypeFromClassSymbol(
        symbol: KaClassLikeSymbol,
    ): SirNominalType? = sirSession.withSessions {
        symbol.toSir().allDeclarations.firstIsInstanceOrNull<SirScopeDefiningDeclaration>()?.let(::SirNominalType)
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

    context(ka: KaSession)
    private val KaType.isTypealiasToFunctionalType: Boolean
        get() = (symbol as? KaTypeAliasSymbol)?.expandedType?.isFunctionType ?: false

    context(ka: KaSession)
    private fun KaType.isUnsupportedFunctionalType(ctx: TypeTranslationCtx, ): Boolean =
        isSuspendFunctionType || ctx.currentPosition == SirTypeVariance.COVARIANT
}


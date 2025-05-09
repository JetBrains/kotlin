/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.KaSession
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
import org.jetbrains.kotlin.sir.providers.source.KotlinRuntimeElement
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeModule
import org.jetbrains.kotlin.sir.providers.withSessions
import org.jetbrains.kotlin.sir.util.SirSwiftModule
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

public class SirTypeProviderImpl(
    private val sirSession: SirSession,
    override val errorTypeStrategy: ErrorTypeStrategy,
    override val unsupportedTypeStrategy: ErrorTypeStrategy,
) : SirTypeProvider {

    private data class TypeTranslationCtx(
        val reportErrorType: (String) -> Nothing,
        val reportUnsupportedType: () -> Nothing,
        val processTypeImports: (List<SirImport>) -> Unit,
    )

    override fun KaType.translateType(
        ktAnalysisSession: KaSession,
        reportErrorType: (String) -> Nothing,
        reportUnsupportedType: () -> Nothing,
        processTypeImports: (List<SirImport>) -> Unit,
    ): SirType = translateType(
        TypeTranslationCtx(
            reportErrorType,
            reportUnsupportedType,
            processTypeImports,
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
                ktType.isCharType -> SirNominalType(SirSwiftModule.utf16CodeUnit)
                ktType.isUnitType -> SirNominalType(SirSwiftModule.void)

                ktType.isByteType -> SirNominalType(SirSwiftModule.int8)
                ktType.isShortType -> SirNominalType(SirSwiftModule.int16)
                ktType.isIntType -> SirNominalType(SirSwiftModule.int32)
                ktType.isLongType -> SirNominalType(SirSwiftModule.int64)

                ktType.isUByteType -> SirNominalType(SirSwiftModule.uint8)
                ktType.isUShortType -> SirNominalType(SirSwiftModule.uint16)
                ktType.isUIntType -> SirNominalType(SirSwiftModule.uint32)
                ktType.isULongType -> SirNominalType(SirSwiftModule.uint64)

                ktType.isBooleanType -> SirNominalType(SirSwiftModule.bool)

                ktType.isDoubleType -> SirNominalType(SirSwiftModule.double)
                ktType.isFloatType -> SirNominalType(SirSwiftModule.float)

                else -> null
            }
                ?.optionalIfNeeded(ktType)
        }

        fun buildRegularType(kaType: KaType): SirType = sirSession.withSessions {
            fun KaTypeProjection.sirType(): SirType = when (this) {
                is KaStarTypeProjection -> SirNominalType(KotlinRuntimeModule.kotlinBase)
                is KaTypeArgumentWithVariance -> buildSirType(type, ctx)
            }
            when (kaType) {
                is KaUsualClassType -> {
                    when {
                        kaType.isNothingType -> SirNominalType(SirSwiftModule.never)
                        kaType.isStringType -> SirNominalType(SirSwiftModule.string)
                        kaType.isAnyType -> SirNominalType(KotlinRuntimeModule.kotlinBase)

                        kaType.isClassType(StandardClassIds.List) -> {
                            SirArrayType(
                                kaType.typeArguments.single().sirType()
                            )
                        }

                        kaType.isClassType(StandardClassIds.Set) -> {
                            SirNominalType(
                                SirSwiftModule.set,
                                listOf(kaType.typeArguments.single().sirType())
                            )
                        }

                        kaType.isClassType(StandardClassIds.Map) -> {
                            SirDictionaryType(
                                kaType.typeArguments.first().sirType(),
                                kaType.typeArguments.last().sirType()
                            )
                        }

                        else -> {
                            val classSymbol = kaType.symbol
                            when (classSymbol.sirAvailability(useSiteSession)) {
                                is SirAvailability.Available, is SirAvailability.Hidden ->
                                    if (classSymbol is KaClassSymbol && classSymbol.classKind == KaClassKind.INTERFACE) {
                                        SirExistentialType(classSymbol.toSir().allDeclarations.firstIsInstance<SirProtocol>())
                                    } else {
                                        ctx.nominalTypeFromClassSymbol(classSymbol)
                                    }
                                is SirAvailability.Unavailable -> null
                            }
                        }
                    }
                        ?.optionalIfNeeded(kaType)
                        ?: SirUnsupportedType
                }
                is KaFunctionType -> {
                    if (kaType.isSuspendFunctionType) {
                        return@withSessions SirUnsupportedType
                    } else {
                        SirFunctionalType(
                            parameterTypes = listOfNotNull(kaType.receiverType?.translateType(ctx)) + kaType.parameterTypes.map { it.translateType(ctx) },
                            returnType = kaType.returnType.translateType(ctx)
                        )
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
            0 -> SirNominalType(KotlinRuntimeModule.kotlinBase).optional()
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

    private fun TypeTranslationCtx.nominalTypeFromClassSymbol(symbol: KaClassLikeSymbol): SirNominalType? = sirSession.withSessions {
        symbol.toSir().allDeclarations.firstIsInstanceOrNull<SirNamedDeclaration>()?.let(::SirNominalType)
    }

    private fun SirType.optionalIfNeeded(originalKtType: KaType): SirType = sirSession.withSessions {
        if (originalKtType.isMarkedNullable && !originalKtType.isTypealiasToNullableType) {
            optional()
        } else {
            this@optionalIfNeeded
        }
    }

    private val KaType.isTypealiasToNullableType: Boolean
        get() = sirSession.withSessions {
            (symbol as? KaTypeAliasSymbol)
                .takeIf { it?.expandedType?.isMarkedNullable == true }
                ?.let { return@let true }
                ?: false
        }
}


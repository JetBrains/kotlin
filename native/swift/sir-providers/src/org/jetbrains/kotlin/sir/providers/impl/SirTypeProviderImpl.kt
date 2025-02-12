/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeAliasSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.SirTypeProvider
import org.jetbrains.kotlin.sir.providers.SirTypeProvider.ErrorTypeStrategy
import org.jetbrains.kotlin.sir.providers.source.KotlinRuntimeElement
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeModule
import org.jetbrains.kotlin.sir.util.SirSwiftModule
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

public class SirTypeProviderImpl(
    private val sirSession: SirSession,
    override val errorTypeStrategy: ErrorTypeStrategy,
    override val unsupportedTypeStrategy: ErrorTypeStrategy,
) : SirTypeProvider {

    private data class TypeTranslationCtx(
        val ktAnalysisSession: KaSession,
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
            ktAnalysisSession,
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
            .handleImports(ctx.ktAnalysisSession, ctx.processTypeImports)

    @OptIn(KaNonPublicApi::class)
    private fun buildSirType(ktType: KaType, ctx: TypeTranslationCtx): SirType {
        fun buildPrimitiveType(ktType: KaType): SirType? = with(ctx.ktAnalysisSession) {
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

        fun buildRegularType(kaType: KaType): SirType = with(ctx.ktAnalysisSession) {
            when (kaType) {
                is KaUsualClassType -> with(sirSession) {
                    when {
                        kaType.isNothingType -> SirNominalType(SirSwiftModule.never)
                        kaType.isStringType -> SirNominalType(SirSwiftModule.string)
                        kaType.isAnyType -> SirNominalType(KotlinRuntimeModule.kotlinBase)

                        kaType.isClassType(StandardClassIds.List) -> {
                            val elementType = buildSirType(kaType.typeArguments.single().type!!, ctx)
                            SirArrayType(elementType)
                        }

                        kaType.isClassType(StandardClassIds.Set) -> {
                            val elementType = buildSirType(kaType.typeArguments.single().type!!, ctx)
                            SirNominalType(SirSwiftModule.set, typeArguments = listOf(elementType))
                        }

                        kaType.isClassType(StandardClassIds.Map) -> {
                            val (keyType, valueType) = kaType.typeArguments.map { buildSirType(it.type!!, ctx) }
                            SirDictionaryType(keyType, valueType)
                        }

                        else -> {
                            val classSymbol = kaType.symbol
                            if (classSymbol.sirVisibility(ctx.ktAnalysisSession) == SirVisibility.PUBLIC) {
                                if (classSymbol is KaClassSymbol && classSymbol.classKind == KaClassKind.INTERFACE) {
                                    SirExistentialType(classSymbol.toSir().allDeclarations.firstIsInstance<SirProtocol>())
                                } else {
                                    classSymbol.toSir().allDeclarations.firstIsInstanceOrNull<SirNamedDeclaration>()?.let(::SirNominalType)
                                }
                            } else {
                                null
                            }
                        }
                    }
                        ?.optionalIfNeeded(kaType)
                        ?: SirUnsupportedType
                }
                is KaFunctionType -> {
                    if (kaType.isSuspendFunctionType) {
                        return SirUnsupportedType
                    } else {
                        SirFunctionalType(
                            parameterTypes = kaType.parameterTypes.map { it.translateType(ctx) },
                            returnType = kaType.returnType.translateType(ctx)
                        )
                    }
                }
                is KaTypeParameterType -> {
                    SirGenericType()
                }
                is KaErrorType,
                    -> SirErrorType(kaType.errorMessage)
                else
                    -> SirErrorType("Unexpected type $kaType")
            }
        }

        return ktType.abbreviation?.let { buildRegularType(it) }
            ?: buildPrimitiveType(ktType)
            ?: buildRegularType(ktType)
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
        ktAnalysisSession: KaSession,
        processTypeImports: (List<SirImport>) -> Unit,
    ): SirType {
        if (this is SirNominalType) {
            when (val origin = typeDeclaration.origin) {
                is KotlinSource -> {
                    val ktModule = with(ktAnalysisSession) {
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
            for (typeArg in typeArguments) {
                typeArg.handleImports(ktAnalysisSession, processTypeImports)
            }
        }
        return this
    }
}

private fun SirType.optionalIfNeeded(originalKtType: KaType) =
    if (originalKtType.nullability == KaTypeNullability.NULLABLE && !originalKtType.isTypealiasToNullableType) {
        optional()
    } else {
        this
    }

private val KaType.isTypealiasToNullableType: Boolean
    get() = (symbol as? KaTypeAliasSymbol)
        .takeIf { it?.expandedType?.nullability == KaTypeNullability.NULLABLE }
        ?.let { return true }
        ?: false
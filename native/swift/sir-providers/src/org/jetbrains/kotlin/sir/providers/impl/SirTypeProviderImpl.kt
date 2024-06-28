/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.analysis.api.KaAnalysisNonPublicApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.SirTypeProvider
import org.jetbrains.kotlin.sir.providers.SirTypeProvider.ErrorTypeStrategy
import org.jetbrains.kotlin.sir.providers.source.KotlinRuntimeElement
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeModule
import org.jetbrains.kotlin.sir.util.SirSwiftModule

public class SirTypeProviderImpl(
    private val sirSession: SirSession,
    override val errorTypeStrategy: ErrorTypeStrategy,
    override val unsupportedTypeStrategy: ErrorTypeStrategy,
) : SirTypeProvider {

    override fun KaType.translateType(
        ktAnalysisSession: KaSession,
        reportErrorType: (String) -> Nothing,
        reportUnsupportedType: () -> Nothing,
        processTypeImports: (List<SirImport>) -> Unit,
    ): SirType =
        buildSirNominalType(this@translateType, ktAnalysisSession)
            .handleErrors(reportErrorType, reportUnsupportedType)
            .handleImports(ktAnalysisSession, processTypeImports)

    @OptIn(KaAnalysisNonPublicApi::class)
    private fun buildSirNominalType(ktType: KaType, ktAnalysisSession: KaSession): SirType {
        fun buildPrimitiveType(ktType: KaType): SirType? = with(ktAnalysisSession) {
            when {
                ktType.isCharType -> SirUnsupportedType()
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
                ktType.isNothingType -> SirNominalType(SirSwiftModule.never)
                else -> null
            }
        }

        fun buildRegularType(ktType: KaType): SirType = with (ktAnalysisSession) {
            when (ktType) {
                is KaUsualClassType -> with(sirSession) {
                    if (ktType.isAnyType && !ktType.isMarkedNullable) {
                        SirNominalType(KotlinRuntimeModule.kotlinBase)
                    } else {
                        val classSymbol = ktType.symbol
                        if (classSymbol.sirVisibility(ktAnalysisSession) == SirVisibility.PUBLIC) {
                            SirNominalType(classSymbol.sirDeclaration() as SirNamedDeclaration)
                        } else {
                            SirUnsupportedType()
                        }
                    }
                }
                is KaFunctionType,
                is KaTypeParameterType,
                -> SirUnsupportedType()
                is KaErrorType -> SirErrorType(ktType.errorMessage)
                else -> SirErrorType("Unexpected type ${ktType}")
            }
        }

        return ktType.abbreviatedType?.let { buildRegularType(it) }
            ?: buildPrimitiveType(ktType)
            ?: buildRegularType(ktType)
    }

    private fun SirType.handleErrors(
        reportErrorType: (String) -> Nothing,
        reportUnsupportedType: () -> Nothing,
    ): SirType {
        if (this is SirErrorType && sirSession.errorTypeStrategy == ErrorTypeStrategy.Fail) {
            reportErrorType(this.reason)
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
            when (val origin = type.origin) {
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
        }
        return this
    }
}

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.analysis.api.KaAnalysisNonPublicApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaSymbolWithVisibility
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.SirTypeProvider
import org.jetbrains.kotlin.sir.providers.SirTypeProvider.ErrorTypeStrategy
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
                ktType.isNothingType -> SirSwiftModule.never
                else -> null
            }?.let { primitiveType ->
                SirNominalType(primitiveType)
            }
        }

        fun buildRegularType(ktType: KaType): SirType = when (ktType) {
            is KaUsualClassType -> with(sirSession) {
                when (val classSymbol = ktType.symbol) {
                    is KaSymbolWithVisibility -> {
                        if (classSymbol.sirVisibility(ktAnalysisSession) == SirVisibility.PUBLIC) {
                            SirNominalType(classSymbol.sirDeclaration() as SirNamedDeclaration)
                        } else {
                            // Mapping all unexported types to KotlinBase
                            SirNominalType(KotlinRuntimeModule.kotlinBase)
                        }
                    }
                    else -> SirUnsupportedType()
                }
            }
            is KaFunctionType,
            is KaTypeParameterType,
            -> SirUnsupportedType()
            is KaErrorType -> SirErrorType(ktType.errorMessage)
            else -> SirErrorType("Unexpected type $ktType")
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
                else -> {}
            }
        }
        return this
    }
}

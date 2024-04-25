/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.SirTypeProvider
import org.jetbrains.kotlin.sir.providers.UnknownTypeStrategy
import org.jetbrains.kotlin.sir.providers.UnsupportedTypeStrategy
import org.jetbrains.kotlin.sir.util.SirSwiftModule

public class SirTypeProviderImpl(
    private val unsupportedTypeStrategy: UnsupportedTypeStrategy,
    private val unknownTypeStrategy: UnknownTypeStrategy,
    private val sirSession: SirSession,
) : SirTypeProvider {

    override fun translateType(
        request: SirTypeProvider.TranslationRequest,
        ktAnalysisSession: KtAnalysisSession,
    ): SirTypeProvider.TranslationResponse =
        buildSirNominalType(request.ktType, ktAnalysisSession)

    private fun buildSirNominalType(kotlinType: KtType, ktAnalysisSession: KtAnalysisSession): SirTypeProvider.TranslationResponse =
        with(ktAnalysisSession) {
            val builtInType = when {
                kotlinType.isUnit -> SirSwiftModule.void

                kotlinType.isByte -> SirSwiftModule.int8
                kotlinType.isShort -> SirSwiftModule.int16
                kotlinType.isInt -> SirSwiftModule.int32
                kotlinType.isLong -> SirSwiftModule.int64

                kotlinType.isUByte -> SirSwiftModule.uint8
                kotlinType.isUShort -> SirSwiftModule.uint16
                kotlinType.isUInt -> SirSwiftModule.uint32
                kotlinType.isULong -> SirSwiftModule.uint64

                kotlinType.isBoolean -> SirSwiftModule.bool

                kotlinType.isDouble -> SirSwiftModule.double
                kotlinType.isFloat -> SirSwiftModule.float
                else -> null
            }
            if (builtInType != null) {
                return SirTypeProvider.TranslationResponse.Success(SirStructType(builtInType), listOf(SirSwiftModule.name))
            }
            return when (kotlinType) {
                is KtUsualClassType -> handleUsualClassType(kotlinType, ktAnalysisSession)
                is KtClassErrorType -> handleErrorType()
                is KtCapturedType,
                is KtFunctionalType,
                is KtDefinitelyNotNullType,
                is KtDynamicType,
                is KtTypeErrorType,
                is KtFlexibleType,
                is KtIntegerLiteralType,
                is KtIntersectionType,
                is KtTypeParameterType,
                -> handleUnsupportedType()
            }
        }

    private fun handleUsualClassType(
        kotlinType: KtUsualClassType,
        ktAnalysisSession: KtAnalysisSession,
    ): SirTypeProvider.TranslationResponse = with(ktAnalysisSession) {
        val classSymbol = kotlinType.classSymbol
        return with(sirSession) {
            val type = when (val sirDeclaration = classSymbol.sirDeclaration() as SirNamedDeclaration) {
                is SirClass -> SirClassType(sirDeclaration)
                is SirStruct -> SirStructType(sirDeclaration)
                is SirEnum -> SirEnumType(sirDeclaration)
                else -> return SirTypeProvider.TranslationResponse.Unsupported()
            }
            val module = classSymbol.getContainingModule().sirModule()
            SirTypeProvider.TranslationResponse.Success(type, listOf(module.name))
        }
    }

    private fun handleUnsupportedType(): SirTypeProvider.TranslationResponse = when (unsupportedTypeStrategy) {
        UnsupportedTypeStrategy.Fail -> SirTypeProvider.TranslationResponse.Unsupported()
        UnsupportedTypeStrategy.SpecialType -> SirTypeProvider.TranslationResponse.Success(SirUnsupportedType, emptyList())
    }

    private fun handleErrorType(): SirTypeProvider.TranslationResponse = when (unknownTypeStrategy) {
        UnknownTypeStrategy.Fail -> SirTypeProvider.TranslationResponse.Unknown()
        UnknownTypeStrategy.SpecialType -> SirTypeProvider.TranslationResponse.Success(SirUnknownType, emptyList())
    }
}

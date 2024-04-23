/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.sir.SirNamedDeclaration
import org.jetbrains.kotlin.sir.SirNominalType
import org.jetbrains.kotlin.sir.SirUnknownType
import org.jetbrains.kotlin.sir.SirUnsupportedType
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.SirTypeProvider
import org.jetbrains.kotlin.sir.providers.UnknownTypeStrategy
import org.jetbrains.kotlin.sir.providers.UnsupportedTypeStrategy
import org.jetbrains.kotlin.sir.providers.utils.withSirAnalyse
import org.jetbrains.kotlin.sir.util.SirSwiftModule

public class SirTypeProviderImpl(
    private val unsupportedTypeStrategy: UnsupportedTypeStrategy,
    private val unknownTypeStrategy: UnknownTypeStrategy,
    private val ktAnalysisSession: KtAnalysisSession,
    private val sirSession: SirSession,
) : SirTypeProvider {

    override fun translateType(request: SirTypeProvider.TranslationRequest): SirTypeProvider.TranslationResponse =
        withSirAnalyse(sirSession, ktAnalysisSession) { buildSirNominalType(request.ktType) }

    context(KtAnalysisSession, SirSession)
    private fun buildSirNominalType(kotlinType: KtType): SirTypeProvider.TranslationResponse {
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
            return SirTypeProvider.TranslationResponse.Success(SirNominalType(builtInType), listOf(SirSwiftModule.name))
        }
        return when (kotlinType) {
            is KtUsualClassType -> handleUsualClassType(kotlinType)
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

    context(KtAnalysisSession, SirSession)
    private fun handleUsualClassType(kotlinType: KtUsualClassType): SirTypeProvider.TranslationResponse {
        val classSymbol = kotlinType.classSymbol
        val sirDeclaration = classSymbol.sirDeclaration() as SirNamedDeclaration
        val module = classSymbol.getContainingModule().sirModule()
        return SirTypeProvider.TranslationResponse.Success(SirNominalType(sirDeclaration), listOf(module.name))
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

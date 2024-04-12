/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.sir.SirNamedDeclaration
import org.jetbrains.kotlin.sir.SirNominalType
import org.jetbrains.kotlin.sir.SirType
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.SirTypeProvider
import org.jetbrains.kotlin.sir.providers.utils.withSirAnalyse
import org.jetbrains.kotlin.sir.util.SirSwiftModule

public class SirTypeProviderImpl(
    private val ktAnalysisSession: KtAnalysisSession,
    private val sirSession: SirSession,
) : SirTypeProvider {
    override fun KtType.translateType(): SirType = withSirAnalyse(sirSession, ktAnalysisSession) {
        buildSirNominalType(this@translateType)
    }

    context(KtAnalysisSession, SirSession)
    private fun buildSirNominalType(it: KtType): SirType = SirNominalType(
        when {
            it.isUnit -> SirSwiftModule.void

            it.isByte -> SirSwiftModule.int8
            it.isShort -> SirSwiftModule.int16
            it.isInt -> SirSwiftModule.int32
            it.isLong -> SirSwiftModule.int64

            it.isUByte -> SirSwiftModule.uint8
            it.isUShort -> SirSwiftModule.uint16
            it.isUInt -> SirSwiftModule.uint32
            it.isULong -> SirSwiftModule.uint64

            it.isBoolean -> SirSwiftModule.bool

            it.isDouble -> SirSwiftModule.double
            it.isFloat -> SirSwiftModule.float

            else -> when (it) {
                is KtUsualClassType -> {
                    it.classSymbol.sirDeclaration() as SirNamedDeclaration
                }
                is KtCapturedType,
                is KtClassErrorType,
                is KtFunctionalType,
                is KtDefinitelyNotNullType,
                is KtDynamicType,
                is KtTypeErrorType,
                is KtFlexibleType,
                is KtIntegerLiteralType,
                is KtIntersectionType,
                is KtTypeParameterType -> error("Swift Export does not support argument type: $it")
            }
        }
    )
}

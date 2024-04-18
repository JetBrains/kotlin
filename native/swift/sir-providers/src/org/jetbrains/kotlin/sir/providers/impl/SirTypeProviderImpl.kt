/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.sir.SirErrorType
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
    private val unresolvedTypeBehaviour: UnresolvedTypeBehavior,
) : SirTypeProvider {
    public enum class UnresolvedTypeBehavior {
        Fail,
        CreateErrorType,
    }

    override fun KtType.translateType(): SirType = withSirAnalyse(sirSession, ktAnalysisSession) {
        buildSirNominalType(this@translateType)
    }

    context(KtAnalysisSession, SirSession)
    private fun buildSirNominalType(ktType: KtType): SirType {
        when {
            ktType.isUnit -> SirSwiftModule.void

            ktType.isByte -> SirSwiftModule.int8
            ktType.isShort -> SirSwiftModule.int16
            ktType.isInt -> SirSwiftModule.int32
            ktType.isLong -> SirSwiftModule.int64

            ktType.isUByte -> SirSwiftModule.uint8
            ktType.isUShort -> SirSwiftModule.uint16
            ktType.isUInt -> SirSwiftModule.uint32
            ktType.isULong -> SirSwiftModule.uint64

            ktType.isBoolean -> SirSwiftModule.bool

            ktType.isDouble -> SirSwiftModule.double
            ktType.isFloat -> SirSwiftModule.float
            else -> null
        }?.let {
            return SirNominalType(it)
        }

        if (ktType is KtUsualClassType) {
            return SirNominalType(ktType.classSymbol.sirDeclaration() as SirNamedDeclaration)
        }
        if (ktType is KtClassErrorType) {
            when (unresolvedTypeBehaviour) {
                UnresolvedTypeBehavior.Fail -> error("Unresolved class reference: ${ktType.errorMessage}")
                UnresolvedTypeBehavior.CreateErrorType -> return SirErrorType
            }
        }
        error("Swift Export does not support argument type: $ktType")
    }
}

/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.analysisapi.origin

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.sir.*

interface AASirOrigin : SirDeclarationOrigin

class AASirFunctionOrigin(
    private val element: KtFunction,
) : KotlinFunctionSirOrigin, AASirOrigin {

    override val fqName: List<String> by lazy {
        element.fqName?.pathSegments()?.map { it.asString() } ?: emptyList()
    }

    override val returnType: SirType by lazy {
        analyze(element) {
            val returnKtType = element.getReturnKtType()
            mapType(returnKtType)
        }
    }

    override val parameters: List<SirParameter> by lazy {
        analyze(element) {
            buildList {
                element.valueParameters.map { parameter ->
                    val name = parameter.name
                    val type = mapType(parameter.getKtType()!!)
                    add(SirParameter(argumentName = name, type = type))
                }
            }
        }
    }

    context(KtAnalysisSession)
    private fun mapType(ktType: KtType): SirType = when {
        ktType.isBoolean -> BuiltinSirTypeDeclaration.Bool
        ktType.isByte -> BuiltinSirTypeDeclaration.Int8
        ktType.isShort -> BuiltinSirTypeDeclaration.Int16
        ktType.isInt -> BuiltinSirTypeDeclaration.Int32
        ktType.isLong -> BuiltinSirTypeDeclaration.Int64
        else -> error("Type is not supported yet: ${ktType}")
    }.let(::SirNominalType)
}


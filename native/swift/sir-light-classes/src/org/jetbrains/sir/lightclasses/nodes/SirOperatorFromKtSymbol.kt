/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.nodes

import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.sir.SirDeclaration
import org.jetbrains.kotlin.sir.SirNamedDeclaration
import org.jetbrains.kotlin.sir.SirNominalType
import org.jetbrains.kotlin.sir.SirParameter
import org.jetbrains.kotlin.sir.SirType
import org.jetbrains.kotlin.sir.providers.SirSession

internal open class SirBinaryMathOperatorFromKtSymbol(
    override val ktSymbol: KaNamedFunctionSymbol,
    override val sirSession: SirSession,
) : SirFunctionFromKtSymbol(ktSymbol, sirSession) {
//    override val isInstance: Boolean get() = false
}

internal open class SirUnaryMathOperatorFromKtSymbol(
    override val ktSymbol: KaNamedFunctionSymbol,
    override val sirSession: SirSession,
) : SirFunctionFromKtSymbol(ktSymbol, sirSession) {
//    override val isInstance: Boolean get() = false

//    override val parameters: List<SirParameter>
//        get() = listOf(SirParameter(parameterName = "self", type = SirNominalType(typeDeclaration = parent as SirNamedDeclaration))) + super.parameters
}
/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.analysisapi.transformers

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.buildForeignFunction

internal fun KtNamedFunction.toForeignFunction(): SirForeignFunction = buildForeignFunction {
    origin = AAFunction(this@toForeignFunction)
}

private fun KtValueParameterSymbol.toSirParam(): SirKotlinOrigin.Parameter = AAParameter(
    name = name.toString(),
    type = AAKotlinType(name = returnType.toString())
)

private class AAFunction(
    private val originalFunction: KtNamedFunction
) : SirKotlinOrigin.Function {
    override val fqName: List<String>
        get() = originalFunction.fqName?.pathSegments()?.toListString() ?: emptyList()

    override val parameters: List<SirKotlinOrigin.Parameter>
        get() = analyze(originalFunction) {
            val function = originalFunction.getFunctionLikeSymbol()
            function.valueParameters.map { it.toSirParam() }
        }

    override val returnType: SirKotlinOrigin.Type
        get() = analyze(originalFunction) {
            val function = originalFunction.getFunctionLikeSymbol()
            AAKotlinType(name = function.returnType.toString())
        }

}
private data class AAParameter(
    override val name: String,
    override val type: SirKotlinOrigin.Type
) : SirKotlinOrigin.Parameter

private data class AAKotlinType(
    override val name: String
) : SirKotlinOrigin.Type

private fun List<Name>.toListString() = map { it.asString() }

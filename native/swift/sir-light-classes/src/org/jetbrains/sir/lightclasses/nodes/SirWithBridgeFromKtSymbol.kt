/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.nodes

import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.sir.SirDeclarationParent
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.sir.SirParameter
import org.jetbrains.kotlin.sir.SirType
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.generateFunctionBridge
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.BridgeFunctionProxy
import org.jetbrains.kotlin.sir.providers.withSessions
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import org.jetbrains.sir.lightclasses.SirFromKtSymbol
import org.jetbrains.sir.lightclasses.utils.forBridge

internal interface SirWithBridgeFromKtSymbol<S : KaFunctionSymbol> : SirFromKtSymbol<S> {

    val parameters: List<SirParameter>
    val extensionReceiverParameter: SirParameter?

    val returnType: SirType
    val errorType: SirType

    val isInstance: Boolean

    val parent: SirDeclarationParent

    fun generateFunctionBridge(
        session: SirSession,
        fqName: List<String>,
        suffix: String,
        selfType: SirType?,
    ): BridgeFunctionProxy? = session.withSessions {
        val baseName = fqName.forBridge.joinToString("_") + suffix

        val extensionReceiverParameter = extensionReceiverParameter?.let {
            SirParameter("", "receiver", it.type)
        }

        generateFunctionBridge(
            baseBridgeName = baseName,
            explicitParameters = listOfNotNull(extensionReceiverParameter) + parameters,
            returnType = returnType,
            kotlinFqName = fqName,
            selfParameter = (parent !is SirModule && isInstance).ifTrue {
                SirParameter("", "self", selfType ?: error("Only a member can have a self parameter"))
            },
            extensionReceiverParameter = extensionReceiverParameter,
            errorParameter = errorType.takeIf { it != SirType.never }?.let {
                SirParameter("", "_out_error", it)
            },
        )
    }
}

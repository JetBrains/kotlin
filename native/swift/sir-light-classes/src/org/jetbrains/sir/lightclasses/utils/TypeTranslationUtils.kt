/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.utils

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.containingModule
import org.jetbrains.kotlin.analysis.api.components.render
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.contextParameters
import org.jetbrains.kotlin.sir.SirParameter
import org.jetbrains.kotlin.sir.SirType
import org.jetbrains.kotlin.sir.SirTypeVariance
import org.jetbrains.kotlin.sir.escaping
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.sirModule
import org.jetbrains.kotlin.sir.providers.source.KotlinParameterOrigin
import org.jetbrains.kotlin.sir.providers.translateType
import org.jetbrains.kotlin.sir.providers.utils.updateImports
import org.jetbrains.sir.lightclasses.SirFromKtSymbol
import org.jetbrains.sir.lightclasses.extensions.withSessions
import org.jetbrains.sir.lightclasses.nodes.SirFunctionFromKtPropertySymbol

@OptIn(KaExperimentalApi::class)
internal inline fun <reified T : KaCallableSymbol> SirFromKtSymbol<T>.translateReturnType(): SirType {
    return withSessions {
        this@translateReturnType.ktSymbol.returnType.translateType(
            SirTypeVariance.COVARIANT,
            reportErrorType = { error("Can't translate return type in ${ktSymbol.render()}: ${it}") },
            reportUnsupportedType = { error("Can't translate return type in ${ktSymbol.render()}: type is not supported") },
            processTypeImports = this@translateReturnType.ktSymbol.containingModule.sirModule()::updateImports
        )
    }
}

internal inline fun <reified T : KaFunctionSymbol> SirFromKtSymbol<T>.translateParameters(): List<SirParameter> {
    return withSessions {
        this@translateParameters.ktSymbol.valueParameters.map { parameter ->
            val sirType = createParameterType(ktSymbol, parameter).escaping
            SirParameter(
                argumentName = parameter.name.asString(),
                type = sirType,
                origin = KotlinParameterOrigin.ValueParameter(parameter),
                isVariadic = parameter.isVararg,
            )
        }
    }
}

internal inline fun <reified T : KaCallableSymbol> SirFromKtSymbol<T>.translateExtensionParameter(): SirParameter? {
    return withSessions {
        this@translateExtensionParameter.ktSymbol.receiverParameter?.let { receiver ->
            val sirType = createParameterType(ktSymbol, receiver)
            SirParameter(
                parameterName = receiver.name.asStringStripSpecialMarkers(),
                type = sirType,
                origin = KotlinParameterOrigin.ReceiverParameter(receiver)
            )
        }
    }
}

internal inline fun <reified T : KaCallableSymbol> SirFromKtSymbol<T>.translateContextParameters(): List<SirParameter> {
    return withSessions {
        val symbol = when (this@translateContextParameters) {
            is SirFunctionFromKtPropertySymbol -> ktPropertySymbol
            else -> ktSymbol
        }
        symbol.contextParameters.map { parameter ->
            val sirType = createParameterType(ktSymbol, parameter)
            SirParameter(
                parameterName = parameter.name.identifierOrNullIfSpecial,
                type = sirType,
            )
        }
    }
}

@OptIn(KaExperimentalApi::class)
context(ka: KaSession, sir: SirSession)
private fun <P : KaParameterSymbol> createParameterType(ktSymbol: KaDeclarationSymbol, parameter: P): SirType {
    return parameter.returnType.translateType(
        position = SirTypeVariance.CONTRAVARIANT,
        reportErrorType = { error("Can't translate parameter ${parameter.render()} type in ${ktSymbol.render()}: $it") },
        reportUnsupportedType = { error("Can't translate parameter ${parameter.render()} type in ${ktSymbol.render()}: type is not supported") },
        processTypeImports = ktSymbol.containingModule.sirModule()::updateImports
    )
}

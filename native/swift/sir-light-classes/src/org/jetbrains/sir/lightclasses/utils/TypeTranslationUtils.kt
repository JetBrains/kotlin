/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.utils

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.sir.SirAttribute
import org.jetbrains.kotlin.sir.SirFunctionalType
import org.jetbrains.kotlin.sir.SirParameter
import org.jetbrains.kotlin.sir.SirType
import org.jetbrains.kotlin.sir.providers.source.KotlinParameterOrigin
import org.jetbrains.kotlin.sir.providers.utils.updateImports
import org.jetbrains.sir.lightclasses.SirFromKtSymbol
import org.jetbrains.sir.lightclasses.extensions.SirAndKaSession
import org.jetbrains.sir.lightclasses.extensions.withSessions
import org.jetbrains.sir.lightclasses.nodes.SirInitFromKtSymbol

@OptIn(KaExperimentalApi::class)
internal inline fun <reified T : KaCallableSymbol> SirFromKtSymbol<T>.translateReturnType(): SirType {
    return withSessions {
        this@translateReturnType.ktSymbol.returnType.translateType(
            useSiteSession,
            reportErrorType = { error("Can't translate return type in ${ktSymbol.render()}: ${it}") },
            reportUnsupportedType = { error("Can't translate return type in ${ktSymbol.render()}: type is not supported") },
            processTypeImports = this@translateReturnType.ktSymbol.containingModule.sirModule()::updateImports
        )
    }
}

internal inline fun <reified T : KaFunctionSymbol> SirFromKtSymbol<T>.translateParameters(): List<SirParameter> {
    return withSessions {

        val sirFromKtSymbol = this@translateParameters
        val outerParamOfInnerClass = if (sirFromKtSymbol is SirInitFromKtSymbol && sirFromKtSymbol.isInner) {
            val outSymbol = (ktSymbol.containingSymbol!!.containingSymbol as KaNamedClassSymbol)
            val outType = outSymbol.defaultType.translateType(
                this.useSiteSession,
                { error("Error translating type") },
                { error("Unsupported type") },
                {})
            val param = SirParameter(argumentName = "outer", type = outType)
            param
        } else null

        sirFromKtSymbol.ktSymbol.valueParameters.map { parameter ->
            val sirType = createParameterType(ktSymbol, parameter)
                .let {
                    if (it is SirFunctionalType) {
                        return@let SirFunctionalType(
                            parameterTypes = it.parameterTypes,
                            returnType = it.returnType,
                            attributes = it.attributes + listOf(SirAttribute.Escaping)
                        )
                    } else {
                        it
                    }
                }
            SirParameter(argumentName = parameter.name.asString(), type = sirType, origin = KotlinParameterOrigin.ValueParameter(parameter))
        } + listOfNotNull(outerParamOfInnerClass)
    }
}

internal inline fun <reified T : KaCallableSymbol> SirFromKtSymbol<T>.translateExtensionParameter(): SirParameter? {
    return withSessions {
        this@translateExtensionParameter.ktSymbol.receiverParameter?.let { receiver ->
            val sirType = createParameterType(ktSymbol, receiver)
            SirParameter(
                argumentName = receiver.name.asStringStripSpecialMarkers(),
                type = sirType,
                origin = KotlinParameterOrigin.ReceiverParameter(receiver)
            )
        }
    }
}

@OptIn(KaExperimentalApi::class)
private fun <P: KaParameterSymbol> SirAndKaSession.createParameterType(ktSymbol: KaDeclarationSymbol, parameter: P): SirType {
    return parameter.returnType.translateType(
        useSiteSession,
        reportErrorType = { error("Can't translate parameter ${parameter.render()} type in ${ktSymbol.render()}: $it") },
        reportUnsupportedType = { error("Can't translate parameter ${parameter.render()} type in ${ktSymbol.render()}: type is not supported") },
        processTypeImports = ktSymbol.containingModule.sirModule()::updateImports
    )
}
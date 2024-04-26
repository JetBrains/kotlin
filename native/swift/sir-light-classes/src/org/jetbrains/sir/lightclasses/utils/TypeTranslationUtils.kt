/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.utils

import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionLikeSymbol
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.providers.utils.updateImports
import org.jetbrains.sir.lightclasses.SirFromKtSymbol
import org.jetbrains.sir.lightclasses.extensions.withSessions

internal inline fun <reified T : KtCallableSymbol> SirFromKtSymbol<T>.translateReturnType(): SirType {
    return withSessions {
        this@translateReturnType.ktSymbol.returnType.translateType(
            analysisSession,
            reportErrorType = { error("Can't translate return type in ${ktSymbol.render()}: ${it}") },
            reportUnsupportedType = { error("Can't translate return type in ${ktSymbol.render()}: type is not supported") },
            processTypeImports = this@translateReturnType.ktSymbol.getContainingModule().sirModule()::updateImports
        )
    }
}

internal inline fun <reified T : KtFunctionLikeSymbol> SirFromKtSymbol<T>.translateParameters(): List<SirParameter> {
    return withSessions {
        this@translateParameters.ktSymbol.valueParameters.map { parameter ->
            val sirType = parameter.returnType.translateType(
                analysisSession,
                reportErrorType = { error("Can't translate parameter ${parameter.render()} type in ${ktSymbol.render()}: $it") },
                reportUnsupportedType = { error("Can't translate parameter ${parameter.render()} type in ${ktSymbol.render()}: type is not supported") },
                processTypeImports = this@translateParameters.ktSymbol.getContainingModule().sirModule()::updateImports
            )
            SirParameter(argumentName = parameter.name.asString(), type = sirType)
        }
    }
}
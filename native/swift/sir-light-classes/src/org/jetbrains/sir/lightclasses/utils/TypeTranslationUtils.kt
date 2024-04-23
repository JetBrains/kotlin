/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.utils

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.sir.SirType
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.SirTypeProvider
import org.jetbrains.sir.lightclasses.SirFromKtSymbol

context(SirSession, KtAnalysisSession)
internal fun <T : KtCallableSymbol> SirFromKtSymbol<T>.translateReturnType(): SirType {
    val typeRequest = SirTypeProvider.TranslationRequest(ktSymbol.returnType)
    return when (val response = translateType(typeRequest)) {
        is SirTypeProvider.TranslationResponse.Success -> response.sirType
        is SirTypeProvider.TranslationResponse.Unknown ->
            error("Return type ${ktSymbol.returnType} in ${ktSymbol.render()} is not found.")
        is SirTypeProvider.TranslationResponse.Unsupported ->
            error("Return type ${ktSymbol.returnType} in ${ktSymbol.render()} is not supported.")
    }
}

context(SirSession, KtAnalysisSession)
internal fun <T : KtCallableSymbol> SirFromKtSymbol<T>.translateParameterType(valueParameter: KtValueParameterSymbol): SirType {
    val typeRequest = SirTypeProvider.TranslationRequest(valueParameter.returnType)
    return when (val response = translateType(typeRequest)) {
        is SirTypeProvider.TranslationResponse.Success -> {
            // TODO: import dependencies

            response.sirType
        }
        is SirTypeProvider.TranslationResponse.Unknown -> {
            error("Parameter ${valueParameter.render()} in ${ktSymbol.render()} is not found.")
        }
        is SirTypeProvider.TranslationResponse.Unsupported -> {
            error("Parameter type ${ktSymbol.returnType} in ${ktSymbol.render()} is not supported.")
        }
    }
}
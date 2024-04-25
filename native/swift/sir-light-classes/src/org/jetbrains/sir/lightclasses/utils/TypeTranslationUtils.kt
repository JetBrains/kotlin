/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.utils

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.sir.SirType
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.SirTypeProvider
import org.jetbrains.kotlin.sir.util.updateImports
import org.jetbrains.sir.lightclasses.SirFromKtSymbol

context(SirSession, KtAnalysisSession)
internal fun <T : KtCallableSymbol> SirFromKtSymbol<T>.translateReturnType(): SirType {
    val typeRequest = SirTypeProvider.TranslationRequest(ktSymbol.returnType)
    return when (val response = translateType(typeRequest, analysisSession)) {
        is SirTypeProvider.TranslationResponse.Success -> {
            response.updateImportsAndReturnType(ktSymbol)
        }
        is SirTypeProvider.TranslationResponse.Unknown ->
            error("Return type ${ktSymbol.returnType} in ${ktSymbol.render()} is not found.")
        is SirTypeProvider.TranslationResponse.Unsupported ->
            error("Return type ${ktSymbol.returnType} in ${ktSymbol.render()} is not supported.")
    }
}

context(SirSession, KtAnalysisSession)
internal fun <T : KtCallableSymbol> SirFromKtSymbol<T>.translateParameterType(valueParameter: KtValueParameterSymbol): SirType {
    val typeRequest = SirTypeProvider.TranslationRequest(valueParameter.returnType)
    return when (val response = translateType(typeRequest, analysisSession)) {
        is SirTypeProvider.TranslationResponse.Success -> {
            response.updateImportsAndReturnType(ktSymbol)
        }
        is SirTypeProvider.TranslationResponse.Unknown -> {
            error("Parameter ${valueParameter.render()} in ${ktSymbol.render()} is not found.")
        }
        is SirTypeProvider.TranslationResponse.Unsupported -> {
            error("Parameter ${valueParameter.render()} in ${ktSymbol.render()} is not supported.")
        }
    }
}

/**
 * Unpacks the TranslationResponse.Success object into a SirType, and updates the corresponding module imports.
 *
 * @param useSite The KtSymbol where the translation is being used.
 * @return The unpacked SirType.
 */
context(SirSession, KtAnalysisSession)
public fun SirTypeProvider.TranslationResponse.Success.updateImportsAndReturnType(useSite: KtSymbol): SirType {
    val sirModule = useSite.getContainingModule().sirModule()
    sirModule.updateImports(requiredModules)
    return sirType
}
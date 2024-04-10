/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.providers.SirDeclarationProvider
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.utils.withSirAnalyse
import org.jetbrains.sir.lightclasses.nodes.*
import org.jetbrains.sir.lightclasses.nodes.SirClassFromKtSymbol
import org.jetbrains.sir.lightclasses.nodes.SirFunctionFromKtSymbol
import org.jetbrains.sir.lightclasses.nodes.SirVariableFromKtSymbol

public class SirDeclarationFromKtSymbolProvider(
    private val ktAnalysisSession: KtAnalysisSession,
    private val sirSession: SirSession,
) : SirDeclarationProvider {

    override fun KtDeclarationSymbol.sirDeclaration(): SirDeclaration = withSirAnalyse(sirSession, ktAnalysisSession) {
        when (val ktSymbol = this@sirDeclaration) {
            is KtNamedClassOrObjectSymbol -> {
                SirClassFromKtSymbol(
                    ktSymbol = ktSymbol,
                    analysisApiSession = ktAnalysisSession,
                    sirSession = sirSession,
                )
            }
            is KtConstructorSymbol -> {
                SirInitFromKtSymbol(
                    ktSymbol = ktSymbol,
                    analysisApiSession = ktAnalysisSession,
                    sirSession = sirSession,
                )
            }
            is KtFunctionLikeSymbol -> {
                SirFunctionFromKtSymbol(
                    ktSymbol = ktSymbol,
                    analysisApiSession = ktAnalysisSession,
                    sirSession = sirSession,
                )
            }
            is KtVariableSymbol -> {
                SirVariableFromKtSymbol(
                    ktSymbol = ktSymbol,
                    analysisApiSession = ktAnalysisSession,
                    sirSession = sirSession,
                )
            }
            else -> TODO("encountered unknown symbol type - $ktSymbol. Error system should be reworked KT-65980")
        }
    }
}

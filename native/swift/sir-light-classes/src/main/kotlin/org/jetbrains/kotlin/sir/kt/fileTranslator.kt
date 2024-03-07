/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.kt

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithVisibility
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.kt.SirSession
import org.jetbrains.kotlin.sir.kt.nodes.SirClassFromSymbol
import org.jetbrains.kotlin.sir.kt.nodes.SirFunctionFromSymbol
import org.jetbrains.kotlin.sir.kt.nodes.SirVariableFromSymbol


context(KtAnalysisSession, SirSession)
public fun KtFileSymbol.toSirDeclarations(): List<SirDeclaration> {
    val res = mutableListOf<SirDeclaration>()
    this.getFileScope().apply {
        getCallableSymbols().mapNotNullTo(res) {
            if (it !is KtSymbolWithVisibility || it.sirVisibility() != SirVisibility.PUBLIC) {
                return@mapNotNullTo null
            }
            val sirDeclaration: SirDeclaration? = when (it) {
                is KtFunctionSymbol -> SirFunctionFromSymbol(it, analysisSession, sirSession)
                is KtKotlinPropertySymbol -> SirVariableFromSymbol(it, analysisSession, sirSession)
                else -> null
            }
            sirDeclaration
        }
        getClassifierSymbols().mapNotNullTo(res) {
            if (it !is KtSymbolWithVisibility || it.sirVisibility() != SirVisibility.PUBLIC) {
                return@mapNotNullTo null
            }
            when (it) {
                is KtNamedClassOrObjectSymbol -> SirClassFromSymbol(it, analysisSession, sirSession)
                else -> null
            }
        }
    }
    return res.toList()
}
/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.ide

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols.AbstractSymbolTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols.SymbolsData
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.sir.SirMutableDeclarationContainer
import org.jetbrains.kotlin.sir.util.addChild
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.sir.printer.SirAsSwiftSourcesPrinter

abstract class AbstractSymbolToSirTest : AbstractSymbolTest() {
    override fun KaSession.collectSymbols(ktFile: KtFile, testServices: TestServices): SymbolsData {
        return SymbolsData(
            listOf(
                testServices.expressionMarkerProvider.getElementOfTypeAtCaret<KtDeclaration>(ktFile).symbol
            ),
            symbolsForPrettyRendering = emptyList(),
        )
    }

    override fun KaSession.renderSymbolForComparison(symbol: KaSymbol, directives: RegisteredDirectives): String {
        if (symbol is KaValueParameterSymbol) return ""
        require(symbol is KaDeclarationSymbol)
        return withSirSession {
            SirAsSwiftSourcesPrinter.print(
                module = symbol.containingModule.sirModule().also {
                    val resultedDeclaration = symbol.sirDeclaration()
                    val parent = resultedDeclaration.parent as? SirMutableDeclarationContainer
                        ?: error("top level declaration can contain only module or extension to package as a parent")
                    parent.addChild { resultedDeclaration }
                },
                stableDeclarationsOrder = true,
                renderDocComments = false,
            )
        }
    }
}
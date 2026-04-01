/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.ide

import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.sir.SirDeclaration
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.sir.SirMutableDeclarationContainer
import org.jetbrains.kotlin.sir.util.addChild
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.sir.printer.SirPrinter

abstract class AbstractSymbolToSirTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        copyAwareAnalyzeForTest(mainFile) { contextFile ->
            val kaDeclaration = testServices
                .expressionMarkerProvider.getBottommostElementOfTypeAtCaret<KtDeclaration>(contextFile).symbol
            val actual: String = withSirSession(moduleToTranslate = kaDeclaration.containingModule) {
                kaDeclaration
                    .toSir()
                    .allDeclarations
                    .joinToString(separator = "\n") {
                        it.print(into = kaDeclaration.containingModule.sirModule())
                    }
            }
            testServices.assertions.assertEqualsToTestOutputFile(actual)
        }
    }
}

private fun SirDeclaration.print(into: SirModule): String = SirPrinter(
    stableDeclarationsOrder = true,
    renderDocComments = false,
).print(
    into.also {
        val parent = parent as? SirMutableDeclarationContainer
            ?: error("top level declaration can contain only module or extension to package as a parent")
        parent.addChild { this }
    }
).swiftSource.joinToString(separator = "\n")

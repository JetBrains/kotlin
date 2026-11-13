/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation.analyzer

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.diagnostics.KaSeverity
import org.jetbrains.kotlin.analysis.api.diagnostics.diagnostics
import org.jetbrains.kotlin.analysis.api.standalone.fir.test.configurators.StandaloneModeConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.test.services.TestServices

/**
 * Like [AbstractUniqueDeclarationIdGoldenTest], but the fixtures contain compiler errors. Each declaration ID
 * line is annotated with the ERROR diagnostics of that declaration, collected the same way the Analysis API
 * does it.
 *
 * This pins down how declaration ID generation behaves on code that does not compile: erroneous declarations
 * still get IDs, and the IDs stay unique even when the errors are about declarations clashing.
 */
abstract class AbstractDeclarationIdErrorsGoldenTest : AbstractAnalysisApiBasedTest() {
    override val configurator: AnalysisApiTestConfigurator =
        object : AnalysisApiTestConfigurator by StandaloneModeConfigurator() {
            override val testPrefixes: List<String> = emptyList()
        }

    override fun doTestByMainModuleAndOptionalMainFile(
        mainFile: KtFile?,
        mainModule: KtTestModule,
        testServices: TestServices,
    ) {
        val contextFile = mainFile ?: mainModule.ktFiles.firstOrNull() ?: error("The fixture must contain a Kotlin file")
        val actual = analyzeForTest(contextFile) {
            val errorsByDeclaration = HashMap<PsiElement, MutableList<String>>()
            for (file in mainModule.psiFiles.filterIsInstance<KtFile>()) {
                for (diagnostic in @OptIn(KaExperimentalApi::class) file.diagnostics()) {
                    if (diagnostic.severity != KaSeverity.ERROR) continue
                    val declaration = owningDeclaration(diagnostic.psi) ?: continue
                    errorsByDeclaration.getOrPut(declaration) { mutableListOf() }.add(diagnostic.factoryName)
                }
            }
            dumpUnhashedIds(mainModule.psiFiles, errorsByDeclaration)
        }
        assertEqualsToTestOutputFile(actual, ".txt")
    }

    /**
     * A diagnostic is reported on a precise element (a name reference, a type reference in a parameter, ...);
     * the ID lines are per declaration. The error is attributed to the nearest enclosing declaration, skipping
     * parameters and type parameters so that an error in a signature (e.g. an unresolved parameter type)
     * annotates the callable's line, not the parameter's.
     */
    private fun owningDeclaration(psi: PsiElement): KtDeclaration? =
        psi.parentsWithSelf
            .filterIsInstance<KtDeclaration>()
            .firstOrNull { it !is KtParameter && it !is KtTypeParameter }
}

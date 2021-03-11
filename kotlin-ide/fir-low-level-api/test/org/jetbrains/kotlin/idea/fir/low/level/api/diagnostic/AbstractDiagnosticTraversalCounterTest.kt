/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.diagnostic

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.idea.fir.low.level.api.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.idea.fir.low.level.api.api.collectDiagnosticsForFile
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getOrBuildFir
import org.jetbrains.kotlin.idea.fir.low.level.api.createResolveStateForNoCaching
import org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics.BeforeElementDiagnosticCollectionHandler
import org.jetbrains.kotlin.idea.fir.low.level.api.renderWithClassName
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

/**
 * Check that every declaration is visited exactly one time during diagnostic collection
 */
abstract class AbstractDiagnosticTraversalCounterTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun isFirPlugin(): Boolean = true

    fun doTest(path: String) {
        val testDataFile = File(path)
        val ktFile = myFixture.configureByText(testDataFile.name, FileUtil.loadFile(testDataFile)) as KtFile

        val handler = BeforeElementTestDiagnosticCollectionHandler()

        @OptIn(SessionConfiguration::class)
        val resolveState = createResolveStateForNoCaching(ktFile.getModuleInfo()) {
            register(BeforeElementDiagnosticCollectionHandler::class, handler)
        }

        // we should get diagnostics before we resolve the whole file by  ktFile.getOrBuildFir
        ktFile.collectDiagnosticsForFile(resolveState, DiagnosticCheckerFilter.ONLY_COMMON_CHECKERS)

        val firFile = ktFile.getOrBuildFir(resolveState)

        val errorElements = collectErrorElements(firFile, handler)

        if (errorElements.isNotEmpty()) {
            val zeroElements = errorElements.filter { it.second == 0 }
            val nonZeroElements = errorElements.filter { it.second > 1 }
            val message = buildString {
                if (zeroElements.isNotEmpty()) {
                    appendLine(
                        """ |The following elements were not visited 
                            |${zeroElements.joinToString(separator = "\n\n") { it.first.renderWithClassName() }}
                             """.trimMargin()
                    )
                }
                if (nonZeroElements.isNotEmpty()) {
                    appendLine(
                        """ |The following elements were visited more than one time
                            |${nonZeroElements.joinToString(separator = "\n\n") { it.second.toString() + " times " + it.first.renderWithClassName() }}
                             """.trimMargin()
                    )
                }
            }
            fail(message)
        }
    }

    private fun collectErrorElements(
        firFile: FirElement,
        handler: BeforeElementTestDiagnosticCollectionHandler
    ): List<Pair<FirElement, Int>> {
        val errorElements = mutableListOf<Pair<FirElement, Int>>()
        val nonDuplicatingElements = findNonDuplicatingFirElements(firFile)
        firFile.accept(object : FirVisitorVoid() {
            override fun visitElement(element: FirElement) {
                if (element !in nonDuplicatingElements) return
                val visitedTimes = handler.visitedTimes[element] ?: 0
                if (visitedTimes != 1) {
                    errorElements += element to visitedTimes
                }
                element.acceptChildren(this)
            }
        })
        return errorElements
    }

    private fun findNonDuplicatingFirElements(
        firFile: FirElement,
    ): Set<FirElement> {
        val elementUsageCount = mutableMapOf<FirElement, Int>()
        firFile.accept(object : FirVisitorVoid() {
            override fun visitElement(element: FirElement) {
                elementUsageCount.compute(element) { _, count -> (count ?: 0) + 1 }
            }
        })
        return elementUsageCount.filterValues { it == 1 }.keys
    }


    class BeforeElementTestDiagnosticCollectionHandler : BeforeElementDiagnosticCollectionHandler() {
        val visitedTimes = mutableMapOf<FirElement, Int>()
        override fun beforeCollectingForElement(element: FirElement) {
            if (!visitedTimes.containsKey(element)) {
                visitedTimes[element] = 1
            } else {
                visitedTimes.compute(element) { _, count -> (count ?: 0) + 1 }
            }
        }
    }
}

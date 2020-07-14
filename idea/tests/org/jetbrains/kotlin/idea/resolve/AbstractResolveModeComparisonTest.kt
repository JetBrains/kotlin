/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.resolve

import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.withCustomCompilerOptions
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

/**
 * This test runs analysis, then for each declaration and reference writes modes in which they have been resolved, if any.
 * "Targeted" resolve for selected expression is used for analysis.
 */
abstract class AbstractResolveModeComparisonTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    fun doTest(unused: String) {
        val testPath = testPath()
        val dumpResults = dump(testPath)

        val testPathNoExt = FileUtil.getNameWithoutExtension(testPath)
        KotlinTestUtils.assertEqualsToFile(File("$testPathNoExt.after"), dumpResults)
    }

    private fun dump(testPath: String): String {
        myFixture.configureByText(KotlinFileType.INSTANCE, File(testPath).readText())
        return withCustomCompilerOptions(myFixture.file.text, project, module) {
            val file = myFixture.file as KtFile
            val resolutionFacade = file.getResolutionFacade()

            val expression = findTargetExpression()
            val analysisResults: Map<BodyResolveMode, BindingContext> =
                RESOLVE_MODES.associateWith { resolveMode -> resolutionFacade.analyze(expression, resolveMode) }

            val markup: Map<KtElement, List<BodyResolveMode>> = markResolveModes(analysisResults)
            val offsets: MultiMap<Int, Mark> = findOffsets(markup)

            composeTextWithMarkup(offsets)
        }
    }

    private fun findTargetExpression(): KtExpression {
        val editor = myFixture.editor
        val selectionModel = editor.selectionModel

        return if (selectionModel.hasSelection()) {
            PsiTreeUtil.findElementOfClassAtRange(
                file,
                selectionModel.selectionStart,
                selectionModel.selectionEnd,
                KtExpression::class.java
            )
                ?: error("No KtExpression at selection range")
        } else {
            val offset = editor.caretModel.offset
            val element = file.findElementAt(offset)!!
            element.getNonStrictParentOfType<KtExpression>() ?: error("No KtExpression at caret")
        }
    }

    private fun markResolveModes(analysisResults: Map<BodyResolveMode, BindingContext>): Map<KtElement, List<BodyResolveMode>> {
        val modesPerformingResolveForElements = hashMapOf<KtElement, List<BodyResolveMode>>()
        file.accept(object : KtTreeVisitorVoid() {
            override fun visitReferenceExpression(reference: KtReferenceExpression) {
                super.visitReferenceExpression(reference)
                modesPerformingResolveForElements[reference] = analysisResults.keys.filter { bodyResolveMode ->
                    val bindingContext = analysisResults[bodyResolveMode] ?: error("No resolution results for mode: $bodyResolveMode")
                    bindingContext[BindingContext.REFERENCE_TARGET, reference] != null
                }
            }

            override fun visitDeclaration(declaration: KtDeclaration) {
                super.visitDeclaration(declaration)
                modesPerformingResolveForElements[declaration] = analysisResults.keys.filter { bodyResolveMode ->
                    val bindingContext = analysisResults[bodyResolveMode] ?: error("No resolution results for mode: $bodyResolveMode")
                    bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration] != null
                }
            }
        })

        return modesPerformingResolveForElements
    }

    private fun findOffsets(resolveModesMarkup: Map<KtElement, List<BodyResolveMode>>): MultiMap<Int, Mark> {
        val offsets = MultiMap.createSmart<Int, Mark>()
        for ((ktElement, resolveModesForElement) in resolveModesMarkup) {
            val start = ktElement.startOffset
            val end = ktElement.endOffset
            if (resolveModesForElement.isEmpty())
                offsets.putValue(start, Mark.NotAnalyzed())
            else
                offsets.putValue(start, Mark.Analyzed(resolveModesForElement))
            offsets.putValue(end, Mark.End())
        }
        return offsets
    }

    private fun composeTextWithMarkup(offsets: MultiMap<Int, Mark>): String {
        val builder = StringBuilder()
        var offsetBefore = 0
        var counter = 0
        val endIndices = ArrayDeque<Int>()
        for ((offset, marks) in offsets.entrySet().sortedBy { it.key }) {
            builder.append(file.text.substring(offsetBefore, offset))
            for (mark in marks) {
                if (mark !is Mark.End) {
                    counter += 1
                    endIndices.add(counter)
                }

                when (mark) {
                    is Mark.Analyzed -> {
                        val modes = mark.modes!!
                        val modesRepresentation =
                            if (modes.size == RESOLVE_MODES.size) "All"
                            else modes.joinToString { it.toString().toLowerCase().capitalize() }
                        builder.append("/*${modesRepresentation} ($counter)*/")
                    }
                    is Mark.NotAnalyzed -> {
                        builder.append("/*None ($counter)*/")
                    }
                    is Mark.End -> {
                        builder.append("/*(${endIndices.pop()})*/")
                    }
                    else -> error("Sealed")
                }
            }
            offsetBefore = offset
        }
        builder.append(file.text.substring(offsetBefore, file.textLength))

        return builder.toString()
    }

    private sealed class Mark(val modes: List<BodyResolveMode>? = null) {
        class NotAnalyzed : Mark()
        class Analyzed(modes: List<BodyResolveMode>) : Mark(modes) {
            init {
                require(modes.isNotEmpty()) { "'Analyzed' mark should not be created without modes" }
            }
        }

        class End : Mark()
    }

    private companion object {
        val RESOLVE_MODES: List<BodyResolveMode> = listOf(
            BodyResolveMode.PARTIAL,
            BodyResolveMode.FULL, // Note that due to caching it's important to do the full resolve as the last one
        )
    }
}

/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.resolve

import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.util.getDataFlowAwareTypes
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.configureCompilerOptions
import org.jetbrains.kotlin.idea.test.rollbackCompilerOptions
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.types.KotlinType
import org.junit.Assert
import java.io.File
import java.util.*

abstract class AbstractPartialBodyResolveTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    fun doTest(unused: String) {
        val testPath = testPath()
        val dumpNormal = dump(testPath, BodyResolveMode.PARTIAL)

        val testPathNoExt = FileUtil.getNameWithoutExtension(testPath)
        KotlinTestUtils.assertEqualsToFile(File("$testPathNoExt.dump"), dumpNormal)

        val dumpForCompletion = dump(testPath, BodyResolveMode.PARTIAL_FOR_COMPLETION)
        val completionDump = File("$testPathNoExt.completion")
        if (dumpForCompletion != dumpNormal) {
            KotlinTestUtils.assertEqualsToFile(completionDump, dumpForCompletion)
        } else {
            Assert.assertFalse(completionDump.exists())
        }
    }

    private fun dump(testPath: String, resolveMode: BodyResolveMode): String {
        myFixture.configureByText(KotlinFileType.INSTANCE, File(testPath).readText())
        val configured = configureCompilerOptions(myFixture.file.text, project, module)

        try {
            val file = myFixture.file as KtFile
            val editor = myFixture.editor
            val selectionModel = editor.selectionModel
            val expression = if (selectionModel.hasSelection()) {
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
                element.getNonStrictParentOfType<KtSimpleNameExpression>() ?: error("No KtSimpleNameExpression at caret")
            }

            val resolutionFacade = file.getResolutionFacade()

            // optimized resolve
            val (target1, type1, processedStatements1) = doResolve(expression, resolutionFacade.analyze(expression, resolveMode))

            // full body resolve
            val (target2, type2, processedStatements2) = doResolve(expression, resolutionFacade.analyze(expression))

            val set = HashSet(processedStatements2)
            assert(set.containsAll(processedStatements1))
            set.removeAll(processedStatements1)

            val builder = StringBuilder()

            if (expression is KtReferenceExpression) {
                builder.append("Resolve target: ${target2.presentation(type2)}\n")
            } else {
                builder.append("Expression type:${type2.presentation()}\n")
            }
            builder.append("----------------------------------------------\n")

            val skippedStatements = set
                .filter { !it.parents.any { it in set } } // do not include skipped statements which are inside other skipped statement
                .sortedBy { it.textOffset }

            myFixture.project.executeWriteCommand("") {
                for (statement in skippedStatements) {
                    statement.replace(KtPsiFactory(project).createComment("/* STATEMENT DELETED: ${statement.compactPresentation()} */"))
                }
            }

            val fileText = file.text
            if (selectionModel.hasSelection()) {
                val start = selectionModel.selectionStart
                val end = selectionModel.selectionEnd
                builder.append(fileText.substring(0, start))
                builder.append("<selection>")
                builder.append(fileText.substring(start, end))
                builder.append("<selection>")
                builder.append(fileText.substring(end))
            } else {
                val newCaretOffset = editor.caretModel.offset
                builder.append(fileText.substring(0, newCaretOffset))
                builder.append("<caret>")
                builder.append(fileText.substring(newCaretOffset))
            }

            Assert.assertEquals(target2.presentation(null), target1.presentation(null))
            Assert.assertEquals(type2.presentation(), type1.presentation())

            return builder.toString()
        } finally {
            if (configured) {
                rollbackCompilerOptions(project, module)
            }
        }
    }

    private data class ResolveData(
        val target: DeclarationDescriptor?,
        val type: KotlinType?,
        val processedStatements: Collection<KtExpression>
    )

    private fun doResolve(expression: KtExpression, bindingContext: BindingContext): ResolveData {
        val target = if (expression is KtReferenceExpression) bindingContext[BindingContext.REFERENCE_TARGET, expression] else null

        val processedStatements = bindingContext.getSliceContents(BindingContext.PROCESSED)
            .filter { it.value }
            .map { it.key }
            .filter { it.parent is KtBlockExpression }

        val receiver = (expression as? KtSimpleNameExpression)?.getReceiverExpression()
        val expressionWithType = if (receiver != null) {
            expression.parent as? KtExpression ?: expression
        } else {
            expression
        }
        val type = bindingContext.getType(expressionWithType)

        return ResolveData(target, type, processedStatements)
    }

    private fun DeclarationDescriptor?.presentation(type: KotlinType?): String {
        if (this == null) return "null"

        val s = DescriptorRenderer.COMPACT.render(this)

        val renderType = this is VariableDescriptor && type != this.returnType
        if (!renderType) return s
        return "$s smart-cast to ${type.presentation()}"
    }

    private fun KotlinType?.presentation() = if (this != null) DescriptorRenderer.COMPACT.renderType(this) else "unknown type"

    private fun KtExpression.compactPresentation(): String {
        val text = text
        val builder = StringBuilder()
        var dropSpace = false
        for (c in text) {
            when (c) {
                ' ', '\n', '\r' -> {
                    if (!dropSpace) builder.append(' ')
                    dropSpace = true
                }

                else -> {
                    builder.append(c)
                    dropSpace = false
                }
            }
        }
        return builder.toString()
    }
}

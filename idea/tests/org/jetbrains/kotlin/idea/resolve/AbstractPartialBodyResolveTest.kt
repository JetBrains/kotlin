/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.resolve

import org.jetbrains.kotlin.idea.test.JetLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.idea.test.JetWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import java.util.HashSet
import org.jetbrains.kotlin.test.JetTestUtils
import java.io.File
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.junit.Assert
import org.jetbrains.kotlin.types.KtType
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.psi.psiUtil.parents
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import org.jetbrains.kotlin.psi.KtPsiFactory
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.idea.KotlinFileType
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand

public abstract class AbstractPartialBodyResolveTest : JetLightCodeInsightFixtureTestCase() {
    override fun getTestDataPath() = JetTestUtils.getHomeDirectory()
    override fun getProjectDescriptor() = JetWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    public fun doTest(testPath: String) {
        val dumpNormal = dump(testPath, BodyResolveMode.PARTIAL)

        val testPathNoExt = FileUtil.getNameWithoutExtension(testPath)
        JetTestUtils.assertEqualsToFile(File(testPathNoExt + ".dump"), dumpNormal)

        val dumpForCompletion = dump(testPath, BodyResolveMode.PARTIAL_FOR_COMPLETION)
        val completionDump = File(testPathNoExt + ".completion")
        if (dumpForCompletion != dumpNormal) {
            JetTestUtils.assertEqualsToFile(completionDump, dumpForCompletion)
        }
        else {
            Assert.assertFalse(completionDump.exists())
        }
    }

    private fun dump(testPath: String, resolveMode: BodyResolveMode): String {
        myFixture.configureByText(KotlinFileType.INSTANCE, File(testPath).readText())

        val file = myFixture.getFile() as KtFile
        val editor = myFixture.getEditor()
        val selectionModel = editor.getSelectionModel()
        val expression = if (selectionModel.hasSelection()) {
            PsiTreeUtil.findElementOfClassAtRange(file, selectionModel.getSelectionStart(), selectionModel.getSelectionEnd(), javaClass<KtExpression>())
                ?: error("No JetExpression at selection range")
        }
        else {
            val offset = editor.getCaretModel().getOffset()
            val element = file.findElementAt(offset)!!
            element.getNonStrictParentOfType<KtSimpleNameExpression>() ?: error("No JetSimpleNameExpression at caret")
        }

        val resolutionFacade = file.getResolutionFacade()

        // optimized resolve
        val (target1, type1, processedStatements1) = doResolve(expression, resolutionFacade.analyze(expression, resolveMode))

        // full body resolve
        val (target2, type2, processedStatements2) = doResolve(expression, resolutionFacade.analyze(expression))

        val set = HashSet(processedStatements2)
        assert (set.containsAll(processedStatements1))
        set.removeAll(processedStatements1)

        val builder = StringBuilder()

        if (expression is KtReferenceExpression) {
            builder.append("Resolve target: ${target2.presentation(type2)}\n")
        }
        else {
            builder.append("Expression type:${type2.presentation()}\n")
        }
        builder.append("----------------------------------------------\n")

        val skippedStatements = set
                .filter { !it.parents.any { it in set } } // do not include skipped statements which are inside other skipped statement
                .sortedBy { it.getTextOffset() }

        myFixture.getProject().executeWriteCommand("") {
            for (statement in skippedStatements) {
                statement.replace(KtPsiFactory(getProject()).createComment("/* STATEMENT DELETED: ${statement.compactPresentation()} */"))
            }
        }

        val fileText = file.getText()
        if (selectionModel.hasSelection()) {
            val start = selectionModel.getSelectionStart()
            val end = selectionModel.getSelectionEnd()
            builder.append(fileText.substring(0, start))
            builder.append("<selection>")
            builder.append(fileText.substring(start, end))
            builder.append("<selection>")
            builder.append(fileText.substring(end))
        }
        else {
            val newCaretOffset = editor.getCaretModel().getOffset()
            builder.append(fileText.substring(0, newCaretOffset))
            builder.append("<caret>")
            builder.append(fileText.substring(newCaretOffset))
        }

        Assert.assertEquals(target2.presentation(null), target1.presentation(null))
        Assert.assertEquals(type2.presentation(), type1.presentation())

        return builder.toString()
    }

    private data class ResolveData(
            val target: DeclarationDescriptor?,
            val type: KtType?,
            val processedStatements: Collection<KtExpression>
    )

    private fun doResolve(expression: KtExpression, bindingContext: BindingContext): ResolveData {
        val target = if (expression is KtReferenceExpression) bindingContext[BindingContext.REFERENCE_TARGET, expression] else null

        val processedStatements = bindingContext.getSliceContents(BindingContext.PROCESSED)
                .filter { it.value }
                .map { it.key }
                .filter { it.getParent() is KtBlockExpression }

        val receiver = (expression as? KtSimpleNameExpression)?.getReceiverExpression()
        val expressionWithType = if (receiver != null) {
            expression.getParent() as? KtExpression ?: expression
        }
        else {
            expression
        }
        val type = bindingContext.getType(expressionWithType)

        return ResolveData(target, type, processedStatements)
    }

    private fun DeclarationDescriptor?.presentation(type: KtType?): String {
        if (this == null) return "null"

        val s = DescriptorRenderer.COMPACT.render(this)

        val renderType = this is VariableDescriptor && type != this.getReturnType()
        if (!renderType) return s
        return "$s smart-cast to ${type.presentation()}"
    }

    private fun KtType?.presentation()
            = if (this != null) DescriptorRenderer.COMPACT.renderType(this) else "unknown type"

    private fun KtExpression.compactPresentation(): String {
        val text = getText()
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

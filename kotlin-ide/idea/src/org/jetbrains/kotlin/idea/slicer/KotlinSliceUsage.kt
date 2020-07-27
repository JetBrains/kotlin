/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.slicer

import com.intellij.ide.SelectInEditorManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.util.ProperTextRange
import com.intellij.openapi.util.Segment
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.slicer.SliceAnalysisParams
import com.intellij.slicer.SliceUsage
import com.intellij.usageView.UsageInfo
import com.intellij.util.Processor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

open class KotlinSliceUsage : SliceUsage {

    val mode: KotlinSliceAnalysisMode
    val forcedExpressionMode: Boolean

    private var usageInfo: AdaptedUsageInfo? = null

    constructor(
        element: PsiElement,
        parent: SliceUsage,
        mode: KotlinSliceAnalysisMode,
        forcedExpressionMode: Boolean,
    ) : super(element, parent) {
        this.mode = mode
        this.forcedExpressionMode = forcedExpressionMode
        initializeUsageInfo()
    }

    constructor(element: PsiElement, params: SliceAnalysisParams) : super(element, params) {
        this.mode = KotlinSliceAnalysisMode.Default
        this.forcedExpressionMode = false
        initializeUsageInfo()
    }

    //TODO: it's all hacks due to UsageInfo stored in the base class - fix it in IDEA
    private fun initializeUsageInfo() {
        usageInfo = getUsageInfo().element?.let { AdaptedUsageInfo(it, mode) }
    }

    override fun getUsageInfo(): UsageInfo {
        return usageInfo ?: super.getUsageInfo()
    }

    override fun getMergedInfos(): Array<UsageInfo> {
        return arrayOf(getUsageInfo())
    }

    override fun openTextEditor(focus: Boolean): Editor? {
        val project = getUsageInfo().project
        val descriptor = OpenFileDescriptor(project, file, getUsageInfo().navigationOffset)
        return FileEditorManager.getInstance(project).openTextEditor(descriptor, focus)
    }

    override fun highlightInEditor() {
        if (!isValid) return

        val usageInfo = getUsageInfo()
        val range = usageInfo.navigationRange ?: return
        SelectInEditorManager.getInstance(getUsageInfo().project).selectInEditor(file, range.startOffset, range.endOffset, false, false)

        if (usageInfo.navigationOffset != range.startOffset) {
            openTextEditor(false) // to position the caret at the identifier
        }
    }

    override fun copy(): KotlinSliceUsage {
        val element = getUsageInfo().element ?: error("No more valid usageInfo.element")
        return if (parent == null)
            KotlinSliceUsage(element, params)
        else
            KotlinSliceUsage(element, parent, mode, forcedExpressionMode)
    }

    override fun canBeLeaf() = element != null && mode == KotlinSliceAnalysisMode.Default

    public override fun processUsagesFlownDownTo(element: PsiElement, uniqueProcessor: Processor<in SliceUsage>) {
        val ktElement = element as? KtElement ?: return
        val behaviour = mode.currentBehaviour
        if (behaviour != null) {
            behaviour.processUsages(ktElement, this, uniqueProcessor)
        } else {
            InflowSlicer(ktElement, uniqueProcessor, this).processChildren(forcedExpressionMode)
        }
    }

    public override fun processUsagesFlownFromThe(element: PsiElement, uniqueProcessor: Processor<in SliceUsage>) {
        val ktElement = element as? KtElement ?: return
        val behaviour = mode.currentBehaviour
        if (behaviour != null) {
            behaviour.processUsages(ktElement, this, uniqueProcessor)
        } else {
            OutflowSlicer(ktElement, uniqueProcessor, this).processChildren(forcedExpressionMode)
        }
    }

    @Suppress("EqualsOrHashCode")
    private class AdaptedUsageInfo(element: PsiElement, private val mode: KotlinSliceAnalysisMode) : UsageInfo(element) {
        override fun equals(other: Any?): Boolean {
            return other is AdaptedUsageInfo && super.equals(other) && mode == other.mode
        }

        override fun getNavigationRange(): Segment? {
            val element = element ?: return null
            return when (element) {
                is KtParameter -> {
                    val nameRange = element.nameIdentifier?.textRange ?: return super.getNavigationRange()
                    val start = element.valOrVarKeyword?.startOffset ?: nameRange.startOffset
                    val end = element.typeReference?.endOffset ?: nameRange.endOffset
                    TextRange(start, end)
                }

                is KtVariableDeclaration -> {
                    val nameRange = element.nameIdentifier?.textRange ?: return super.getNavigationRange()
                    val start = element.valOrVarKeyword?.startOffset ?: nameRange.startOffset
                    val end = element.typeReference?.endOffset ?: nameRange.endOffset
                    TextRange(start, end)
                }

                is KtNamedFunction -> {
                    val funKeyword = element.funKeyword
                    val parameterList = element.valueParameterList
                    val typeReference = element.typeReference
                    if (funKeyword != null && parameterList != null)
                        TextRange(funKeyword.startOffset, typeReference?.endOffset ?: parameterList.endOffset)
                    else
                        null
                }

                is KtPrimaryConstructor -> {
                    element.containingClassOrObject?.nameIdentifier
                        ?.let { TextRange(it.startOffset, element.endOffset) }
                }

                else -> null
            } ?: TextRange(element.textOffset, element.endOffset)
        }

        override fun getRangeInElement(): ProperTextRange? {
            val elementRange = element?.textRange ?: return null
            return navigationRange
                ?.takeIf { it in elementRange }
                ?.let { ProperTextRange(it.startOffset, it.endOffset).shiftRight(-elementRange.startOffset) }
                ?: super.getRangeInElement()
        }

        override fun getNavigationOffset(): Int {
            return element?.textOffset ?: -1
        }
    }
}


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

import com.intellij.psi.PsiElement
import com.intellij.slicer.SliceAnalysisParams
import com.intellij.slicer.SliceUsage
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.idea.findUsages.handlers.SliceUsageProcessor
import org.jetbrains.kotlin.psi.KtElement

open class KotlinSliceUsage : SliceUsage {

    val mode: KotlinSliceAnalysisMode
    val forcedExpressionMode: Boolean

    private var usageInfo: UsageInfo? = null

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

    private fun initializeUsageInfo() {
        val originalInfo = getUsageInfo()
        if (mode != KotlinSliceAnalysisMode.Default) {
            val element = originalInfo.element
            if (element != null) {
                usageInfo = UsageInfoWrapper(element, mode)
            } else {
                usageInfo = null
            }
        } else {
            usageInfo = originalInfo
        }
    }

    // we have to replace UsageInfo with another one whose equality takes into account mode
    override fun getUsageInfo(): UsageInfo {
        return usageInfo ?: super.getUsageInfo()
    }

    override fun copy(): KotlinSliceUsage {
        val element = getUsageInfo().element!!
        return if (parent == null)
            KotlinSliceUsage(element, params)
        else
            KotlinSliceUsage(element, parent, mode, forcedExpressionMode)
    }

    override fun canBeLeaf() = element != null && mode == KotlinSliceAnalysisMode.Default

    public override fun processUsagesFlownDownTo(element: PsiElement, uniqueProcessor: SliceUsageProcessor) {
        val ktElement = element as? KtElement ?: return
        val behaviour = mode.currentBehaviour
        if (behaviour != null) {
            behaviour.processUsages(ktElement, this, uniqueProcessor)
        } else {
            InflowSlicer(ktElement, uniqueProcessor, this).processChildren(forcedExpressionMode)
        }
    }

    public override fun processUsagesFlownFromThe(element: PsiElement, uniqueProcessor: SliceUsageProcessor) {
        val ktElement = element as? KtElement ?: return
        val behaviour = mode.currentBehaviour
        if (behaviour != null) {
            behaviour.processUsages(ktElement, this, uniqueProcessor)
        } else {
            OutflowSlicer(ktElement, uniqueProcessor, this).processChildren(forcedExpressionMode)
        }
    }

    @Suppress("EqualsOrHashCode")
    private class UsageInfoWrapper(element: PsiElement, private val mode: KotlinSliceAnalysisMode) : UsageInfo(element) {
        override fun equals(other: Any?): Boolean {
            return other is UsageInfoWrapper && super.equals(other) && mode == other.mode
        }
    }
}


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
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.findUsages.handlers.SliceUsageProcessor
import org.jetbrains.kotlin.psi.KtElement

open class KotlinSliceUsage : SliceUsage {
    interface SpecialBehaviour {
        val originalBehaviour: SpecialBehaviour?
        fun processUsages(element: KtElement, parent: KotlinSliceUsage, uniqueProcessor: SliceUsageProcessor)

        val slicePresentationPrefix: String
        val testPresentationPrefix: String

        override fun equals(other: Any?): Boolean
        override fun hashCode(): Int
    }

    val behaviour: SpecialBehaviour?
    val forcedExpressionMode: Boolean

    constructor(
        element: PsiElement,
        parent: SliceUsage,
        behaviour: SpecialBehaviour?,
        forcedExpressionMode: Boolean,
    ) : super(element, parent) {
        this.behaviour = behaviour
        this.forcedExpressionMode = forcedExpressionMode
    }

    constructor(element: PsiElement, params: SliceAnalysisParams) : super(element, params) {
        this.behaviour = null
        this.forcedExpressionMode = false
    }

    override fun copy(): KotlinSliceUsage {
        val element = usageInfo.element!!
        return if (parent == null)
            KotlinSliceUsage(element, params)
        else
            KotlinSliceUsage(element, parent, behaviour, forcedExpressionMode)
    }

    override fun getUsageInfo(): UsageInfo {
        val originalInfo = super.getUsageInfo()
        if (behaviour != null) {
            val element = originalInfo.element ?: return originalInfo
            // Do not let IDEA consider usages of the same anonymous function as duplicates when their levels differ
            return UsageInfoWrapper(element, behaviour)
        }
        return originalInfo
    }

    override fun canBeLeaf() = element != null && behaviour == null

    public override fun processUsagesFlownDownTo(element: PsiElement, uniqueProcessor: SliceUsageProcessor) {
        val ktElement = element as? KtElement ?: return
        if (behaviour != null) {
            behaviour.processUsages(ktElement, this, uniqueProcessor)
        } else {
            InflowSlicer(ktElement, uniqueProcessor, this).processChildren(forcedExpressionMode)
        }
    }

    public override fun processUsagesFlownFromThe(element: PsiElement, uniqueProcessor: SliceUsageProcessor) {
        val ktElement = element as? KtElement ?: return
        if (behaviour != null) {
            behaviour.processUsages(ktElement, this, uniqueProcessor)
        } else {
            OutflowSlicer(ktElement, uniqueProcessor, this).processChildren(forcedExpressionMode)
        }
    }

    @Suppress("EqualsOrHashCode")
    private class UsageInfoWrapper(element: PsiElement, private val behaviour: SpecialBehaviour) : UsageInfo(element) {
        override fun equals(other: Any?): Boolean {
            return other is UsageInfoWrapper && super.equals(other) && behaviour == other.behaviour
        }
    }
}

data class LambdaResultOutflowBehaviour(
    override val originalBehaviour: KotlinSliceUsage.SpecialBehaviour?
) : KotlinSliceUsage.SpecialBehaviour {

    override fun processUsages(element: KtElement, parent: KotlinSliceUsage, uniqueProcessor: SliceUsageProcessor) {
        OutflowSlicer(element, uniqueProcessor, parent).processChildren(parent.forcedExpressionMode)
    }

    override val slicePresentationPrefix: String
        get() = KotlinBundle.message("slicer.text.tracking.enclosing.lambda")

    override val testPresentationPrefix: String
        get() = "[LAMBDA] "
}

data class LambdaResultInflowBehaviour(
    override val originalBehaviour: KotlinSliceUsage.SpecialBehaviour?
) : KotlinSliceUsage.SpecialBehaviour {

    override fun processUsages(element: KtElement, parent: KotlinSliceUsage, uniqueProcessor: SliceUsageProcessor) {
        InflowSlicer(element, uniqueProcessor, parent).processChildren(parent.forcedExpressionMode)
    }

    override val slicePresentationPrefix: String
        get() = KotlinBundle.message("slicer.text.tracking.enclosing.lambda")

    override val testPresentationPrefix: String
        get() = "[LAMBDA] "
}

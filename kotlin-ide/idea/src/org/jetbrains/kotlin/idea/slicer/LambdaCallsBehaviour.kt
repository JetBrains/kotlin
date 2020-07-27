/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.slicer

import com.intellij.slicer.SliceUsage
import com.intellij.util.Processor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.psi.KtElement

data class LambdaCallsBehaviour(private val sliceProducer: SliceProducer) : KotlinSliceAnalysisMode.Behaviour {
    override fun processUsages(element: KtElement, parent: KotlinSliceUsage, uniqueProcessor: Processor<in SliceUsage>) {
        val processor = object : Processor<SliceUsage> {
            override fun process(sliceUsage: SliceUsage): Boolean {
                if (sliceUsage is KotlinSliceUsage && sliceUsage.mode.currentBehaviour === this@LambdaCallsBehaviour) {
                    val sliceElement = sliceUsage.element ?: return true
                    val resolvedCall = (sliceElement as? KtElement)?.resolveToCall()
                    if (resolvedCall != null && resolvedCall.resultingDescriptor.isImplicitInvokeFunction()) {
                        val originalMode = sliceUsage.mode.dropBehaviour()
                        val newSliceUsage = KotlinSliceUsage(resolvedCall.call.callElement, parent, originalMode, true)
                        return sliceProducer.produceAndProcess(newSliceUsage, originalMode, parent, uniqueProcessor)
                    }
                }
                return uniqueProcessor.process(sliceUsage)
            }
        }
        OutflowSlicer(element, processor, parent).processChildren(parent.forcedExpressionMode)
    }

    override val slicePresentationPrefix: String
        get() = KotlinBundle.message("slicer.text.tracking.lambda.calls")

    override val testPresentationPrefix: String
        get() = buildString {
            append("[LAMBDA CALLS")
            sliceProducer.testPresentation?.let {
                append(" ")
                append(it)
            }
            append("] ")
        }
}

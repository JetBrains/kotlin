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

package org.jetbrains.kotlin.idea.refactoring.changeSignature

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.changeSignature.ChangeSignatureProcessorBase
import com.intellij.refactoring.changeSignature.ChangeSignatureUsageProcessor
import com.intellij.refactoring.changeSignature.JavaChangeSignatureUsageProcessor
import com.intellij.refactoring.rename.RenameUtil
import com.intellij.refactoring.rename.UnresolvableCollisionUsageInfo
import com.intellij.usageView.UsageInfo
import com.intellij.usageView.UsageViewDescriptor
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.codeInsight.shorten.performDelayedRefactoringRequests
import org.jetbrains.kotlin.idea.core.canMoveLambdaOutsideParentheses
import org.jetbrains.kotlin.idea.core.moveFunctionLiteralOutsideParentheses
import org.jetbrains.kotlin.idea.refactoring.broadcastRefactoringExit
import org.jetbrains.kotlin.idea.refactoring.changeSignature.usages.KotlinFunctionCallUsage
import org.jetbrains.kotlin.idea.refactoring.changeSignature.usages.KotlinImplicitReceiverUsage
import org.jetbrains.kotlin.idea.refactoring.changeSignature.usages.KotlinUsageInfo
import org.jetbrains.kotlin.idea.refactoring.changeSignature.usages.KotlinWrapperForJavaUsageInfos
import org.jetbrains.kotlin.psi.KtCallExpression

class KotlinChangeSignatureProcessor(
    project: Project,
    changeInfo: KotlinChangeInfo,
    @NlsContexts.Command private val commandName: String
) : ChangeSignatureProcessorBase(project, KotlinChangeInfoWrapper(changeInfo)) {
    init {
        // we must force collecting references to other parameters now before the signature is changed
        changeInfo.newParameters.forEach { it.defaultValueParameterReferences }
    }

    val ktChangeInfo: KotlinChangeInfo
        get() = changeInfo.delegate!!

    override fun setPrepareSuccessfulSwingThreadCallback(callback: Runnable?) {
        val actualCallback = if (callback != null) {
            Runnable {
                callback.run()
                setPrepareSuccessfulSwingThreadCallback(null)
            }
        } else null
        super.setPrepareSuccessfulSwingThreadCallback(actualCallback)
    }

    override fun createUsageViewDescriptor(usages: Array<UsageInfo>): UsageViewDescriptor {
        val subject = if (ktChangeInfo.kind.isConstructor)
            KotlinBundle.message("text.constructor")
        else
            KotlinBundle.message("text.function")
        return KotlinUsagesViewDescriptor(myChangeInfo.method, RefactoringBundle.message("0.to.change.signature", subject))
    }

    override fun getChangeInfo(): KotlinChangeInfoWrapper = super.getChangeInfo() as KotlinChangeInfoWrapper

    override fun findUsages(): Array<UsageInfo> {
        val allUsages = ArrayList<UsageInfo>()
        val javaUsages = mutableSetOf<UsageInfo>()
        ktChangeInfo.getOrCreateJavaChangeInfos()?.let { javaChangeInfos ->
            val javaProcessor = JavaChangeSignatureUsageProcessor()
            javaChangeInfos.mapTo(allUsages) { javaChangeInfo ->
                val javaUsagesForKtChange = javaProcessor.findUsages(javaChangeInfo)
                val uniqueJavaUsagesForKtChange = javaUsagesForKtChange.filterNot<UsageInfo> { javaUsages.contains(it) }
                javaUsages.addAll(javaUsagesForKtChange)
                KotlinWrapperForJavaUsageInfos(javaChangeInfo, uniqueJavaUsagesForKtChange.toTypedArray(), changeInfo.method)
            }
        }

        super.findUsages().filterTo(allUsages) { it is KotlinUsageInfo<*> || it is UnresolvableCollisionUsageInfo }
        return allUsages.toTypedArray()
    }

    override fun preprocessUsages(refUsages: Ref<Array<UsageInfo>>): Boolean {
        val usageProcessors = ChangeSignatureUsageProcessor.EP_NAME.extensions

        if (!usageProcessors.all { it.setupDefaultValues(myChangeInfo, refUsages, myProject) }) return false

        val conflictDescriptions = MultiMap<PsiElement, String>()
        usageProcessors.forEach { conflictDescriptions.putAllValues(it.findConflicts(myChangeInfo, refUsages)) }

        val usages = refUsages.get()
        val usagesSet = usages.toHashSet()

        RenameUtil.addConflictDescriptions(usages, conflictDescriptions)
        RenameUtil.removeConflictUsages(usagesSet)

        val usageArray = usagesSet.sortedWith(Comparator { u1, u2 ->
            if (u1 is KotlinImplicitReceiverUsage && u2 is KotlinFunctionCallUsage) return@Comparator -1
            if (u2 is KotlinImplicitReceiverUsage && u1 is KotlinFunctionCallUsage) return@Comparator 1
            val element1 = u1.element
            val element2 = u2.element
            val rank1 = element1?.textOffset ?: -1
            val rank2 = element2?.textOffset ?: -1
            rank2 - rank1 // Reverse order
        }).toTypedArray()

        refUsages.set(usageArray)
        return showConflicts(conflictDescriptions, usageArray)
    }

    override fun isPreviewUsages(usages: Array<out UsageInfo>): Boolean = isPreviewUsages

    override fun getCommandName() = commandName

    override fun performRefactoring(usages: Array<out UsageInfo>) {
        try {
            super.performRefactoring(usages)
            usages.forEach {
                val callExpression = it.element as? KtCallExpression ?: return@forEach
                if (callExpression.canMoveLambdaOutsideParentheses()) {
                    callExpression.moveFunctionLiteralOutsideParentheses()
                }
            }
            performDelayedRefactoringRequests(myProject)
        } finally {
            changeInfo.invalidate()
        }
    }

    override fun doRun() {
        try {
            super.doRun()
        } finally {
            broadcastRefactoringExit(myProject, refactoringId!!)
        }
    }
}

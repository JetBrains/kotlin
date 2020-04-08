/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinChangeSignatureConfiguration
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinMethodDescriptor
import org.jetbrains.kotlin.idea.refactoring.changeSignature.modify
import org.jetbrains.kotlin.idea.refactoring.changeSignature.runChangeSignature
import org.jetbrains.kotlin.idea.refactoring.resolveToExpectedDescriptorIfPossible
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class ConvertParameterToReceiverIntention : SelfTargetingIntention<KtParameter>(
    KtParameter::class.java,
    KotlinBundle.lazyMessage("convert.parameter.to.receiver")
) {
    override fun isApplicableTo(element: KtParameter, caretOffset: Int): Boolean {
        val identifier = element.nameIdentifier ?: return false
        if (!identifier.textRange.containsOffset(caretOffset)) return false
        if (element.isVarArg) return false
        val function = element.getStrictParentOfType<KtNamedFunction>() ?: return false
        return function.valueParameterList == element.parent && function.receiverTypeReference == null
    }

    private fun configureChangeSignature(parameterIndex: Int): KotlinChangeSignatureConfiguration =
        object : KotlinChangeSignatureConfiguration {
            override fun configure(originalDescriptor: KotlinMethodDescriptor): KotlinMethodDescriptor {
                return originalDescriptor.modify { it.receiver = originalDescriptor.parameters[parameterIndex] }
            }

            override fun performSilently(affectedFunctions: Collection<PsiElement>) = true
        }

    override fun startInWriteAction() = false

    override fun applyTo(element: KtParameter, editor: Editor?) {
        val function = element.getStrictParentOfType<KtNamedFunction>() ?: return
        val parameterIndex = function.valueParameters.indexOf(element)
        val descriptor = function.resolveToExpectedDescriptorIfPossible() as? FunctionDescriptor ?: return
        runChangeSignature(element.project, descriptor, configureChangeSignature(parameterIndex), element, text)
    }
}

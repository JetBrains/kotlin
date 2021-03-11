/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createVariable

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithAllCompilerChecks
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.guessTypes
import org.jetbrains.kotlin.idea.refactoring.canRefactor
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinParameterInfo
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinTypeInfo
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinValVar
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns

object CreateParameterByNamedArgumentActionFactory : CreateParameterFromUsageFactory<KtValueArgument>() {
    override fun getElementOfInterest(diagnostic: Diagnostic): KtValueArgument? {
        val argument = QuickFixUtil.getParentElementOfType(diagnostic, KtValueArgument::class.java) ?: return null
        return if (argument.isNamed()) argument else null
    }

    override fun extractFixData(element: KtValueArgument, diagnostic: Diagnostic): CreateParameterData<KtValueArgument>? {
        val result = (diagnostic.psiFile as? KtFile)?.analyzeWithAllCompilerChecks() ?: return null
        val context = result.bindingContext

        val name = element.getArgumentName()?.text ?: return null
        val argumentExpression = element.getArgumentExpression()

        val callElement = element.getStrictParentOfType<KtCallElement>() ?: return null
        val functionDescriptor = callElement.getResolvedCall(context)?.resultingDescriptor as? FunctionDescriptor ?: return null
        val callable = DescriptorToSourceUtilsIde.getAnyDeclaration(callElement.project, functionDescriptor) ?: return null
        if (!((callable is KtFunction || callable is KtClass) && callable.canRefactor())) return null

        val anyType = functionDescriptor.builtIns.anyType
        val paramType = argumentExpression?.guessTypes(context, result.moduleDescriptor)?.let {
            when (it.size) {
                0 -> anyType
                1 -> it.first()
                else -> return null
            }
        } ?: anyType
        if (paramType.hasTypeParametersToAdd(functionDescriptor, context)) return null

        val needVal = callable is KtPrimaryConstructor && ((callable.getContainingClassOrObject() as? KtClass)?.isData() ?: false)

        val parameterInfo = KotlinParameterInfo(
            callableDescriptor = functionDescriptor,
            name = name,
            originalTypeInfo = KotlinTypeInfo(false, paramType),
            defaultValueForCall = argumentExpression,
            valOrVar = if (needVal) KotlinValVar.Val else KotlinValVar.None
        )

        return CreateParameterData(parameterInfo, element)
    }
}

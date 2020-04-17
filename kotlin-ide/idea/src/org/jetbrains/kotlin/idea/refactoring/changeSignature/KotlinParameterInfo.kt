/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.changeSignature

import com.intellij.psi.PsiReference
import com.intellij.refactoring.changeSignature.ParameterInfo
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.compareDescriptors
import org.jetbrains.kotlin.idea.core.copied
import org.jetbrains.kotlin.idea.core.setDefaultValue
import org.jetbrains.kotlin.idea.refactoring.changeSignature.usages.KotlinCallableDefinitionUsage
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.addRemoveModifier.setModifierList
import org.jetbrains.kotlin.psi.psiUtil.quoteIfNeeded
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.types.isError
import java.util.*

class KotlinParameterInfo @JvmOverloads constructor(
    val callableDescriptor: CallableDescriptor,
    val originalIndex: Int = -1,
    private var name: String,
    val originalTypeInfo: KotlinTypeInfo = KotlinTypeInfo(false),
    var defaultValueForParameter: KtExpression? = null,
    var defaultValueForCall: KtExpression? = null,
    var valOrVar: KotlinValVar = KotlinValVar.None,
    val modifierList: KtModifierList? = null
) : ParameterInfo {
    var currentTypeInfo: KotlinTypeInfo = originalTypeInfo

    val defaultValueParameterReferences: Map<PsiReference, DeclarationDescriptor> by lazy {
        collectDefaultValueParameterReferences(defaultValueForCall)
    }

    private fun collectDefaultValueParameterReferences(defaultValueForCall: KtExpression?): Map<PsiReference, DeclarationDescriptor> {
        val file = defaultValueForCall?.containingFile as? KtFile ?: return emptyMap()
        if (!file.isPhysical && file.analysisContext == null) return emptyMap()

        val project = file.project
        val map = LinkedHashMap<PsiReference, DeclarationDescriptor>()

        defaultValueForCall.accept(
            object : KtTreeVisitorVoid() {
                private fun compareDescriptors(currentDescriptor: DeclarationDescriptor?, originalDescriptor: DeclarationDescriptor?): Boolean {
                    return compareDescriptors(project, currentDescriptor, originalDescriptor)
                }

                private fun takeIfParameterOfSelf(parameter: DeclarationDescriptor?): ValueParameterDescriptor? {
                    return (parameter as? ValueParameterDescriptor)
                        ?.takeIf { compareDescriptors(it.containingDeclaration, callableDescriptor) }
                }

                private fun takeIfReceiverOfSelf(receiverDescriptor: DeclarationDescriptor?): DeclarationDescriptor? {
                    return receiverDescriptor?.takeIf {
                        compareDescriptors(it, callableDescriptor.extensionReceiverParameter?.containingDeclaration)
                                || compareDescriptors(it, callableDescriptor.dispatchReceiverParameter?.containingDeclaration)
                    }
                }

                private fun takeIfReceiverOfSelf(receiver: ImplicitReceiver?): DeclarationDescriptor? {
                    return takeIfReceiverOfSelf(receiver?.declarationDescriptor)
                }

                private fun getRelevantDescriptor(expression: KtSimpleNameExpression, ref: KtReference): DeclarationDescriptor? {
                    val context = expression.analyze(BodyResolveMode.PARTIAL)

                    val descriptor = ref.resolveToDescriptors(context).singleOrNull()
                    if (descriptor is ValueParameterDescriptor) {
                        return takeIfParameterOfSelf(descriptor)
                    }

                    if (descriptor is PropertyDescriptor && callableDescriptor is ConstructorDescriptor) {
                        val parameter = DescriptorToSourceUtils.getSourceFromDescriptor(descriptor)
                        if (parameter is KtParameter) {
                            return takeIfParameterOfSelf(context[BindingContext.VALUE_PARAMETER, parameter])
                        }
                    }

                    val resolvedCall = expression.getResolvedCall(context) ?: return null
                    (resolvedCall.resultingDescriptor as? ReceiverParameterDescriptor)?.let {
                        return if (takeIfReceiverOfSelf(it.containingDeclaration) != null) it else null
                    }

                    takeIfReceiverOfSelf(resolvedCall.extensionReceiver as? ImplicitReceiver)?.let { return it }
                    takeIfReceiverOfSelf(resolvedCall.dispatchReceiver as? ImplicitReceiver)?.let { return it }

                    return null
                }

                override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
                    val ref = expression.mainReference
                    val descriptor = getRelevantDescriptor(expression, ref) ?: return
                    map[ref] = descriptor
                }
            }
        )
        return map
    }

    override fun getOldIndex(): Int = originalIndex

    val isNewParameter: Boolean
        get() = originalIndex == -1

    override fun getDefaultValue(): String? = null

    override fun getName(): String = name

    override fun setName(name: String?) {
        this.name = name ?: ""
    }

    override fun getTypeText(): String = currentTypeInfo.render()

    val isTypeChanged: Boolean get() = !currentTypeInfo.isEquivalentTo(originalTypeInfo)

    override fun isUseAnySingleVariable(): Boolean = false

    override fun setUseAnySingleVariable(b: Boolean) {
        throw UnsupportedOperationException()
    }

    fun renderType(parameterIndex: Int, inheritedCallable: KotlinCallableDefinitionUsage<*>): String {
        val defaultRendering = currentTypeInfo.render()
        val typeSubstitutor = inheritedCallable.typeSubstitutor ?: return defaultRendering
        val currentBaseFunction = inheritedCallable.baseFunction.currentCallableDescriptor ?: return defaultRendering
        val parameter = currentBaseFunction.valueParameters[parameterIndex]
        if (parameter.isVararg) return defaultRendering
        val parameterType = parameter.type
        if (parameterType.isError) return defaultRendering
        return parameterType.renderTypeWithSubstitution(typeSubstitutor, defaultRendering, true)
    }

    fun getInheritedName(inheritedCallable: KotlinCallableDefinitionUsage<*>): String {
        if (!inheritedCallable.isInherited) return name

        val baseFunction = inheritedCallable.baseFunction
        val baseFunctionDescriptor = baseFunction.originalCallableDescriptor

        val inheritedFunctionDescriptor = inheritedCallable.originalCallableDescriptor
        val inheritedParameterDescriptors = inheritedFunctionDescriptor.valueParameters
        if (originalIndex < 0
            || originalIndex >= baseFunctionDescriptor.valueParameters.size
            || originalIndex >= inheritedParameterDescriptors.size
        ) return name

        val inheritedParamName = inheritedParameterDescriptors[originalIndex].name.asString()
        val oldParamName = baseFunctionDescriptor.valueParameters[originalIndex].name.asString()

        return when {
            oldParamName == inheritedParamName && inheritedFunctionDescriptor !is AnonymousFunctionDescriptor -> name
            else -> inheritedParamName
        }
    }

    fun requiresExplicitType(inheritedCallable: KotlinCallableDefinitionUsage<*>): Boolean {
        val inheritedFunctionDescriptor = inheritedCallable.originalCallableDescriptor
        if (inheritedFunctionDescriptor !is AnonymousFunctionDescriptor) return true

        if (originalIndex < 0) return !inheritedCallable.hasExpectedType

        val inheritedParameterDescriptor = inheritedFunctionDescriptor.valueParameters[originalIndex]
        val parameter = DescriptorToSourceUtils.descriptorToDeclaration(inheritedParameterDescriptor) as? KtParameter ?: return false
        return parameter.typeReference != null
    }

    private fun getOriginalParameter(inheritedCallable: KotlinCallableDefinitionUsage<*>): KtParameter? {
        return (inheritedCallable.declaration as? KtFunction)?.valueParameters?.getOrNull(originalIndex)
    }

    private fun buildNewParameter(inheritedCallable: KotlinCallableDefinitionUsage<*>, parameterIndex: Int): KtParameter {
        val psiFactory = KtPsiFactory(inheritedCallable.project)

        val buffer = StringBuilder()

        if (modifierList != null) {
            buffer.append(modifierList.text).append(' ')
        }

        if (valOrVar != KotlinValVar.None) {
            buffer.append(valOrVar).append(' ')
        }

        buffer.append(getInheritedName(inheritedCallable).quoteIfNeeded())

        if (requiresExplicitType(inheritedCallable)) {
            buffer.append(": ").append(renderType(parameterIndex, inheritedCallable))
        }

        if (!inheritedCallable.isInherited) {
            defaultValueForParameter?.let { buffer.append(" = ").append(it.text) }
        }

        return psiFactory.createParameter(buffer.toString())
    }

    fun getDeclarationSignature(parameterIndex: Int, inheritedCallable: KotlinCallableDefinitionUsage<*>): KtParameter {
        val originalParameter = getOriginalParameter(inheritedCallable)
            ?: return buildNewParameter(inheritedCallable, parameterIndex)

        val psiFactory = KtPsiFactory(originalParameter)
        val newParameter = originalParameter.copied()

        modifierList?.let { newParameter.setModifierList(it) }

        if (valOrVar != newParameter.valOrVarKeyword.toValVar()) {
            newParameter.setValOrVar(valOrVar)
        }

        val newName = getInheritedName(inheritedCallable)
        if (newParameter.name != newName) {
            newParameter.setName(newName.quoteIfNeeded())
        }

        if (newParameter.typeReference != null || requiresExplicitType(inheritedCallable)) {
            newParameter.typeReference = psiFactory.createType(renderType(parameterIndex, inheritedCallable))
        }

        if (!inheritedCallable.isInherited) {
            defaultValueForParameter?.let { newParameter.setDefaultValue(it) }
        }

        return newParameter
    }
}

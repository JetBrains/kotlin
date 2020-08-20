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

package org.jetbrains.kotlin.idea.search.usagesSearch

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.asJava.LightClassUtil.PropertyAccessorsPsiMethods
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.caches.resolve.resolveToParameterDescriptorIfAny
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DataClassDescriptorResolver
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.findOriginalTopMostOverriddenDescriptors
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import org.jetbrains.kotlin.resolve.source.getPsi
import java.util.*

fun PsiNamedElement.getAccessorNames(readable: Boolean = true, writable: Boolean = true): List<String> {
    fun PropertyAccessorsPsiMethods.toNameList(): List<String> {
        val getter = getter
        val setter = setter

        val result = ArrayList<String>()
        if (readable && getter != null) result.add(getter.name)
        if (writable && setter != null) result.add(setter.name)
        return result
    }

    if (this !is KtDeclaration || KtPsiUtil.isLocal(this)) return Collections.emptyList()

    when (this) {
        is KtProperty ->
            return LightClassUtil.getLightClassPropertyMethods(this).toNameList()
        is KtParameter ->
            if (hasValOrVar()) {
                return LightClassUtil.getLightClassPropertyMethods(this).toNameList()
            }
    }

    return Collections.emptyList()
}

fun PsiNamedElement.getClassNameForCompanionObject(): String? {
    return if (this is KtObjectDeclaration && this.isCompanion()) {
        getNonStrictParentOfType<KtClass>()?.name
    } else {
        null
    }
}

fun KtParameter.dataClassComponentFunction(): FunctionDescriptor? {
    if (!isDataClassProperty()) return null

    val context = this.analyze()
    val paramDescriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, this] as? ValueParameterDescriptor

    val constructor = paramDescriptor?.containingDeclaration as? ConstructorDescriptor ?: return null
    val index = constructor.valueParameters.indexOf(paramDescriptor)
    val correspondingComponentName = DataClassDescriptorResolver.createComponentName(index + 1)

    val dataClass = constructor.containingDeclaration as? ClassDescriptor ?: return null
    dataClass.unsubstitutedMemberScope.getContributedFunctions(correspondingComponentName, NoLookupLocation.FROM_IDE)

    return context[BindingContext.DATA_CLASS_COMPONENT_FUNCTION, paramDescriptor]
}

fun KtParameter.isDataClassProperty(): Boolean {
    if (!hasValOrVar()) return false
    return this.containingClassOrObject?.hasModifier(KtTokens.DATA_KEYWORD) ?: false
}

fun getTopMostOverriddenElementsToHighlight(target: PsiElement): List<PsiElement> {
    val callableDescriptor = (target as? KtCallableDeclaration)?.resolveToDescriptorIfAny() as? CallableDescriptor
    val descriptorsToHighlight = if (callableDescriptor is ParameterDescriptor)
        listOf(callableDescriptor)
    else
        callableDescriptor?.findOriginalTopMostOverriddenDescriptors() ?: emptyList()

    return descriptorsToHighlight.mapNotNull { it.source.getPsi() }.filter { it != target }
}

val KtDeclaration.descriptor: DeclarationDescriptor?
    get() = if (this is KtParameter) this.descriptor else this.resolveToDescriptorIfAny(BodyResolveMode.FULL)

val KtParameter.descriptor: ValueParameterDescriptor?
    get() = this.resolveToParameterDescriptorIfAny(BodyResolveMode.FULL)

fun isCallReceiverRefersToCompanionObject(element: KtElement, companionObject: KtObjectDeclaration): Boolean {
    val companionObjectDescriptor = companionObject.descriptor
    val bindingContext = element.analyze()
    val resolvedCall = bindingContext[BindingContext.CALL, element]?.getResolvedCall(bindingContext) ?: return false
    return (resolvedCall.dispatchReceiver as? ImplicitClassReceiver)?.declarationDescriptor == companionObjectDescriptor ||
            (resolvedCall.extensionReceiver as? ImplicitClassReceiver)?.declarationDescriptor == companionObjectDescriptor
}

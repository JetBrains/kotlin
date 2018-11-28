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

package org.jetbrains.kotlin.js.translate.callTranslator

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.SideEffectKind
import org.jetbrains.kotlin.js.backend.ast.metadata.sideEffects
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.context.Namer.getCapturedVarAccessor
import org.jetbrains.kotlin.js.translate.declaration.contextWithPropertyMetadataCreationIntrinsified
import org.jetbrains.kotlin.js.translate.reference.ReferenceTranslator
import org.jetbrains.kotlin.js.translate.reference.buildReifiedTypeArgs
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils.pureFqn
import org.jetbrains.kotlin.js.translate.utils.JsDescriptorUtils
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject
import java.util.*

object NativeVariableAccessCase : VariableAccessCase() {
    override fun VariableAccessInfo.extensionReceiver(): JsExpression {
        return constructAccessExpression(JsNameRef(variableName, extensionReceiver!!))
    }

    override fun VariableAccessInfo.dispatchReceiver(): JsExpression {
        val descriptor = resolvedCall.resultingDescriptor
        return if (descriptor is PropertyDescriptor && TranslationUtils.shouldAccessViaFunctions(descriptor)) {
            val methodRef = context.getNameForDescriptor(getAccessDescriptorIfNeeded())
            JsInvocation(pureFqn(methodRef, dispatchReceiver!!), *additionalArguments.toTypedArray())
        }
        else {
            constructAccessExpression(JsNameRef(variableName, dispatchReceiver!!))
        }
    }

    override fun VariableAccessInfo.noReceivers(): JsExpression {
        val descriptor = resolvedCall.resultingDescriptor
        return if (descriptor is PropertyDescriptor && TranslationUtils.shouldAccessViaFunctions(descriptor)) {
            val methodRef = ReferenceTranslator.translateAsValueReference(getAccessDescriptorIfNeeded(), context)
            JsInvocation(methodRef, *additionalArguments.toTypedArray())
        }
        else {
            constructAccessExpression(context.getQualifiedReference(callableDescriptor))
        }
    }
}

object DefaultVariableAccessCase : VariableAccessCase() {
    override fun VariableAccessInfo.noReceivers(): JsExpression {
        val variableDescriptor = this.variableDescriptor

        if (variableDescriptor is PropertyDescriptor &&
            !JsDescriptorUtils.isSimpleFinalProperty(variableDescriptor) &&
            context.isFromCurrentModule(variableDescriptor)
        ) {
            val methodRef = context.getInnerReference(getAccessDescriptor())
            return JsInvocation(methodRef, *additionalArguments.toTypedArray())
        }

        val descriptor = resolvedCall.resultingDescriptor
        if (descriptor is PropertyDescriptor && TranslationUtils.shouldAccessViaFunctions(descriptor)) {
            val methodRef = ReferenceTranslator.translateAsValueReference(getAccessDescriptorIfNeeded(), context)
            return JsInvocation(methodRef, *additionalArguments.toTypedArray())
        }

        if (descriptor is FakeCallableDescriptorForObject) {
            return ReferenceTranslator.translateAsValueReference(descriptor.getReferencedObject(), context)
        }

        val functionRef = ReferenceTranslator.translateAsValueReference(callableDescriptor, context)
        val ref = if (context.isBoxedLocalCapturedInClosure(callableDescriptor)) {
            getCapturedVarAccessor(functionRef)
        }
        else {
            functionRef.apply {
                if (isGetAccess()) {
                    sideEffects = SideEffectKind.DEPENDS_ON_STATE
                }
            }
        }

        val localVariableDescriptor = resolvedCall.resultingDescriptor as? LocalVariableDescriptor
        val accessorDescriptor = if (isGetAccess()) localVariableDescriptor?.getter else localVariableDescriptor?.setter
        val delegatedCall = accessorDescriptor?.let { context.bindingContext()[BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, it] }
        if (delegatedCall != null) {
            val delegateContext = context.contextWithPropertyMetadataCreationIntrinsified(
                    delegatedCall, localVariableDescriptor!!, JsNullLiteral())
            val delegateContextWithArgs = if (!isGetAccess()) {
                val valueArg = delegatedCall.valueArgumentsByIndex!![2].arguments[0].getArgumentExpression()
                delegateContext.innerContextWithAliasesForExpressions(mapOf(valueArg to value!!))
            }
            else {
                delegateContext
            }
            val localVariableRef = context.getAliasForDescriptor(localVariableDescriptor) ?:
                                   JsAstUtils.pureFqn(context.getNameForDescriptor(localVariableDescriptor), null)
            return CallTranslator.translate(delegateContextWithArgs, delegatedCall, localVariableRef)
        }

        return constructAccessExpression(ref)
    }

    override fun VariableAccessInfo.dispatchReceiver(): JsExpression {
        val descriptor = resolvedCall.resultingDescriptor
        return if (descriptor is PropertyDescriptor && TranslationUtils.shouldAccessViaFunctions(descriptor)) {
            val callExpr = pureFqn(context.getNameForDescriptor(getAccessDescriptorIfNeeded()), dispatchReceiver!!)
            JsInvocation(callExpr, *additionalArguments.toTypedArray())
        }
        else {
            val accessor = JsNameRef(variableName, dispatchReceiver!!)
            if (descriptor is PropertyDescriptor && !JsDescriptorUtils.sideEffectsPossibleOnRead(descriptor)) {
                accessor.sideEffects = SideEffectKind.DEPENDS_ON_STATE
            }
            constructAccessExpression(accessor)
        }
    }

    override fun VariableAccessInfo.extensionReceiver(): JsExpression {
        val functionRef = ReferenceTranslator.translateAsValueReference(getAccessDescriptorIfNeeded(), context)
        val reifiedTypeArguments = resolvedCall.typeArguments.buildReifiedTypeArgs(context)
        return  JsInvocation(functionRef, reifiedTypeArguments + listOf(extensionReceiver!!) + additionalArguments)
    }

    override fun VariableAccessInfo.bothReceivers(): JsExpression {
        val funRef = JsAstUtils.pureFqn(context.getNameForDescriptor(getAccessDescriptorIfNeeded()), dispatchReceiver!!)
        return JsInvocation(funRef, extensionReceiver!!, *additionalArguments.toTypedArray())
    }
}

object DelegatePropertyAccessIntrinsic : DelegateIntrinsic<VariableAccessInfo> {
    override fun VariableAccessInfo.canBeApply(): Boolean {
        if(variableDescriptor is PropertyDescriptor) {
            return isGetAccess() || (variableDescriptor as PropertyDescriptor).isVar
        }
        return false
    }

    override fun VariableAccessInfo.getArgs(): List<JsExpression> {
        return if (isGetAccess())
            Collections.emptyList<JsExpression>()
        else
            Collections.singletonList(value!!)
    }

    override fun VariableAccessInfo.getDescriptor(): CallableDescriptor {
        val propertyDescriptor = variableDescriptor as PropertyDescriptor
        return if (isGetAccess()) {
            propertyDescriptor.getter!!
        } else {
            propertyDescriptor.setter!!
        }
    }
}

object SuperPropertyAccessCase : VariableAccessCase() {
    override fun VariableAccessInfo.dispatchReceiver(): JsExpression {
        val variableName = JsStringLiteral(this.variableName.ident)
        val descriptor = resolvedCall.resultingDescriptor

        return if (descriptor is PropertyDescriptor && TranslationUtils.shouldAccessViaFunctions(descriptor)) {
            val accessor = getAccessDescriptorIfNeeded()
            val containingRef = ReferenceTranslator.translateAsValueReference(descriptor.containingDeclaration, context)
            val prototype = pureFqn(Namer.getPrototypeName(), containingRef)
            val funRef = Namer.getFunctionCallRef(pureFqn(context.getNameForDescriptor(accessor), prototype))
            val arguments = listOf(dispatchReceiver!!) + additionalArguments
            JsInvocation(funRef, *arguments.toTypedArray())
        }
        else {
            val callExpr = if (isGetAccess()) context.namer().callGetProperty else context.namer().callSetProperty
            val arguments = listOf(dispatchReceiver!!, JsAstUtils.prototypeOf(calleeOwner), variableName) + additionalArguments
            JsInvocation(callExpr, *arguments.toTypedArray())
        }
    }
}

private val VariableAccessInfo.additionalArguments: List<JsExpression> get() = value?.let { listOf(it) }.orEmpty()

fun VariableAccessInfo.translateVariableAccess(): JsExpression {
    val intrinsic = DelegatePropertyAccessIntrinsic.intrinsic(this, context)

    return when {
        intrinsic != null ->
            intrinsic
        isSuperInvocation() ->
            SuperPropertyAccessCase.translate(this)
        isNative() ->
            NativeVariableAccessCase.translate(this)
        else ->
            DefaultVariableAccessCase.translate(this)
    }
}

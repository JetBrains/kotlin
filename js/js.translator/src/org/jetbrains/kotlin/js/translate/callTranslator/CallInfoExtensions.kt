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

package org.jetbrains.kotlin.js.translate.callTranslator

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.psi.JetSuperExpression
import com.google.dart.compiler.backend.js.ast.JsExpression
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import com.google.dart.compiler.backend.js.ast.JsName
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.jet.lang.resolve.descriptorUtil.isExtension


val CallInfo.callableDescriptor: CallableDescriptor
    get() = resolvedCall.getResultingDescriptor().getOriginal()

fun CallInfo.isExtension(): Boolean = extensionReceiver != null

fun CallInfo.isMemberCall(): Boolean = dispatchReceiver != null

fun CallInfo.isNative(): Boolean = AnnotationsUtils.isNativeObject(callableDescriptor)

fun CallInfo.isSuperInvocation(): Boolean {
    val dispatchReceiver = resolvedCall.getDispatchReceiver()
    return dispatchReceiver is ExpressionReceiver && dispatchReceiver.getExpression() is JetSuperExpression
}

val VariableAccessInfo.variableDescriptor: VariableDescriptor
    get() = callableDescriptor as VariableDescriptor

val VariableAccessInfo.variableName: JsName
    get() = context.getNameForDescriptor(variableDescriptor)

fun VariableAccessInfo.isGetAccess(): Boolean = value == null

fun VariableAccessInfo.getAccessFunctionName(): String {
    val descriptor = variableDescriptor
    if (descriptor is PropertyDescriptor && descriptor.isExtension) {
        val propertyAccessorDescriptor = if (isGetAccess()) descriptor.getGetter() else descriptor.getSetter()
        return context.getNameForDescriptor(propertyAccessorDescriptor).getIdent()
    }
    else {
        return Namer.getNameForAccessor(variableName.getIdent()!!, isGetAccess(), false)
    }
}

fun VariableAccessInfo.constructAccessExpression(ref: JsExpression): JsExpression {
    if (isGetAccess()) {
        return ref
    } else {
        return JsAstUtils.assignment(ref, value!!)
    }
}

val FunctionCallInfo.functionName: JsName
    get() = context.getNameForDescriptor(callableDescriptor)

fun FunctionCallInfo.hasSpreadOperator(): Boolean = argumentsInfo.isHasSpreadOperator()

fun TranslationContext.aliasOrValue(callableDescriptor: CallableDescriptor, value: (CallableDescriptor) -> JsExpression): JsExpression {
    val alias = getAliasForDescriptor(callableDescriptor)
    if (alias != null) {
        return alias
    } else {
        return value(callableDescriptor)
    }
}

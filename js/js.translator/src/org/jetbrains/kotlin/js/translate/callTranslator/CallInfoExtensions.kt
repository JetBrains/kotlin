/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import com.google.dart.compiler.backend.js.ast.JsExpression
import com.google.dart.compiler.backend.js.ast.JsName
import com.google.dart.compiler.backend.js.ast.metadata.HasMetadata
import com.google.dart.compiler.backend.js.ast.metadata.SideEffectKind
import com.google.dart.compiler.backend.js.ast.metadata.sideEffects
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.reference.ReferenceTranslator
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.psi.KtSuperExpression
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver


val CallInfo.callableDescriptor: CallableDescriptor
    get() = resolvedCall.resultingDescriptor.original

fun CallInfo.isExtension(): Boolean = extensionReceiver != null

fun CallInfo.isNative(): Boolean = AnnotationsUtils.isNativeObject(callableDescriptor)

fun CallInfo.isSuperInvocation(): Boolean {
    val dispatchReceiver = resolvedCall.dispatchReceiver
    return dispatchReceiver is ExpressionReceiver && dispatchReceiver.expression is KtSuperExpression
}

val CallInfo.calleeOwner: JsExpression
    get() {
        val calleeOwner = resolvedCall.resultingDescriptor.containingDeclaration
        return ReferenceTranslator.translateAsFQReference(calleeOwner, context)
    }

val VariableAccessInfo.variableDescriptor: VariableDescriptor
    get() = callableDescriptor as VariableDescriptor

val VariableAccessInfo.variableName: JsName
    get() = context.getNameForDescriptor(variableDescriptor)

fun VariableAccessInfo.isGetAccess(): Boolean = value == null

fun VariableAccessInfo.getAccessDescriptor(): DeclarationDescriptor {
    val descriptor = variableDescriptor
    if (descriptor is PropertyDescriptor && descriptor.isExtension) {
        val propertyAccessorDescriptor = if (isGetAccess()) descriptor.getter else descriptor.setter
        return propertyAccessorDescriptor!!
    }
    else {
        return variableDescriptor
    }
}

fun VariableAccessInfo.constructAccessExpression(ref: JsExpression): JsExpression {
    return if (isGetAccess()) {
        ref
    }
    else {
        // This is useful when passing AST to TemporaryAssignmentElimination. It can bring
        // property assignment like `obj.propertyName = $tmp` to places where `$tmp` gets its value,
        // but only when it's sure that no side effects possible.
        (ref as? HasMetadata)?.let { it.sideEffects = SideEffectKind.PURE }
        JsAstUtils.assignment(ref, value!!)
    }
}

val FunctionCallInfo.functionName: JsName
    get() = context.getNameForDescriptor(callableDescriptor)

fun FunctionCallInfo.hasSpreadOperator(): Boolean = argumentsInfo.hasSpreadOperator

fun TranslationContext.aliasOrValue(callableDescriptor: CallableDescriptor, value: (CallableDescriptor) -> JsExpression): JsExpression {
    val alias = getAliasForDescriptor(callableDescriptor)
    if (alias != null) {
        return alias
    } else {
        return value(callableDescriptor)
    }
}

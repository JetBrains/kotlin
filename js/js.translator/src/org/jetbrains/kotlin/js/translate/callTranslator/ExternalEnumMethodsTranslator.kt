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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns.isString
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.js.backend.ast.JsExpression
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.general.Translation
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall

typealias FunctionInvocation = ResolvedCall<out FunctionDescriptor>

class ExternalEnumMethodsTranslator(private val context: TranslationContext) {
    fun translateAsEnumMethodCall(resolvedCall: FunctionInvocation, receivers: ExplicitReceivers): JsExpression? {
        return resolvedCall.resultingDescriptor
            .takeIf { it.isExternalEnumDefaultMethod() }
            ?.generateCallOfTopLevelDefaultFunctions(resolvedCall, receivers.extensionOrDispatchReceiver ?: return null)
    }

    private fun FunctionDescriptor.isExternalEnumDefaultMethod(): Boolean {
        return containingDeclaration.isExternalEnum() && kind == CallableMemberDescriptor.Kind.SYNTHESIZED
    }

    private fun DeclarationDescriptor.isExternalEnum(): Boolean {
        return this is ClassDescriptor && isExternal && kind == ClassKind.ENUM_CLASS
    }

    private fun FunctionDescriptor.generateCallOfTopLevelDefaultFunctions(invocation: FunctionInvocation, receiver: JsExpression): JsExpression? {
        return with(invocation) {
            when {
                isValueOfMethod() -> translateToDefaultValueOfCall(receiver)
                isValuesMethod() -> translateToDefaultValuesCall(receiver)
                else -> null
            }
        }
    }

    private fun FunctionDescriptor.isValueOfMethod(): Boolean {
        return name.identifier == "valueOf" &&
                valueParameters.count() == 1 &&
                isString(valueParameters[0].type)
    }

    private fun FunctionDescriptor.isValuesMethod(): Boolean {
        return name.identifier == "values" && valueParameters.isEmpty()
    }

    private fun FunctionInvocation.translateToDefaultValueOfCall(receiver: JsExpression): JsExpression {
        return JsAstUtils.invokeKotlinFunction(
            "defaultEnumValueOf",
            receiver,
            Translation.translateAsExpression(valueArguments.values.single().arguments.single().getArgumentExpression()!!, context)
        )
    }

    private fun translateToDefaultValuesCall(receiver: JsExpression): JsExpression {
        return JsAstUtils.invokeKotlinFunction("defaultEnumValues", receiver)
    }
}



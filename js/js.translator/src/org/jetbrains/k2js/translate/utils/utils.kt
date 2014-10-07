/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.k2js.translate.utils

import com.google.dart.compiler.backend.js.ast.JsExpression
import com.google.dart.compiler.backend.js.ast.JsInvocation
import com.google.dart.compiler.backend.js.ast.JsNameRef
import com.google.dart.compiler.backend.js.ast.JsParameter
import com.google.dart.compiler.backend.js.ast.JsPropertyInitializer
import com.intellij.util.SmartList
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor
import org.jetbrains.k2js.translate.context.Namer
import org.jetbrains.k2js.translate.context.TranslationContext
import org.jetbrains.k2js.translate.utils.TranslationUtils.simpleReturnFunction

public val <T> ID: (T) -> T = { it }

public fun generateDelegateCall(
        fromDescriptor: FunctionDescriptor,
        toDescriptor: FunctionDescriptor,
        thisObject: JsExpression,
        context: TranslationContext
): JsPropertyInitializer {
    val delegateMemberFunctionName = context.getNameForDescriptor(fromDescriptor)
    val overriddenMemberFunctionName = context.getNameForDescriptor(toDescriptor)
    val overriddenMemberFunctionRef = JsNameRef(overriddenMemberFunctionName, thisObject)

    val parameters = SmartList<JsParameter>()
    val args = SmartList<JsExpression>()
    val functionScope = context.getScopeForDescriptor(fromDescriptor);

    if (JsDescriptorUtils.isExtension(fromDescriptor)) {
        val extensionFunctionReceiverName = functionScope.declareName(Namer.getReceiverParameterName())
        parameters.add(JsParameter(extensionFunctionReceiverName))
        args.add(JsNameRef(extensionFunctionReceiverName))
    }

    for (param in fromDescriptor.getValueParameters()) {
        val paramName = param.getName().asString()
        val jsParamName = functionScope.declareName(paramName)
        parameters.add(JsParameter(jsParamName))
        args.add(JsNameRef(jsParamName))
    }

    val functionObject = simpleReturnFunction(context.getScopeForDescriptor(fromDescriptor), JsInvocation(overriddenMemberFunctionRef, args))
    functionObject.getParameters().addAll(parameters)
    return JsPropertyInitializer(delegateMemberFunctionName.makeRef(), functionObject)
}

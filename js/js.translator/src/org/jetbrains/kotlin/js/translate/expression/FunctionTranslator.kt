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

package org.jetbrains.kotlin.js.translate.expression

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.js.backend.ast.JsExpression
import org.jetbrains.kotlin.js.backend.ast.JsFunction
import org.jetbrains.kotlin.js.backend.ast.JsParameter
import org.jetbrains.kotlin.js.backend.ast.metadata.functionDescriptor
import org.jetbrains.kotlin.js.backend.ast.metadata.hasDefaultValue
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.reference.CallExpressionTranslator.shouldBeInlined
import org.jetbrains.kotlin.js.translate.utils.BindingUtils
import org.jetbrains.kotlin.js.translate.utils.FunctionBodyTranslator.translateFunctionBody
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.hasDefaultValue
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyPublicApi

fun TranslationContext.translateAndAliasParameters(
        descriptor: FunctionDescriptor,
        targetList: MutableList<JsParameter>
): TranslationContext {
    val aliases = mutableMapOf<DeclarationDescriptor, JsExpression>()

    for (type in descriptor.getCorrectTypeParameters()) {
        if (type.isReified) {
            val paramNameForType = getNameForDescriptor(type)
            targetList += JsParameter(paramNameForType)

            val suggestedName = Namer.isInstanceSuggestedName(type)
            val paramName = scope().declareTemporaryName(suggestedName)
            targetList += JsParameter(paramName)
            aliases[type] = paramName.makeRef()
        }
    }

    if (descriptor.requiresExtensionReceiverParameter) {
        val receiverParameterName = scope().declareTemporaryName(Namer.getReceiverParameterName())
        aliases[descriptor.extensionReceiverParameter!!] = receiverParameterName.makeRef()
        targetList += JsParameter(receiverParameterName)
    }

    for (valueParameter in descriptor.valueParameters) {
        targetList += JsParameter(getNameForDescriptor(valueParameter)).apply { hasDefaultValue = valueParameter.hasDefaultValue() }
    }

    val continuationDescriptor = continuationParameterDescriptor
    if (continuationDescriptor != null) {
        val jsParameter = JsParameter(getNameForDescriptor(continuationDescriptor))
        targetList += jsParameter
    }

    return this.innerContextWithDescriptorsAliased(aliases)
}

private fun FunctionDescriptor.getCorrectTypeParameters() =
    (this as? PropertyAccessorDescriptor)?.correspondingProperty?.typeParameters ?: typeParameters


private val FunctionDescriptor.requiresExtensionReceiverParameter
    get() = DescriptorUtils.isExtension(this)

fun TranslationContext.translateFunction(declaration: KtDeclarationWithBody, function: JsFunction) {
    val descriptor = BindingUtils.getFunctionDescriptor(bindingContext(), declaration)
    if (declaration.hasBody()) {
        val body = translateFunctionBody(descriptor, declaration, this)
        function.body.statements += body.statements
    }
    function.functionDescriptor = descriptor
}

fun TranslationContext.wrapWithInlineMetadata(function: JsFunction, descriptor: FunctionDescriptor): JsExpression {
    return if (shouldBeInlined(descriptor, this) && descriptor.isEffectivelyPublicApi) {
        val metadata = InlineMetadata.compose(function, descriptor)
        metadata.functionWithMetadata
    }
    else {
        function
    }
}

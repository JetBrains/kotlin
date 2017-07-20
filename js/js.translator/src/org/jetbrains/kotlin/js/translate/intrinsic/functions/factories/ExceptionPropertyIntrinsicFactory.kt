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

package org.jetbrains.kotlin.js.translate.intrinsic.functions.factories

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.js.backend.ast.JsExpression
import org.jetbrains.kotlin.js.backend.ast.JsNameRef
import org.jetbrains.kotlin.js.translate.callTranslator.CallInfo
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.intrinsic.functions.basic.FunctionIntrinsic
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.receivers.SuperCallReceiverValue
import org.jetbrains.kotlin.types.typeUtil.isNotNullThrowable

object ExceptionPropertyIntrinsicFactory : FunctionIntrinsicFactory {
    override fun getIntrinsic(descriptor: FunctionDescriptor): FunctionIntrinsic? {
        if (descriptor !is PropertyGetterDescriptor) return null
        val classDescriptor = descriptor.correspondingProperty.containingDeclaration as? ClassDescriptor ?: return null
        if (!classDescriptor.defaultType.isNotNullThrowable()) return null

        return Intrinsic
    }

    object Intrinsic : FunctionIntrinsic() {
        override fun apply(callInfo: CallInfo, arguments: List<JsExpression>, context: TranslationContext): JsExpression {
            val property = callInfo.resolvedCall.resultingDescriptor as PropertyDescriptor
            if (callInfo.resolvedCall.call.explicitReceiver !is SuperCallReceiverValue) {
                val name = context.getNameForDescriptor(property)
                return JsNameRef(name, callInfo.dispatchReceiver!!)
            }

            val currentClassProperty = context.classDescriptor!!.unsubstitutedMemberScope
                    .getContributedDescriptors(DescriptorKindFilter.CALLABLES)
                    .filterIsInstance<PropertyDescriptor>()
                    .first { it.overriddenDescriptors.any { it == property } }
            return JsAstUtils.pureFqn(context.getNameForBackingField(currentClassProperty), callInfo.dispatchReceiver!!)
        }
    }
}
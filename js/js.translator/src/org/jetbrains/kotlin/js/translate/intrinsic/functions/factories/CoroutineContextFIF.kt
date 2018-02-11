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

package org.jetbrains.kotlin.js.translate.intrinsic.functions.factories

import org.jetbrains.kotlin.backend.common.isBuiltInCoroutineContext
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.js.backend.ast.JsExpression
import org.jetbrains.kotlin.js.backend.ast.JsNameRef
import org.jetbrains.kotlin.js.backend.ast.metadata.inlineStrategy
import org.jetbrains.kotlin.js.descriptorUtils.isCoroutineLambda
import org.jetbrains.kotlin.js.translate.callTranslator.CallInfo
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.intrinsic.functions.basic.FunctionIntrinsic
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.inline.InlineStrategy

object CoroutineContextFIF : FunctionIntrinsicFactory {
    override fun getIntrinsic(descriptor: FunctionDescriptor): FunctionIntrinsic? {
        if (!descriptor.isBuiltInCoroutineContext()) return null
        return Intrinsic
    }

    object Intrinsic : FunctionIntrinsic() {
        override fun apply(callInfo: CallInfo, arguments: List<JsExpression>, context: TranslationContext): JsExpression {
            val continuation = TranslationUtils.getEnclosingContinuationParameter(context)
            val continuationDescriptor = continuation.type.constructor.declarationDescriptor as? ClassDescriptor ?: error("Continuation is not a class")
            val contContext = DescriptorUtils.getPropertyByName(continuationDescriptor.unsubstitutedMemberScope, Name.identifier("context"))
            val res = JsNameRef(
                context.getNameForDescriptor(contContext),
                if (context.declarationDescriptor?.isCoroutineLambda == true) JsAstUtils.stateMachineReceiver()
                else TranslationUtils.translateContinuationArgument(context)
            )
            res.inlineStrategy = InlineStrategy.NOT_INLINE
            return res
        }
    }
}

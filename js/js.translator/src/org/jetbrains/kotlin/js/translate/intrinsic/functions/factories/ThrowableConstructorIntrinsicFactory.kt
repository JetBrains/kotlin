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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.js.backend.ast.JsExpression
import org.jetbrains.kotlin.js.backend.ast.JsInvocation
import org.jetbrains.kotlin.js.backend.ast.JsLiteral
import org.jetbrains.kotlin.js.translate.callTranslator.CallInfo
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.intrinsic.functions.basic.FunctionIntrinsic
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.typeUtil.isThrowable

object ThrowableConstructorIntrinsicFactory : FunctionIntrinsicFactory {
    override fun getIntrinsic(descriptor: FunctionDescriptor): FunctionIntrinsic? {
        if (descriptor !is ConstructorDescriptor) return null
        if (!descriptor.constructedClass.defaultType.isThrowable()) return null

        return Intrinsic
    }

    object Intrinsic : FunctionIntrinsic() {
        override fun apply(callInfo: CallInfo, arguments: List<JsExpression>, context: TranslationContext): JsExpression {
            val constructor = callInfo.resolvedCall.resultingDescriptor
            val argumentsToPass = arguments.toMutableList()
            val hasCauseParameter = constructor.valueParameters.any {
                it.type.constructor.declarationDescriptor?.fqNameSafe == KotlinBuiltIns.FQ_NAMES.throwable
            }

            if (constructor.valueParameters.size == 1 && hasCauseParameter) {
                argumentsToPass.add(0, Namer.getUndefinedExpression())
            }

            return JsInvocation(JsAstUtils.pureFqn("newThrowable", Namer.kotlinObject()), argumentsToPass)
        }
    }
}
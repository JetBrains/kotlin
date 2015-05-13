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

package org.jetbrains.kotlin.js.translate.intrinsic.operation

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.psi.JetBinaryExpression
import org.jetbrains.kotlin.lexer.JetToken
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.utils.BindingUtils.getCallableDescriptorForOperationExpression
import org.jetbrains.kotlin.js.translate.utils.PsiUtils.getOperationToken
import gnu.trove.THashMap
import com.google.dart.compiler.backend.js.ast.JsExpression
import com.google.common.collect.ImmutableSet

public trait BinaryOperationIntrinsic {

    fun apply(expression: JetBinaryExpression, left: JsExpression, right: JsExpression, context: TranslationContext): JsExpression

    fun exists(): Boolean
}

public class BinaryOperationIntrinsics {

    private val intrinsicCache = THashMap<Pair<JetToken, FunctionDescriptor>, BinaryOperationIntrinsic>()

    private val factories = listOf(LongCompareToBOIF, EqualsBOIF, CompareToBOIF)

    public fun getIntrinsic(expression: JetBinaryExpression, context: TranslationContext): BinaryOperationIntrinsic {
        val token = getOperationToken(expression)
        val descriptor = getCallableDescriptorForOperationExpression(context.bindingContext(), expression)
        if (descriptor == null || descriptor !is FunctionDescriptor) {
            return NO_INTRINSIC
        }

        return lookUpCache(token, descriptor) ?: computeAndCacheIntrinsic(token, descriptor)
    }

    private fun lookUpCache(token: JetToken, descriptor: FunctionDescriptor): BinaryOperationIntrinsic? =
            intrinsicCache.get(Pair(token, descriptor))

    private fun computeAndCacheIntrinsic(token: JetToken, descriptor: FunctionDescriptor): BinaryOperationIntrinsic {
        val result = computeIntrinsic(token, descriptor)
        intrinsicCache.put(Pair(token, descriptor), result)
        return result
    }

    private fun computeIntrinsic(token: JetToken, descriptor: FunctionDescriptor): BinaryOperationIntrinsic {
        for (factory in factories) {
            if (factory.getSupportTokens().contains(token)) {
                val intrinsic = factory.getIntrinsic(descriptor)
                if (intrinsic != null) {
                    return intrinsic
                }
            }
        }
        return NO_INTRINSIC
    }
}

trait BinaryOperationIntrinsicFactory {

    public fun getSupportTokens(): ImmutableSet<out JetToken>

    public fun getIntrinsic(descriptor: FunctionDescriptor): BinaryOperationIntrinsic?
}

abstract class AbstractBinaryOperationIntrinsic : BinaryOperationIntrinsic {

    public override abstract fun apply(expression: JetBinaryExpression, left: JsExpression, right: JsExpression, context: TranslationContext): JsExpression

    public override fun exists(): Boolean = true
}

object NO_INTRINSIC : AbstractBinaryOperationIntrinsic() {
    override fun exists(): Boolean = false

    override fun apply(expression: JetBinaryExpression, left: JsExpression, right: JsExpression, context: TranslationContext): JsExpression =
            throw UnsupportedOperationException("BinaryOperationIntrinsic#NO_INTRINSIC_#apply")
}

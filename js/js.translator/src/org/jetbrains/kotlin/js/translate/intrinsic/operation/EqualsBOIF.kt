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

import com.google.dart.compiler.backend.js.ast.*
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.js.patterns.NamePredicate
import org.jetbrains.kotlin.js.patterns.PatternBuilder.pattern
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.intrinsic.functions.factories.TopLevelFIF
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.js.translate.utils.JsDescriptorUtils
import org.jetbrains.kotlin.js.translate.utils.PsiUtils.getOperationToken
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils
import org.jetbrains.kotlin.lexer.JetToken
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.JetBinaryExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.types.isDynamic

import java.util.Arrays
import com.google.common.collect.ImmutableSet

object EqualsBOIF : BinaryOperationIntrinsicFactory {

    val LONG_EQUALS_ANY = pattern("Long.equals")

    private object LONG_EQUALS_ANY_INTRINSIC : AbstractBinaryOperationIntrinsic() {
        override fun apply(expression: JetBinaryExpression, left: JsExpression, right: JsExpression, context: TranslationContext): JsExpression {
            val isNegated = getOperationToken(expression) == JetTokens.EXCLEQ
            val invokeEquals = JsAstUtils.equalsForObject(left, right)
            return if (isNegated) JsAstUtils.not(invokeEquals) else invokeEquals
        }
    }

    private object EqualsIntrinsic : AbstractBinaryOperationIntrinsic() {
        override fun apply(expression: JetBinaryExpression, left: JsExpression, right: JsExpression, context: TranslationContext): JsExpression {
            val isNegated = getOperationToken(expression) == JetTokens.EXCLEQ
            if (right == JsLiteral.NULL || left == JsLiteral.NULL) {
                return TranslationUtils.nullCheck(if (right == JsLiteral.NULL) left else right, isNegated)
            }
            else if (canUseSimpleEquals(expression, context)) {
                return JsBinaryOperation(if (isNegated) JsBinaryOperator.REF_NEQ else JsBinaryOperator.REF_EQ, left, right)
            }

            val resolvedCall = expression.getResolvedCall(context.bindingContext())
            val appliedToDynamic =
                    resolvedCall != null &&
                    with(resolvedCall.getDispatchReceiver()) {
                        if (exists()) getType().isDynamic() else false
                    }

            if (appliedToDynamic) {
                return JsBinaryOperation(if (isNegated) JsBinaryOperator.NEQ else JsBinaryOperator.EQ, left, right)
            }

            val result = TopLevelFIF.KOTLIN_EQUALS.apply(left, Arrays.asList<JsExpression>(right), context)
            return if (isNegated) JsAstUtils.negated(result) else result
        }

        private fun canUseSimpleEquals(expression: JetBinaryExpression, context: TranslationContext): Boolean {
            val left = expression.getLeft()
            assert(left != null) { "No left-hand side: " + expression.getText() }
            val typeName = JsDescriptorUtils.getNameIfStandardType(left!!, context)
            return typeName != null && NamePredicate.PRIMITIVE_NUMBERS_MAPPED_TO_PRIMITIVE_JS.apply(typeName)
        }
    }

    override public fun getSupportTokens() = OperatorConventions.EQUALS_OPERATIONS

    override public fun getIntrinsic(descriptor: FunctionDescriptor): BinaryOperationIntrinsic? {
        if (JsDescriptorUtils.isBuiltin(descriptor) || TopLevelFIF.EQUALS_IN_ANY.apply(descriptor)) {
            return if (LONG_EQUALS_ANY.apply(descriptor)) LONG_EQUALS_ANY_INTRINSIC else EqualsIntrinsic
        }
        return null
    }
}

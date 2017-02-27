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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.js.backend.ast.JsBinaryOperation
import org.jetbrains.kotlin.js.backend.ast.JsBinaryOperator
import org.jetbrains.kotlin.js.backend.ast.JsExpression
import org.jetbrains.kotlin.js.backend.ast.JsLiteral
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.general.Translation
import org.jetbrains.kotlin.js.translate.intrinsic.functions.factories.TopLevelFIF
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.js.translate.utils.PsiUtils.getOperationToken
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfoBefore
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.types.isDynamic
import java.util.*

object EqualsBOIF : BinaryOperationIntrinsicFactory {
    private object EqualsIntrinsic : AbstractBinaryOperationIntrinsic() {

        // Primitives with no boxing or tricky NaN behavior, represented by a Number at runtime.
        // Whenever the left side of equality is of this type, equality could be tested via '==='.
        private val SIMPLE_PRIMITIVES = EnumSet.of(PrimitiveType.BYTE, PrimitiveType.SHORT, PrimitiveType.INT)

        override fun apply(expression: KtBinaryExpression, left: JsExpression, right: JsExpression, context: TranslationContext): JsExpression {
            val isNegated = expression.isNegated()
            if (right == JsLiteral.NULL || left == JsLiteral.NULL) {
                return TranslationUtils.nullCheck(if (right == JsLiteral.NULL) left else right, isNegated)
            }

            val ktLeft = checkNotNull(expression.left) { "No left-hand side: " + expression.text }
            val ktRight = checkNotNull(expression.right) { "No right-hand side: " + expression.text }
            val leftType = getRefinedType(ktLeft, context)?.let { KotlinBuiltIns.getPrimitiveTypeByKotlinType(it) }
            val rightType = getRefinedType(ktRight, context)?.let { KotlinBuiltIns.getPrimitiveTypeByKotlinType(it) }

            if (leftType != null && (leftType in SIMPLE_PRIMITIVES || leftType == rightType && leftType != PrimitiveType.LONG)) {
                return JsBinaryOperation(if (isNegated) JsBinaryOperator.REF_NEQ else JsBinaryOperator.REF_EQ,
                                         Translation.unboxIfNeeded(left, leftType == PrimitiveType.CHAR),
                                         Translation.unboxIfNeeded(right, rightType == PrimitiveType.CHAR))
            }

            val resolvedCall = expression.getResolvedCall(context.bindingContext())
            val appliedToDynamic =
                    resolvedCall != null &&
                    with(resolvedCall.dispatchReceiver) {
                        if (this != null) type.isDynamic() else false
                    }

            if (appliedToDynamic) {
                return JsBinaryOperation(if (isNegated) JsBinaryOperator.NEQ else JsBinaryOperator.EQ, left, right)
            }

            val maybeBoxedLeft = if (leftType == PrimitiveType.CHAR) JsAstUtils.charToBoxedChar(left) else left
            val maybeBoxedRight = if (rightType == PrimitiveType.CHAR) JsAstUtils.charToBoxedChar(right) else right

            val result = TopLevelFIF.KOTLIN_EQUALS.apply(maybeBoxedLeft, Arrays.asList<JsExpression>(maybeBoxedRight), context)
            return if (isNegated) JsAstUtils.not(result) else result
        }

        /**
         * Tries to get as precise statically known primitive type as possible - taking smart-casts and generic supertypes into account.
         * This is needed to be compatible with JVM NaN behaviour, in particular the following two cases.
         *
         * // Smart-casts
         * val a: Any = Double.NaN
         * println(a == a) // true
         * if (a is Double) {
         *   println(a == a) // false
         * }
         *
         * // Generics with Double super-type
         * fun <T: Double> foo(v: T) = println(v == v)
         * foo(Double.NaN) // false
         *
         * Also see org/jetbrains/kotlin/codegen/codegenUtil.kt#calcTypeForIEEE754ArithmeticIfNeeded
         */
        private fun getRefinedType(expression: KtExpression, context: TranslationContext): KotlinType? {
            val bindingContext = context.bindingContext()
            val descriptor = context.declarationDescriptor ?: context.currentModule
            val ktType = bindingContext.getType(expression) ?: return null

            val dataFlow = DataFlowValueFactory.createDataFlowValue(expression, ktType, bindingContext, descriptor)
            val isPrimitiveFn = KotlinBuiltIns::isPrimitiveTypeOrNullablePrimitiveType

            return bindingContext.getDataFlowInfoBefore(expression).getStableTypes(dataFlow).find(isPrimitiveFn) ?: // Smart-casts
                   TypeUtils.getAllSupertypes(ktType).find(isPrimitiveFn) ?: // Generic super-types
                   ktType // Default
        }
    }

    object EnumEqualsIntrinsic : AbstractBinaryOperationIntrinsic() {
        override fun apply(expression: KtBinaryExpression, left: JsExpression, right: JsExpression, context: TranslationContext): JsBinaryOperation {
            val operator = if (expression.isNegated()) JsBinaryOperator.REF_NEQ else JsBinaryOperator.REF_EQ
            return JsBinaryOperation(operator, left, right)
        }
    }

    override fun getSupportTokens() = OperatorConventions.EQUALS_OPERATIONS!!

    override fun getIntrinsic(descriptor: FunctionDescriptor, leftType: KotlinType?, rightType: KotlinType?): BinaryOperationIntrinsic? {
        val result = when {
            isEnumIntrinsicApplicable(descriptor, leftType, rightType) -> EnumEqualsIntrinsic

            KotlinBuiltIns.isBuiltIn(descriptor) ||
            TopLevelFIF.EQUALS_IN_ANY.apply(descriptor) -> EqualsIntrinsic

            else -> null
        }

        return result
    }

    private fun isEnumIntrinsicApplicable(descriptor: FunctionDescriptor, leftType: KotlinType?, rightType: KotlinType?): Boolean {
        return DescriptorUtils.isEnumClass(descriptor.containingDeclaration) && leftType != null && rightType != null &&
               !TypeUtils.isNullableType(leftType) && !TypeUtils.isNullableType(rightType)
    }

    private fun KtBinaryExpression.isNegated() = getOperationToken(this) == KtTokens.EXCLEQ
}

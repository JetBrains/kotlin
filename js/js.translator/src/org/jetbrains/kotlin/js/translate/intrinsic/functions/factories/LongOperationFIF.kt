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

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.js.backend.ast.JsExpression
import org.jetbrains.kotlin.js.patterns.PatternBuilder.pattern
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.intrinsic.functions.basic.FunctionIntrinsic
import org.jetbrains.kotlin.js.translate.intrinsic.functions.basic.FunctionIntrinsicWithReceiverComputed
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils.*
import org.jetbrains.kotlin.utils.identity as ID

// TODO Move to FunctionCallCases
object LongOperationFIF : FunctionIntrinsicFactory {

    val LONG_EQUALS_ANY = pattern("Long.equals")
    val LONG_BINARY_OPERATION_LONG = pattern("Long.compareTo|rangeTo|plus|minus|times|div|mod|rem|and|or|xor(Long)")
    val LONG_BIT_SHIFTS = pattern("Long.shl|shr|ushr(Int)")
    val LONG_BINARY_OPERATION_INTEGER = pattern("Long.compareTo|rangeTo|plus|minus|times|div|mod|rem(Int|Short|Byte)")
    val LONG_BINARY_OPERATION_FLOATING_POINT = pattern("Long.compareTo|plus|minus|times|div|mod|rem(Double|Float)")
    val INTEGER_BINARY_OPERATION_LONG = pattern("Int|Short|Byte.compareTo|rangeTo|plus|minus|times|div|mod|rem(Long)")
    val FLOATING_POINT_BINARY_OPERATION_LONG = pattern("Double|Float.compareTo|plus|minus|times|div|mod|rem(Long)")

    private val longBinaryIntrinsics =
            (
                    listOf(
                            "equals" to Namer.EQUALS_METHOD_NAME,
                            "compareTo" to Namer.COMPARE_TO_METHOD_NAME,
                            "rangeTo" to "rangeTo",
                            "plus" to "add",
                            "minus" to "subtract",
                            "times" to "multiply",
                            "div" to "div",
                            "mod" to "modulo",
                            "rem" to "modulo",
                            "shl" to "shiftLeft",
                            "shr" to "shiftRight",
                            "ushr" to "shiftRightUnsigned",
                            "and" to "and",
                            "or" to "or",
                            "xor" to "xor"
                    ).map { it.first to methodIntrinsic(it.second) }).toMap()

    private val floatBinaryIntrinsics: Map<String, BaseBinaryIntrinsic> =
            mapOf(
                    "compareTo" to BaseBinaryIntrinsic(::compareTo),
                    "plus" to BaseBinaryIntrinsic(::sum),
                    "minus" to BaseBinaryIntrinsic(::subtract),
                    "times" to BaseBinaryIntrinsic(::mul),
                    "div" to BaseBinaryIntrinsic(::div),
                    "mod" to BaseBinaryIntrinsic(::mod),
                    "rem" to BaseBinaryIntrinsic(::mod)
            )

    class BaseBinaryIntrinsic(val applyFun: (left: JsExpression, right: JsExpression) -> JsExpression) :
            FunctionIntrinsicWithReceiverComputed() {
        override fun apply(receiver: JsExpression?, arguments: List<JsExpression>, context: TranslationContext): JsExpression {
            assert(receiver != null)
            assert(arguments.size == 1)
            return applyFun(receiver!!, arguments.get(0))
        }
    }

    fun methodIntrinsic(methodName: String): BaseBinaryIntrinsic =
            BaseBinaryIntrinsic() { left, right -> invokeMethod(left, methodName, right) }

    fun wrapIntrinsicIfPresent(intrinsic: BaseBinaryIntrinsic?, toLeft: (JsExpression) -> JsExpression, toRight: (JsExpression) -> JsExpression): FunctionIntrinsic? =
        if (intrinsic != null) BaseBinaryIntrinsic() { left, right -> intrinsic.applyFun(toLeft(left), toRight(right)) }  else null

   override fun getIntrinsic(descriptor: FunctionDescriptor): FunctionIntrinsic? {
       val operationName = descriptor.name.asString()
       return when {
           LONG_EQUALS_ANY.test(descriptor) || LONG_BINARY_OPERATION_LONG.test(descriptor) || LONG_BIT_SHIFTS.test(descriptor) ->
               longBinaryIntrinsics[operationName]
           INTEGER_BINARY_OPERATION_LONG.test(descriptor) ->
               wrapIntrinsicIfPresent(longBinaryIntrinsics[operationName], ::longFromInt, ID())
           LONG_BINARY_OPERATION_INTEGER.test(descriptor) ->
               wrapIntrinsicIfPresent(longBinaryIntrinsics[operationName], ID(), ::longFromInt)
           FLOATING_POINT_BINARY_OPERATION_LONG.test(descriptor) ->
               wrapIntrinsicIfPresent(floatBinaryIntrinsics[operationName], ID(), { invokeMethod(it, Namer.LONG_TO_NUMBER) })
           LONG_BINARY_OPERATION_FLOATING_POINT.test(descriptor) ->
               wrapIntrinsicIfPresent(floatBinaryIntrinsics[operationName], { invokeMethod(it, Namer.LONG_TO_NUMBER) }, ID())
           else ->
               null
       }
    }
}

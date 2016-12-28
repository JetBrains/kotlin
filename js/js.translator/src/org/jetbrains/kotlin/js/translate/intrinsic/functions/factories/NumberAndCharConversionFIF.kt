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

package org.jetbrains.kotlin.js.translate.intrinsic.functions.factories

import com.google.common.base.Predicates
import org.jetbrains.kotlin.js.backend.ast.JsExpression
import org.jetbrains.kotlin.js.patterns.PatternBuilder.pattern
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.intrinsic.functions.basic.FunctionIntrinsicWithReceiverComputed
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils.*
import org.jetbrains.kotlin.utils.identity

object NumberAndCharConversionFIF : CompositeFIF() {
    val USE_AS_IS = Predicates.or(
            pattern("Int.toInt|toFloat|toDouble"), pattern("Short.toShort|toInt|toFloat|toDouble"),
            pattern("Byte.toByte|toShort|toInt|toFloat|toDouble"), pattern("Float|Double.toFloat|toDouble"),
            pattern("Long.toLong"), pattern("Char.toChar")
    )

    private val convertOperations: Map<String, ConversionUnaryIntrinsic>  =
            mapOf(
                    "Float|Double.toInt" to ConversionUnaryIntrinsic { toInt32(it) },
                    "Int|Float|Double.toShort" to ConversionUnaryIntrinsic { toShort(it) },
                    "Short|Int|Float|Double.toByte" to ConversionUnaryIntrinsic { toByte(it) },

                    "Int|Short|Byte.toLong" to ConversionUnaryIntrinsic { longFromInt(it) },
                    "Float|Double.toLong" to ConversionUnaryIntrinsic { longFromNumber(it) },

                    "Char.toDouble|toFloat|toInt" to ConversionUnaryIntrinsic { charToInt(it) },
                    "Char.toShort" to ConversionUnaryIntrinsic { toShort(charToInt(it)) },
                    "Char.toByte" to ConversionUnaryIntrinsic { toByte(charToInt(it)) },
                    "Char.toLong" to ConversionUnaryIntrinsic { longFromInt(charToInt(it)) },

                    "Number.toInt" to ConversionUnaryIntrinsic { invokeKotlinFunction("numberToInt", it) },
                    "Number.toShort" to ConversionUnaryIntrinsic { invokeKotlinFunction("numberToShort", it) },
                    "Number.toByte" to ConversionUnaryIntrinsic { invokeKotlinFunction("numberToByte", it) },
                    "Number.toChar" to ConversionUnaryIntrinsic { invokeKotlinFunction("numberToChar", it) },
                    "Number.toFloat|toDouble" to ConversionUnaryIntrinsic { invokeKotlinFunction("numberToDouble", it) },
                    "Number.toLong" to ConversionUnaryIntrinsic { invokeKotlinFunction("numberToLong", it) },

                    "Int|Short|Byte|Float|Double.toChar" to  ConversionUnaryIntrinsic { toChar(it) },

                    "Long.toFloat|toDouble" to  ConversionUnaryIntrinsic { invokeMethod(it, "toNumber") },
                    "Long.toInt" to  ConversionUnaryIntrinsic { invokeMethod(it, "toInt") },
                    "Long.toShort" to  ConversionUnaryIntrinsic { toShort(invokeMethod(it, "toInt")) },
                    "Long.toByte" to  ConversionUnaryIntrinsic { toByte(invokeMethod(it, "toInt")) },
                    "Long.toChar" to  ConversionUnaryIntrinsic { toChar(invokeMethod(it, "toInt")) }

            )

    class ConversionUnaryIntrinsic(val applyFun: (receiver: JsExpression) -> JsExpression) : FunctionIntrinsicWithReceiverComputed() {
        override fun apply(receiver: JsExpression?, arguments: List<JsExpression>, context: TranslationContext): JsExpression {
            assert(receiver != null)
            assert(arguments.isEmpty())
            return applyFun(receiver!!)
        }
    }

    init {
        add(USE_AS_IS!!, ConversionUnaryIntrinsic(identity()))
        for((stringPattern, intrinsic) in convertOperations) {
            add(pattern(stringPattern), intrinsic)
        }
    }
}

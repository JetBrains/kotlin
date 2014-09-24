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

package org.jetbrains.k2js.translate.intrinsic.functions.factories

import com.google.dart.compiler.backend.js.ast.JsExpression
import org.jetbrains.k2js.translate.context.TranslationContext
import org.jetbrains.k2js.translate.intrinsic.functions.basic.FunctionIntrinsic
import org.jetbrains.k2js.translate.utils.JsAstUtils

import org.jetbrains.k2js.translate.intrinsic.functions.patterns.PatternBuilder.pattern

//TODO: support longs and chars
public object NumberConversionFIF : CompositeFIF() {

    //NOTE: treat Number as if it is floating point type
    private val convertOperations: Map<String, (receiver: JsExpression, context: TranslationContext) ->JsExpression>  =
            mapOf(
                    "Int.toInt|toFloat|toDouble" to { (receiver, context) -> receiver },
                    "Short.toShort|toInt|toFloat|toDouble" to { (receiver, context) -> receiver },
                    "Byte.toByte|toShort|toInt|toFloat|toDouble" to { (receiver, context) -> receiver },
                    "Float|Double|Number.toFloat|toDouble" to { (receiver, context) -> receiver },

                    "Float|Double|Number.toInt" to { (receiver, context) -> JsAstUtils.toInt32(receiver, context) },
                    "Int|Float|Double|Number.toShort" to { (receiver, context) -> JsAstUtils.toShort(receiver) },
                    "Short|Int|Float|Double|Number.toByte" to { (receiver, context) -> JsAstUtils.toByte(receiver) }
            )

    fun newUnaryIntrinsic(applyFun: (receiver: JsExpression, context: TranslationContext) -> JsExpression): FunctionIntrinsic =
            object : FunctionIntrinsic() {
                override fun apply(receiver: JsExpression?, arguments: List<JsExpression>, context: TranslationContext): JsExpression {
                    assert(receiver != null)
                    assert(arguments.size() == 0)
                    return applyFun(receiver!!, context)
                }
            }

    {
        for((stringPattern, applyFun) in convertOperations) {
            add(pattern(stringPattern), newUnaryIntrinsic(applyFun))
        }
    }
}

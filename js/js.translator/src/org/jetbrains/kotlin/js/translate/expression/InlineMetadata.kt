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

package org.jetbrains.kotlin.js.translate.expression

import com.google.dart.compiler.backend.js.ast.*
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.utils.JsDescriptorUtils.*

import kotlin.platform.platformStatic

private val METADATA_PROPERTIES_COUNT = 2

public class InlineMetadata(val tag: JsStringLiteral, val function: JsFunction) {
    companion object {
        platformStatic
        fun compose(function: JsFunction, descriptor: CallableDescriptor): InlineMetadata {
            val program = function.getScope().getProgram()
            val tag = program.getStringLiteral(Namer.getFunctionTag(descriptor))
            return InlineMetadata(tag, function)
        }

        platformStatic
        fun decompose(expression: JsExpression?): InlineMetadata? =
                when (expression) {
                    is JsBinaryOperation -> decomposeCommaExpression(expression)
                    is JsInvocation -> decomposeCreateFunctionCall(expression)
                    else -> null
                }

        private fun decomposeCreateFunctionCall(call: JsInvocation): InlineMetadata? {
            if (Namer.CREATE_INLINE_FUNCTION != call.getQualifier()) return null

            return decomposePropertiesList(call.getArguments())
        }

        private fun decomposeCommaExpression(expression: JsExpression): InlineMetadata? {
            val properties = arrayListOf<JsExpression>()
            var decomposable: JsExpression? = expression

            while (decomposable is JsExpression) {
                val binOp = decomposable as? JsBinaryOperation

                if (JsBinaryOperator.COMMA == binOp?.getOperator()) {
                    properties.add(binOp?.getArg2())
                    decomposable = binOp?.getArg1()
                } else {
                    properties.add(decomposable)
                    break
                }
            }

            return decomposePropertiesList(properties.reverse())
        }

        private fun decomposePropertiesList(properties: List<JsExpression>): InlineMetadata? {
            if (properties.size() != METADATA_PROPERTIES_COUNT) return null

            val tag = properties[0] as? JsStringLiteral
            val function = properties[1] as? JsFunction
            if (tag == null || function == null) return null

            return InlineMetadata(tag, function)
        }
    }

    public val functionWithMetadata: JsExpression
        get() {
            val propertiesList = listOf(tag, function)
            return JsInvocation(Namer.CREATE_INLINE_FUNCTION, propertiesList)
        }
}
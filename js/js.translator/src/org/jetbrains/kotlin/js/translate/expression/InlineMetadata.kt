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

private val METADATA_PROPERTIES_COUNT = 3

public class InlineMetadata(
        val startTag: JsStringLiteral,
        val function: JsFunction,
        val endTag: JsStringLiteral
) {
    class object {
        platformStatic
        fun compose(function: JsFunction, descriptor: CallableDescriptor): InlineMetadata {
            val program = function.getScope().getProgram()
            val startTag = program.getStringLiteral(Namer.getInlineStartTag(descriptor))
            val endTag = program.getStringLiteral(Namer.getInlineEndTag(descriptor))
            return InlineMetadata(startTag, function, endTag)
        }

        /**
         * Reads metadata from expression.
         *
         * To read metadata from source one needs to:
         * 1. find index of startTag and endTag in source;
         * 2. parse substring between startTagIndex - 1 (for opening quote)
         *    and endTagIndex + endTag.length() + 1 (for closing quote)
         * 3. call InlineMetadata#decompose on resulting expression
         *
         * @see Namer#getInlineStartTag
         * @see Namer#getInlineEndTag
         * @see com.google.gwt.dev.js.JsParser
         */
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

            val startTag = properties[0] as? JsStringLiteral
            val function = properties[1] as? JsFunction
            val endTag = properties[2] as? JsStringLiteral

            if (startTag == null || function == null || endTag == null) return null

            return InlineMetadata(startTag, function, endTag)
        }
    }

    public val functionWithMetadata: JsExpression
        get() {
            val propertiesList = listOf(startTag, function, endTag)
            return JsInvocation(Namer.CREATE_INLINE_FUNCTION, propertiesList)
        }
}
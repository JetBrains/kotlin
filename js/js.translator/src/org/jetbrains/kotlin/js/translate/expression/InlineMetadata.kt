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

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.translate.context.Namer

private val METADATA_PROPERTIES_COUNT = 2

class InlineMetadata(val tag: JsStringLiteral, val function: JsFunction) {
    companion object {
        @JvmStatic
        fun compose(function: JsFunction, descriptor: CallableDescriptor, config: JsConfig): InlineMetadata {
            val program = function.scope.program
            val tag = program.getStringLiteral(Namer.getFunctionTag(descriptor, config))
            return InlineMetadata(tag, function)
        }

        @JvmStatic
        fun decompose(expression: JsExpression?): InlineMetadata? =
                when (expression) {
                    is JsInvocation -> decomposeCreateFunctionCall(expression)
                    else -> null
                }

        private fun decomposeCreateFunctionCall(call: JsInvocation): InlineMetadata? {
            val qualifier = call.qualifier
            if (qualifier !is JsNameRef || qualifier.ident != Namer.DEFINE_INLINE_FUNCTION) return null

            val arguments = call.arguments
            if (arguments.size != METADATA_PROPERTIES_COUNT) return null

            val tag = arguments[0] as? JsStringLiteral
            val function = arguments[1] as? JsFunction
            if (tag == null || function == null) return null

            return InlineMetadata(tag, function)
        }
    }

    val functionWithMetadata: JsExpression
        get() {
            val propertiesList = listOf(tag, function)
            return JsInvocation(Namer.createInlineFunction(), propertiesList)
        }
}
/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.translate.declaration

import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.*
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils
import org.jetbrains.kotlin.js.translate.utils.name

fun <T : JsNode> transformCoroutineMetadataToSpecialFunctions(context: TranslationContext, node: T): T {
    val visitor = object : JsVisitorWithContextImpl() {
        override fun endVisit(x: JsNameRef, ctx: JsContext<in JsExpression>) {
            val specialFunction = when {
                x.coroutineController -> SpecialFunction.COROUTINE_CONTROLLER
                x.coroutineReceiver -> SpecialFunction.COROUTINE_RECEIVER
                x.coroutineResult -> SpecialFunction.COROUTINE_RESULT
                else -> null
            }
            if (specialFunction != null) {
                val arguments = listOfNotNull(x.qualifier).toTypedArray()
                ctx.replaceMe(TranslationUtils.invokeSpecialFunction(context, specialFunction, *arguments).source(x.source))
            }
            else {
                super.endVisit(x, ctx)
            }
        }

        override fun endVisit(x: JsExpression, ctx: JsContext<in JsExpression>) {
            if (x.isSuspend) {
                x.isSuspend = false
                ctx.replaceMe(TranslationUtils.invokeSpecialFunction(context, SpecialFunction.SUSPEND_CALL, x).source(x.source))
            }
        }
    }
    return visitor.accept(node)
}

fun <T : JsNode> transformSpecialFunctionsToCoroutineMetadata(node: T): T {
    val visitor = object : JsVisitorWithContextImpl() {
        override fun endVisit(x: JsInvocation, ctx: JsContext<in JsExpression>) {
            x.qualifier.name?.specialFunction?.let { specialFunction ->
                val replacement = when (specialFunction) {
                    SpecialFunction.COROUTINE_CONTROLLER -> {
                        JsNameRef("\$\$controller\$\$", x.arguments.getOrNull(0)).apply {
                            coroutineController = true
                        }
                    }
                    SpecialFunction.COROUTINE_RECEIVER -> {
                        JsNameRef("\$this\$", x.arguments.getOrNull(0)).apply {
                            coroutineReceiver = true
                        }
                    }
                    SpecialFunction.COROUTINE_RESULT -> {
                        JsNameRef("\$result\$", x.arguments.getOrNull(0)).apply {
                            coroutineResult = true
                        }
                    }
                    SpecialFunction.SUSPEND_CALL -> {
                        x.arguments[0].apply {
                            isSuspend = true
                        }
                    }
                    else -> null
                }
                replacement?.let { ctx.replaceMe(it) }
            }
        }
    }
    return visitor.accept(node)
}
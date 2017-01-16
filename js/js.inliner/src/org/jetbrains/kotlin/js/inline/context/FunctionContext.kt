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

package org.jetbrains.kotlin.js.inline.context

import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.descriptor
import org.jetbrains.kotlin.js.inline.FunctionReader
import org.jetbrains.kotlin.js.inline.util.*
import org.jetbrains.kotlin.js.translate.context.Namer

abstract class FunctionContext(
        private val function: JsFunction,
        private val functionReader: FunctionReader
) {
    protected abstract fun lookUpStaticFunction(functionName: JsName?): JsFunction?

    protected abstract fun lookUpStaticFunctionByTag(functionTag: String): JsFunction?

    fun getFunctionDefinition(call: JsInvocation): JsFunction {
        return getFunctionDefinitionImpl(call)!!
    }

    fun hasFunctionDefinition(call: JsInvocation): Boolean {
        return getFunctionDefinitionImpl(call) != null
    }

    fun getScope(): JsScope {
        return function.scope
    }

    /**
     * Gets function definition by invocation.
     *
     * Notes:
     *      1. Qualifier -- [()/.call()] part of invocation.
     *      2. Local functions are compiled like function literals,
     *      but called not directly, but through variable.
     *
     *      For example, local `fun f(a, b) = a + b; f(1, 2)` becomes `var f = _.foo.f$; f(1, 2)`
     *
     * Invocation properties:
     * 1. Ends with either [()/.call()].
     *
     * 2. Qualifier can be JsNameRef with static ref to JsFunction
     *    in case of function literal without closure.
     *
     *    For example, qualifier == _.foo.lambda$
     *
     * 3. Qualifier can be JsInvocation with static ref to JsFunction
     *    in case of function literal with closure. In this case
     *    qualifier arguments are captured in closure.
     *
     *    For example, qualifier == _.foo.lambda(captured_1)
     *
     * 4. Qualifier can be JsNameRef with static ref to case [2]
     *    in case of local function without closure.
     *
     * 5. Qualifier can be JsNameRef with ref to case [3]
     *    in case of local function with closure.
     */
    private fun getFunctionDefinitionImpl(call: JsInvocation): JsFunction? {
        val descriptor = call.descriptor
        if (descriptor != null) {
            if (descriptor in functionReader) return functionReader[descriptor]
            lookUpStaticFunctionByTag(Namer.getFunctionTag(descriptor))?.let { return it }
        }

        /** remove ending `()` */
        val callQualifier: JsExpression = if (isCallInvocation(call)) {
            (call.qualifier as JsNameRef).qualifier!!
        }
        else {
            call.qualifier
        }

        /** process cases 2, 3 */
        val qualifier = callQualifier.transitiveStaticRef
        return when (qualifier) {
            is JsInvocation -> {
                getSimpleName(qualifier)?.let { simpleName ->
                    lookUpStaticFunction(simpleName)?.let { if (isFunctionCreator(it)) it else null }
                }
            }
            is JsNameRef -> lookUpStaticFunction(qualifier.name)
            is JsFunction -> qualifier
            else -> null
        }
    }
}

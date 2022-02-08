/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin.inference

/**
 * Call bindings are the binding variables for either a function being inferred or a target of a
 * call. If a call and the function's call bindings can be unified the call is valid and the
 * variables bound by the unification contribute to the environment for the any subsequent bindings.
 *
 * @param target the binding variable for the call target.
 * @param parameters the call bindings the lambda parameters (if any).
 */
class CallBindings(
    val target: Binding,
    val parameters: List<CallBindings> = emptyList(),
    val result: CallBindings?,
    val anyParameters: Boolean
) {
    override fun toString(): String {
        val paramsString = if (parameters.isEmpty()) "" else ", ${
            parameters.joinToString(", ") { it.toString() }
        }"
        val anyParametersStr = if (anyParameters) "*" else ""
        val resultString = result?.let { "-> $it" } ?: ""
        return "[$target$anyParametersStr$paramsString$resultString]"
    }
}
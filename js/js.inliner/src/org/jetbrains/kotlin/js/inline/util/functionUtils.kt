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

package org.jetbrains.kotlin.js.inline.util

import org.jetbrains.kotlin.js.backend.ast.JsFunction
import org.jetbrains.kotlin.js.backend.ast.JsReturn

fun isFunctionCreator(outer: JsFunction): Boolean =
        outer.getInnerFunction() != null

/**
 * Gets inner function from function, that creates closure
 *
 * For example:
 * function(a) {
 *   return function() { return a; }
 * }
 *
 * Inner functions can only be generated when lambda
 * with closure is created
 */
fun JsFunction.getInnerFunction(): JsFunction? {
    val statements = body.statements
    if (statements.size != 1) return null

    val statement = statements.get(0)
    val returnExpr = (statement as? JsReturn)?.expression

    return returnExpr as? JsFunction
}

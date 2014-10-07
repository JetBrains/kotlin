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

package org.jetbrains.k2js.inline.util

import com.google.dart.compiler.backend.js.ast.JsFunction
import com.google.dart.compiler.backend.js.ast.JsReturn

public fun isFunctionCreator(outer: JsFunction): Boolean =
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
public fun JsFunction.getInnerFunction(): JsFunction? {
    val statements = getBody().getStatements()
    if (statements.size() != 1) return null

    val statement = statements.get(0)
    val returnExpr = (statement as? JsReturn)?.getExpression()

    return returnExpr as? JsFunction
}
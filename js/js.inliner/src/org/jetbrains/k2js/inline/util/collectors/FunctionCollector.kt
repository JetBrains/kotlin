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

package org.jetbrains.k2js.inline.util.collectors

import com.google.dart.compiler.backend.js.ast.RecursiveJsVisitor
import com.google.dart.compiler.backend.js.ast.JsPropertyInitializer
import com.google.dart.compiler.backend.js.ast.JsNameRef
import com.google.dart.compiler.backend.js.ast.JsFunction
import com.google.dart.compiler.backend.js.ast.JsName

import java.util.IdentityHashMap

class FunctionCollector : RecursiveJsVisitor() {
    public val functions: IdentityHashMap<JsName, JsFunction> = IdentityHashMap()

    override fun visitPropertyInitializer(x: JsPropertyInitializer?) {
        super.visitPropertyInitializer(x)

        val label = x?.getLabelExpr()
        val value = x?.getValueExpr()

        if (label is JsNameRef && value is JsFunction) {
            val name = (label as JsNameRef).getName()
            val function = value as JsFunction

            if (name != null) {
                functions[name] = function
            }
        }
    }
}
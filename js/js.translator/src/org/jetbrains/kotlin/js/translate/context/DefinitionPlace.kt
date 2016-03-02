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

package org.jetbrains.kotlin.js.translate.context

import com.google.dart.compiler.backend.js.ast.*
import com.google.dart.compiler.backend.js.ast.metadata.staticRef

class DefinitionPlace(
        private val scope: JsObjectScope,
        private val fqName: JsExpression,
        val properties: MutableList<JsPropertyInitializer>
) {
    fun define(suggestedName: String, expression : JsExpression) =  define(scope.declareFreshName(suggestedName), expression)

    fun define(name: JsName, expression: JsExpression): JsNameRef {
        if (expression is JsFunction) {
            /** JsInliner should be able
             * to find function by name */
            name.staticRef = expression
        }

        properties.add(JsPropertyInitializer(name.makeRef(), expression))
        return JsNameRef(name, fqName)
    }
}

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

package org.jetbrains.kotlin.js.translate.utils.ast

import com.google.dart.compiler.backend.js.ast.*
import org.jetbrains.kotlin.js.inline.util.IdentitySet

public fun JsFunction.addStatement(stmt: JsStatement) {
    getBody().getStatements().add(stmt)
}

public fun JsFunction.addParameter(identifier: String, index: Int? = null): JsParameter {
    val name = getScope().declareFreshName(identifier)
    val parameter = JsParameter(name)

    if (index == null) {
        getParameters().add(parameter)
    } else {
        getParameters().add(index, parameter)
    }

    return parameter
}

/**
 * Tests, if any node containing in receiver's AST matches, [predicate].
 */
public fun JsNode.any(predicate: (JsNode) -> Boolean): Boolean {
    val visitor = object : RecursiveJsVisitor() {
        public var matched: Boolean = false

        override fun visitElement(node: JsNode) {
            matched = matched || predicate(node)

            if (!matched) {
                super.visitElement(node)
            }
        }
    }

    visitor.accept(this)
    return visitor.matched
}

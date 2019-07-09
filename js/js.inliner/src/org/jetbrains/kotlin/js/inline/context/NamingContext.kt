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

package org.jetbrains.kotlin.js.inline.context

import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.synthetic
import org.jetbrains.kotlin.js.inline.util.replaceNames
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils

class NamingContext(private val previousStatements: MutableList<JsStatement>) {
    private val renamings = mutableMapOf<JsName, JsNameRef>()
    private val declarations = mutableListOf<JsVars>()
    private var addedDeclarations = false

    fun applyRenameTo(target: JsNode): JsNode {
        if (!addedDeclarations) {
            previousStatements.addAll(declarations)
            addedDeclarations = true
        }

        return replaceNames(target, renamings)
    }

    fun replaceName(name: JsName, replacement: JsNameRef) {
        assert(!renamings.containsKey(name)) { "$name has been renamed already" }

        renamings.put(name, replacement)
    }

    fun newVar(name: JsName, value: JsExpression? = null, source: Any?) {
        val vars = JsAstUtils.newVar(name, value)
        vars.synthetic = true
        vars.source = source
        declarations.add(vars)
    }
}

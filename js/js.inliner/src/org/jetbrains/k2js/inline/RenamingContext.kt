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

package org.jetbrains.k2js.inline

import com.google.dart.compiler.backend.js.ast.*
import com.google.dart.compiler.backend.js.ast.JsVars.JsVar

import java.util.ArrayList
import java.util.IdentityHashMap

import org.jetbrains.k2js.translate.utils.JsAstUtils


data class RenamingResult<T : JsNode>(val renamed: T, val declarations: Collection<JsVars>)

class RenamingContext<T : JsNode>(scope: JsScope) {
    private val scope = scope
    private val renamings = IdentityHashMap<JsName, JsExpression>()
    private val declarations = ArrayList<JsVars>()

    public fun applyRename(target : T): RenamingResult<T> {
        val renamed = RenamingVisitor.rename(target, renamings)
        return RenamingResult(renamed, declarations)
    }

    public fun replaceName(name: JsName, replacement: JsExpression) {
        assert(!renamings.containsKey(name)) { "$name has been renamed already" }

        renamings.put(name, replacement)
    }

    public fun getFreshName(candidate: String?): JsName = scope.declareFreshName(candidate)

    public fun getFreshName(candidate: JsName?): JsName = getFreshName(candidate?.getIdent())

    public fun newVar(name: JsName, value: JsExpression? = null) {
        val vars = JsAstUtils.newVar(name, value)
        declarations.add(vars)
    }
}

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

import com.google.dart.compiler.backend.js.ast.JsScope
import com.google.dart.compiler.backend.js.ast.RecursiveJsVisitor
import com.google.dart.compiler.backend.js.ast.JsFunction
import com.google.dart.compiler.backend.js.ast.JsLabel
import com.google.dart.compiler.backend.js.ast.HasName
import com.google.dart.compiler.backend.js.ast.JsName
import com.google.dart.compiler.backend.js.ast.JsVars

import java.util.HashMap

class NameCollector(private val scope: JsScope) : RecursiveJsVisitor() {
    public val names: MutableMap<String, JsName> = HashMap()

    override fun visit(x: JsVars.JsVar?) {
        super.visit(x)
        addNameIfNeeded(x)
    }

    override fun visitFunction(x: JsFunction?) { }

    private fun addNameIfNeeded(hasName: HasName?) {
        val name = hasName?.getName()
        val ident = name?.getIdent()

        if (name == null || ident == null) return

        val nameCollected = names.get(ident)
        assert(nameCollected == null || nameCollected identityEquals name) { "ambiguous identifier for $hasName" }
        assert(scope.hasOwnName(ident)) { "non-local name was added $hasName" }

        names.put(ident, name)
    }
}
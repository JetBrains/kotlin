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

package org.jetbrains.k2js.inline.util.rewriters

import com.google.dart.compiler.backend.js.ast.JsContext
import com.google.dart.compiler.backend.js.ast.JsExpression
import com.google.dart.compiler.backend.js.ast.JsFunction
import com.google.dart.compiler.backend.js.ast.JsLiteral
import com.google.dart.compiler.backend.js.ast.JsObjectLiteral
import com.google.dart.compiler.backend.js.ast.JsVisitorWithContextImpl

class ThisReplacingVisitor(private val thisReplacement: JsExpression) : JsVisitorWithContextImpl() {
    override fun endVisit(x: JsLiteral.JsThisRef?, ctx: JsContext?) {
        ctx?.replaceMe(thisReplacement)
    }

    override fun visit(x: JsFunction?, ctx: JsContext?) = false

    override fun visit(x: JsObjectLiteral?, ctx: JsContext?) = false
}
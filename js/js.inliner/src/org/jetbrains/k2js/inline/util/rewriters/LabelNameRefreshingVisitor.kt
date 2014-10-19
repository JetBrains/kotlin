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

import com.google.dart.compiler.backend.js.ast.JsVisitorWithContextImpl
import com.google.dart.compiler.backend.js.ast.JsFunctionScope
import org.jetbrains.k2js.inline.context.NamingContext
import com.google.dart.compiler.backend.js.ast.JsFunction
import com.google.dart.compiler.backend.js.ast.JsContext
import com.google.dart.compiler.backend.js.ast.JsLabel
import com.google.dart.compiler.backend.js.ast.JsContinue
import com.google.dart.compiler.backend.js.ast.JsBreak

class LabelNameRefreshingVisitor(val context: NamingContext, val functionScope: JsFunctionScope) : JsVisitorWithContextImpl() {
    override fun visit(x: JsFunction?, ctx: JsContext?): Boolean = false

    override fun visit(x: JsLabel?, ctx: JsContext?): Boolean {
        val labelName = x!!.getName()
        val freshName = functionScope.enterLabel(labelName.getIdent())

        if (freshName.getIdent() != labelName.getIdent()) {
            context.replaceName(labelName, freshName.makeRef())
        }

        return super.visit(x, ctx)
    }

    override fun endVisit(x: JsLabel?, ctx: JsContext?) {
        super.endVisit(x, ctx)
        functionScope.exitLabel()
    }
}
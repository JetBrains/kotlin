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

package org.jetbrains.kotlin.js.translate.utils

import com.google.dart.compiler.backend.js.ast.*
import com.google.dart.compiler.backend.js.ast.metadata.TypeCheck
import com.google.dart.compiler.backend.js.ast.metadata.typeCheck
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils.*

public fun expandIsCalls(node: JsNode, context: TranslationContext) {
    val visitor = object : JsVisitorWithContextImpl() {
        override fun visit(x: JsInvocation, ctx: JsContext<*>): Boolean {
            val callee = x.getQualifier() as? JsInvocation
            val instance = x.getArguments().firstOrNull()
            val type = callee?.getArguments()?.firstOrNull()

            val replacement = when (callee?.typeCheck) {
                TypeCheck.TYPEOF -> typeOfIs(instance!!, type as JsStringLiteral)
                TypeCheck.INSTANCEOF -> context.namer().isInstanceOf(instance!!, type!!)
                else -> null
            }

            if (replacement != null) {
                ctx.replaceMe(replacement)
                return false
            }

            return super.visit(x, ctx)
        }
    }

    visitor.accept(node)
}

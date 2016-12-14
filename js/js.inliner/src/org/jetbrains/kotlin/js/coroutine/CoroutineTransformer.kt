/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.coroutine

import com.google.dart.compiler.backend.js.ast.*
import com.google.dart.compiler.backend.js.ast.metadata.coroutineType

class CoroutineTransformer(private val program: JsProgram) : JsVisitorWithContextImpl() {
    private val additionalStatements = mutableListOf<JsStatement>()

    override fun endVisit(x: JsExpressionStatement, ctx: JsContext<in JsStatement>) {
        additionalStatements.forEach { ctx.addNext(it) }
        additionalStatements.clear()
        super.endVisit(x, ctx)
    }

    override fun endVisit(x: JsFunction, ctx: JsContext<in JsStatement>) {
        val coroutineType = x.coroutineType
        if (coroutineType != null) {
            additionalStatements += CoroutineFunctionTransformer(program, x).transform()
        }
    }
}

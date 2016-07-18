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

package org.jetbrains.kotlin.js.test.utils

import com.google.dart.compiler.backend.js.ast.JsExpressionStatement
import com.google.dart.compiler.backend.js.ast.JsNullLiteral
import com.google.dart.compiler.backend.js.ast.JsProgram
import com.google.dart.compiler.backend.js.ast.RecursiveJsVisitor
import org.junit.Assert

fun JsProgram.verifyAst() {
    accept(object : RecursiveJsVisitor() {
        override fun visitExpressionStatement(x: JsExpressionStatement) {
            val expression = x.expression
            if (expression is JsNullLiteral) {
                Assert.fail("Expression statement contains `null` literal")
            }
            else {
                super.visitExpressionStatement(x)
            }
        }
    })
}
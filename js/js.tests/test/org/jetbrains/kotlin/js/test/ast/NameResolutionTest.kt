/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.test.ast

import com.google.gwt.dev.js.rhino.CodePosition
import com.google.gwt.dev.js.rhino.ErrorReporter
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.inline.clean.resolveTemporaryNames
import org.jetbrains.kotlin.js.parser.parse
import org.jetbrains.kotlin.js.test.BasicBoxTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import java.io.File

class NameResolutionTest {
    @Rule
    @JvmField
    var testName = TestName()

    @Test
    fun simple() = doTest()

    @Test
    fun reuseName() = doTest()

    @Test
    fun globalName() = doTest()

    @Test
    fun labels() = doTest()

    private fun doTest() {
        val methodName = testName.methodName
        val baseName = "${BasicBoxTest.TEST_DATA_DIR_PATH}/js-name-resolution/"
        val originalName = "$baseName/$methodName.original.js"
        val expectedName = "$baseName/$methodName.expected.js"

        val originalCode = FileUtil.loadFile(File(originalName))
        val expectedCode = FileUtil.loadFile(File(expectedName))

        val parserScope = JsFunctionScope(JsRootScope(JsProgram()), "<js fun>")
        val originalAst = JsGlobalBlock().apply { statements += parse(originalCode, errorReporter, parserScope, originalName) }
        val expectedAst = JsGlobalBlock().apply { statements += parse(expectedCode, errorReporter, parserScope, expectedName) }

        originalAst.accept(object : RecursiveJsVisitor() {
            val cache = mutableMapOf<JsName, JsName>()

            override fun visitElement(node: JsNode) {
                super.visitElement(node)
                if (node is HasName) {
                    node.name = node.name?.let { name ->
                        if (name.ident.startsWith("$")) {
                            cache.getOrPut(name) { JsScope.declareTemporaryName("x") }
                        }
                        else {
                            name
                        }
                    }
                }
            }
        })
        originalAst.resolveTemporaryNames()

        assertEquals(expectedAst.toString(), originalAst.toString())
    }

    private val errorReporter = object : ErrorReporter {
        override fun warning(message: String, startPosition: CodePosition, endPosition: CodePosition) { }

        override fun error(message: String, startPosition: CodePosition, endPosition: CodePosition) {
            fail("Error parsing JS file: $message at $startPosition")
        }
    }
}

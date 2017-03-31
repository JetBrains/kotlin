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

package org.jetbrains.kotlin.js.test.optimizer


import com.google.gwt.dev.js.rhino.CodePosition
import com.google.gwt.dev.js.rhino.ErrorReporter
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.synthetic
import org.jetbrains.kotlin.js.inline.clean.FunctionPostProcessor
import org.jetbrains.kotlin.js.parser.parse
import org.jetbrains.kotlin.js.sourceMap.JsSourceGenerationVisitor
import org.jetbrains.kotlin.js.test.BasicBoxTest
import org.jetbrains.kotlin.js.test.createScriptEngine
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.js.util.TextOutputImpl
import org.junit.Assert
import org.junit.Rule
import org.junit.rules.TestName
import java.io.File

abstract class BasicOptimizerTest(private var basePath: String) {
    @Rule
    @JvmField
    var testName = TestName()

    protected fun box() {
        val methodName = testName.methodName
        val baseName = "${BasicBoxTest.TEST_DATA_DIR_PATH}/js-optimizer/$basePath"
        val unoptimizedName = "$baseName/$methodName.original.js"
        val optimizedName = "$baseName/$methodName.optimized.js"

        val unoptimizedCode = FileUtil.loadFile(File(unoptimizedName))
        val optimizedCode = FileUtil.loadFile(File(optimizedName))

        runScript(unoptimizedName, unoptimizedCode)
        runScript(optimizedName, optimizedCode)
        checkOptimizer(unoptimizedCode, optimizedCode)
    }

    private fun checkOptimizer(unoptimizedCode: String, optimizedCode: String) {
        val parserScope = JsFunctionScope(JsRootScope(JsProgram()), "<js fun>")
        val unoptimizedAst = parse(unoptimizedCode, errorReporter, parserScope, "<unknown file>")

        updateMetadata(unoptimizedCode, unoptimizedAst)

        for (statement in unoptimizedAst) {
            process(statement)
        }

        val optimizedAst = parse(optimizedCode, errorReporter, parserScope, "<unknown file>")
        Assert.assertEquals(astToString(optimizedAst), astToString(unoptimizedAst))
    }

    protected open fun process(statement: JsStatement) {
        object : RecursiveJsVisitor() {
            override fun visitFunction(x: JsFunction) {
                FunctionPostProcessor(x).apply()
                super.visitFunction(x)
            }
        }.accept(statement)
    }

    private fun updateMetadata(code: String, ast: List<JsStatement>) {
        val comments = findSyntheticComments(code)

        for (stmt in ast) {
            object : RecursiveJsVisitor() {
                override fun visitVars(x: JsVars) {
                    x.synthetic = x.vars.any { isSyntheticId(it.name.ident) }
                    super.visitVars(x)
                }

                override fun visitExpressionStatement(x: JsExpressionStatement) {
                    val assignment = JsAstUtils.decomposeAssignmentToVariable(x.expression)
                    if (assignment != null) {
                        val name = assignment.first
                        x.synthetic = isSyntheticId(name.ident)
                    }
                    super.visitExpressionStatement(x)
                }

                override fun visitIf(x: JsIf) {
                    val line = x.getData<Int?>("line")
                    if (line != null && line in comments.indices && comments[line]) {
                        x.synthetic = true
                    }
                    super.visitIf(x)
                }

                override fun visitLabel(x: JsLabel) {
                    x.synthetic = isSyntheticId(x.name.ident)
                    super.visitLabel(x)
                }
            }.accept(stmt)
        }
    }

    private fun findSyntheticComments(code: String): List<Boolean> {
        val parts = code.lines()
        return parts.map { it.contains("/*synthetic*/") }
    }

    private fun isSyntheticId(id: String) = id.startsWith("$")

    private fun astToString(ast: List<JsStatement>): String {
        val output = TextOutputImpl()
        val visitor = JsSourceGenerationVisitor(output, null)
        for (stmt in ast) {
            stmt.accept(visitor)
        }
        return output.toString()
    }

    private fun runScript(fileName: String, code: String) {
        val engine = createScriptEngine()
        engine.eval(code)
        val result = engine.eval("box()")

        Assert.assertEquals("$fileName: box() function must return 'OK'", "OK", result)
    }

    private val errorReporter = object : ErrorReporter {
        override fun warning(message: String, startPosition: CodePosition, endPosition: CodePosition) { }

        override fun error(message: String, startPosition: CodePosition, endPosition: CodePosition) {
            Assert.fail("Error parsing JS file: $message at $startPosition")
        }
    }
}

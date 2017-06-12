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

package org.jetbrains.kotlin.js.test.utils

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.js.backend.ast.*

class LineCollector : RecursiveJsVisitor() {
    val lines = mutableListOf<Int?>()
    private var currentStatement: JsStatement? = null
    val lineNumbersByStatement = mutableMapOf<JsStatement, MutableList<Int>>()
    val statementsWithoutLineNumbers = mutableSetOf<JsStatement>()

    override fun visitElement(node: JsNode) {
        handleNodeLocation(node)
        super.visitElement(node)
    }

    private fun handleNodeLocation(node: JsNode) {
        val source = node.source
        val line = when (source) {
            is PsiElement -> {
                val file = source.containingFile
                val offset = source.node.startOffset
                val document = file.viewProvider.document!!
                document.getLineNumber(offset)
            }
            is JsLocation -> {
                source.startLine
            }
            else -> null
        }

        if (line != null) {
            currentStatement?.let {
                val linesByStatement = lineNumbersByStatement.getOrPut(it, ::mutableListOf)
                if (linesByStatement.lastOrNull() != line) {
                    linesByStatement += line
                    lines += line
                }
            }
        }
    }

    override fun visitExpressionStatement(x: JsExpressionStatement) {
        val expression = x.expression
        if (expression is JsFunction) {
            expression.accept(this)
        }
        else {
            withStatement(x) {
                super.visitExpressionStatement(x)
            }
        }
    }

    override fun visitIf(x: JsIf) {
        withStatement(x) {
            handleNodeLocation(x)
            x.ifExpression.accept(this)
        }
        x.thenStatement.accept(this)
        x.elseStatement?.accept(this)
    }

    override fun visitWhile(x: JsWhile) {
        withStatement(x) {
            handleNodeLocation(x)
            x.condition.accept(this)
        }
        x.body.accept(this)
    }

    override fun visitDoWhile(x: JsDoWhile) {
        withStatement(x) {
            handleNodeLocation(x)
            x.condition.accept(this)
        }
        x.body.accept(this)
    }

    override fun visitFor(x: JsFor) {
        withStatement(x) {
            x.initExpression?.accept(this)
            x.initVars?.accept(this)
            x.condition?.accept(this)
            x.incrementExpression?.accept(this)
        }
        x.body?.accept(this)
    }

    override fun visitBreak(x: JsBreak) {
        withStatement(x) {
            super.visitBreak(x)
        }
    }

    override fun visitContinue(x: JsContinue) {
        withStatement(x) {
            super.visitContinue(x)
        }
    }

    override fun visitReturn(x: JsReturn) {
        withStatement(x) {
            super.visitReturn(x)
        }
    }

    override fun visitVars(x: JsVars) {
        withStatement(x) {
            super.visitVars(x)
        }
    }

    override fun visit(x: JsSwitch) {
        withStatement(x) {
            x.expression.accept(this)
        }
        x.cases.forEach { accept(it) }
    }

    override fun visitThrow(x: JsThrow) {
        withStatement(x) {
            super.visitThrow(x)
        }
    }

    private fun withStatement(statement: JsStatement, action: () -> Unit) {
        val oldStatement = currentStatement
        currentStatement = statement

        action()

        if (statement !in lineNumbersByStatement && lines.lastOrNull() != null) {
            lines.add(null)
            statementsWithoutLineNumbers += statement
        }
        currentStatement = oldStatement
    }
}
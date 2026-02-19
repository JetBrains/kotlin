/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.utils

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.resolve.calls.util.isFakePsiElement

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
                if (!source.isFakePsiElement) {
                    val file = source.containingFile
                    val offset = source.node.startOffset
                    val document = file.viewProvider.document!!
                    document.getLineNumber(offset)
                } else null
            }
            is JsLocationWithSource -> {
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
        withStatement(x) {
            handleNodeLocation(x.expression)
            lineNumbersByStatement[x]?.add(-1)
            x.expression.acceptChildren(this)
        }
    }

    override fun visitIf(x: JsIf) {
        withStatement(x) {
            handleNodeLocation(x)
            lineNumbersByStatement[x]?.add(-1)
            x.ifExpression.accept(this)
        }
        x.thenStatement.accept(this)
        x.elseStatement?.accept(this)
    }

    override fun visitWhile(x: JsWhile) {
        withStatement(x) {
            handleNodeLocation(x)
            lineNumbersByStatement[x]?.add(-1)
            x.condition.accept(this)
        }
        x.body.accept(this)
    }

    override fun visitDoWhile(x: JsDoWhile) {
        withStatement(x) {
            x.body.accept(this)
            x.condition.accept(this)
        }
    }

    override fun visitFor(x: JsFor) {
        withStatement(x) {
            handleNodeLocation(x)
            lineNumbersByStatement[x]?.add(-1)
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
            handleNodeLocation(x)
            lineNumbersByStatement[x]?.add(-1)
            super.visitReturn(x)
        }
    }

    override fun visitVars(x: JsVars) {
        withStatement(x) {
            handleNodeLocation(x)
            lineNumbersByStatement[x]?.add(-1)
            super.visitVars(x)
        }
    }

    override fun visit(x: JsSwitch) {
        withStatement(x) {
            handleNodeLocation(x)
            lineNumbersByStatement[x]?.add(-1)
            x.expression.accept(this)
            x.cases.forEach { accept(it) }
        }
    }

    override fun visitThrow(x: JsThrow) {
        withStatement(x) {
            handleNodeLocation(x)
            lineNumbersByStatement[x]?.add(-1)
            super.visitThrow(x)
        }
    }

    override fun visitTry(x: JsTry) {
        withStatement(x) {
            x.tryBlock.acceptChildren(this)
            x.catches?.forEach { accept(it) }
            x.finallyBlock?.acceptChildren(this)
        }
    }


    override fun visitSingleLineComment(comment: JsSingleLineComment) {
        withStatement(comment) {
            lineNumbersByStatement[comment]?.add(-1)
        }
    }

    override fun visitMultiLineComment(comment: JsMultiLineComment) {
        withStatement(comment) {
            lineNumbersByStatement[comment]?.add(-1)
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

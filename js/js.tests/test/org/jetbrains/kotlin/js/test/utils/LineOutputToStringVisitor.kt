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

import org.jetbrains.kotlin.js.backend.JsToStringGenerationVisitor
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.util.TextOutput

class LineOutputToStringVisitor(output: TextOutput, val lineCollector: LineCollector) :
        JsToStringGenerationVisitor(output) {

    override fun visitIf(x: JsIf) {
        printLineNumbers(x)
        super.visitIf(x)
    }

    override fun visitExpressionStatement(x: JsExpressionStatement) {
        printLineNumbers(x)
        super.visitExpressionStatement(x)
    }

    override fun visitWhile(x: JsWhile) {
        printLineNumbers(x)
        super.visitWhile(x)
    }

    override fun visitDoWhile(x: JsDoWhile) {
        printLineNumbers(x)
        super.visitDoWhile(x)
    }

    override fun visitBreak(x: JsBreak) {
        printLineNumbers(x)
        super.visitBreak(x)
    }

    override fun visitContinue(x: JsContinue) {
        printLineNumbers(x)
        super.visitContinue(x)
    }

    override fun visitFor(x: JsFor) {
        printLineNumbers(x)
        super.visitFor(x)
    }

    override fun visitVars(x: JsVars) {
        printLineNumbers(x)
        super.visitVars(x)
    }

    override fun visitThrow(x: JsThrow) {
        printLineNumbers(x)
        super.visitThrow(x)
    }

    override fun visitReturn(x: JsReturn) {
        printLineNumbers(x)
        super.visitReturn(x)
    }

    override fun visitTry(x: JsTry) {
        printLineNumbers(x)
        super.visitTry(x)
    }

    override fun visit(x: JsSwitch) {
        printLineNumbers(x)
        super.visit(x)
    }

    private fun printLineNumbers(statement: JsStatement) {
        if (statement in lineCollector.statementsWithoutLineNumbers) {
            p.print("/* no-line */ ")
        }
        else if (statement in lineCollector.lineNumbersByStatement) {
            p.print("/* ")
            p.print(lineCollector.lineNumbersByStatement[statement]!!.filter { it >= 0 }.joinToString(" ") { (it + 1).toString() })
            p.print(" */ ")
        }
    }
}
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

package org.jetbrains.kotlin.js.inline.clean

import com.google.dart.compiler.backend.js.ast.*
import com.google.dart.compiler.backend.js.ast.metadata.SideEffectKind
import com.google.dart.compiler.backend.js.ast.metadata.sideEffects
import com.google.dart.compiler.backend.js.ast.metadata.synthetic
import org.jetbrains.kotlin.js.inline.util.collectFreeVariables
import org.jetbrains.kotlin.js.inline.util.collectLocalVariables
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.js.translate.utils.splitToRanges

internal class TemporaryVariableElimination(function: JsFunction) {
    private val root = function.body
    private val definitions = mutableMapOf<JsName, Int>()
    private val usages = mutableMapOf<JsName, Int>()
    private val inconsistent = mutableSetOf<JsName>()
    private val definedValues = mutableMapOf<JsName, JsExpression>()
    private val temporary = mutableSetOf<JsName>()
    private var hasChanges = false
    private val namesToProcess = function.collectLocalVariables()
    private val statementsToRemove = mutableSetOf<JsNode>()
    private val namesToSubstitute = mutableSetOf<JsName>()
    private val variablesToRemove = mutableSetOf<JsName>()

    fun apply(): Boolean {
        analyze()
        perform()
        cleanUp()
        return hasChanges
    }

    private fun analyze() {
        object : RecursiveJsVisitor() {
            val currentScope = mutableSetOf<JsName>()
            var localVars = mutableSetOf<JsName>()

            override fun visitExpressionStatement(x: JsExpressionStatement) {
                val assignment = JsAstUtils.decomposeAssignmentToVariable(x.expression)
                if (assignment != null) {
                    val (name, value) = assignment
                    if (name in namesToProcess) {
                        assignVariable(name, value)
                        addVar(name)
                        accept(value)
                        if (x.synthetic) {
                            temporary += name
                        }
                    }
                    return
                }
                super.visitExpressionStatement(x)
            }

            override fun visitVars(x: JsVars) {
                for (v in x.vars) {
                    val name = v.name
                    val value = v.initExpression
                    if (value != null && name in namesToProcess) {
                        assignVariable(name, value)
                        addVar(name)
                        accept(value)
                        if (x.synthetic) {
                            temporary += name
                        }
                    }
                }
            }

            override fun visitNameRef(nameRef: JsNameRef) {
                val name = nameRef.name
                if (name != null && nameRef.qualifier == null && name in namesToProcess) {
                    useVariable(name)
                    if (name !in currentScope) {
                        inconsistent += name
                    }
                    return
                }
                super.visitNameRef(nameRef)
            }

            override fun visitBreak(x: JsBreak) { }

            override fun visitContinue(x: JsContinue) { }

            override fun visitObjectLiteral(x: JsObjectLiteral) {
                for (initializer in x.propertyInitializers) {
                    accept(initializer.valueExpr)
                }
            }

            override fun visitFor(x: JsFor) = withNewScope { super.visitFor(x) }

            override fun visitForIn(x: JsForIn) = withNewScope { super.visitForIn(x) }

            override fun visitWhile(x: JsWhile) = withNewScope { super.visitWhile(x) }

            override fun visitDoWhile(x: JsDoWhile) = withNewScope { super.visitDoWhile(x) }

            override fun visitIf(x: JsIf) {
                accept(x.ifExpression)
                withNewScope { accept(x.thenStatement) }
                x.elseStatement?.let { withNewScope { accept(it) } }
            }

            override fun visitCase(x: JsCase) = withNewScope { super.visitCase(x) }

            override fun visitDefault(x: JsDefault) = withNewScope { super.visitDefault(x) }

            override fun visitCatch(x: JsCatch) = withNewScope { super.visitCatch(x) }

            override fun visitFunction(x: JsFunction) {
                for (freeVar in x.collectFreeVariables()) {
                    useVariable(freeVar)
                    useVariable(freeVar)
                }
            }

            private inline fun <T> withNewScope(block: () -> T): T {
                val localVarsBackup = localVars
                try {
                    localVars = mutableSetOf()
                    return block()
                }
                finally {
                    currentScope -= localVars
                    localVars = localVarsBackup
                }
            }

            private fun addVar(name: JsName) {
                currentScope += name
                localVars.add(name)
            }
        }.accept(root)
    }

    private fun perform() {
        object : RecursiveJsVisitor() {
            val lastAssignedVars = mutableListOf<Pair<JsName, JsNode>>()
            var lastProperlyUsedIndex = -1
            var firstUsedIndex = 0
            var nextExpectedIndex = -1
            var sideEffectOccurred = false

            override fun visitNameRef(nameRef: JsNameRef) {
                val name = nameRef.name
                if (name != null && nameRef.qualifier == null && shouldConsiderTemporary(name)) {
                    val index = lastAssignedVars.indexOfFirst { it.first == name }
                    if (index >= 0) {
                        if (sideEffectOccurred) {
                            lastProperlyUsedIndex = -1
                        }
                        else {
                            if (index == nextExpectedIndex) {
                                ++nextExpectedIndex
                            }
                            else {
                                lastProperlyUsedIndex = index
                                nextExpectedIndex = index + 1
                            }
                        }
                        firstUsedIndex = Math.min(firstUsedIndex, index)
                    }
                    return
                }

                super.visitNameRef(nameRef)
                if (nameRef.qualifier != null && nameRef.sideEffects == SideEffectKind.AFFECTS_STATE) {
                    sideEffectOccurred = true
                }
            }

            override fun visitInvocation(invocation: JsInvocation) {
                super.visitInvocation(invocation)
                if (invocation.sideEffects == SideEffectKind.AFFECTS_STATE) {
                    sideEffectOccurred = true
                }
            }

            override fun visitNew(x: JsNew) {
                super.visitNew(x)
                sideEffectOccurred = true
            }

            override fun visitPrefixOperation(x: JsPrefixOperation) {
                super.visitPrefixOperation(x)
                when (x.operator) {
                    JsUnaryOperator.INC, JsUnaryOperator.DEC -> sideEffectOccurred = true
                    else -> {}
                }
            }

            override fun visitPostfixOperation(x: JsPostfixOperation) {
                super.visitPostfixOperation(x)
                when (x.operator) {
                    JsUnaryOperator.INC, JsUnaryOperator.DEC -> sideEffectOccurred = true
                    else -> {}
                }
            }

            override fun visitBinaryExpression(x: JsBinaryOperation) {
                if (x.operator == JsBinaryOperator.ASG) {
                    val left = x.arg1
                    val right = x.arg2

                    if (left is JsNameRef) {
                        val qualifier = left.qualifier
                        if (qualifier != null) {
                            accept(qualifier)
                        }
                    }
                    else if (left is JsArrayAccess) {
                        accept(left.arrayExpression)
                        accept(left.indexExpression)
                    }
                    else {
                        accept(left)
                    }

                    accept(right)
                    sideEffectOccurred = true
                }
                else {
                    super.visitBinaryExpression(x)
                }
            }

            override fun visitArrayAccess(x: JsArrayAccess) {
                super.visitArrayAccess(x)
                if (x.sideEffects == SideEffectKind.AFFECTS_STATE) {
                    sideEffectOccurred = true
                }
            }

            override fun visitArray(x: JsArrayLiteral) {
                super.visitArray(x)
                sideEffectOccurred = true
            }

            override fun visitExpressionStatement(x: JsExpressionStatement) {
                val expression = x.expression
                val assignment = JsAstUtils.decomposeAssignmentToVariable(expression)
                if (assignment != null) {
                    val (name, value) = assignment
                    if (shouldConsiderTemporary(name)) {
                        handleTopLevel(value, false)
                        if (isTrivial(value) && name !in inconsistent) {
                            statementsToRemove += x
                            namesToSubstitute += name
                        }
                        else {
                            lastAssignedVars.clear()
                            lastAssignedVars += Pair(name, x)
                            sideEffectOccurred = false
                        }
                        return
                    }
                    else if (shouldConsiderUnused(name)) {
                        variablesToRemove += name
                        handleTopLevel(value)
                        return
                    }
                }

                handleTopLevel(expression)
            }

            override fun visitIf(x: JsIf) {
                handleTopLevel(x.ifExpression)
                flush()
                accept(x.thenStatement)
                flush()
                x.elseStatement?.let { accept(it); flush() }
            }

            override fun visitConditional(x: JsConditional) {
                accept(x.testExpression)

                val lastAssignedVarsBackup = lastAssignedVars.toMutableList()
                lastAssignedVars.clear()
                accept(x.thenExpression)
                accept(x.elseExpression)
                lastAssignedVars.clear()
                lastAssignedVars += lastAssignedVarsBackup

                sideEffectOccurred = true
            }

            override fun visitReturn(x: JsReturn) {
                x.expression?.let { handleTopLevel(it) }
                flush()
            }

            override fun visitThrow(x: JsThrow) {
                handleTopLevel(x.expression)
                flush()
            }

            override fun visit(x: JsSwitch) {
                handleTopLevel(x.expression)
                flush()
                x.cases.forEach { accept(it); flush() }
            }

            override fun visitObjectLiteral(x: JsObjectLiteral) {
                for (initializer in x.propertyInitializers) {
                    accept(initializer.valueExpr)
                }
            }

            override fun visitWhile(x: JsWhile) {
                flush()
                super.visitWhile(x)
                flush()
            }

            override fun visitDoWhile(x: JsDoWhile) {
                flush()
                super.visitDoWhile(x)
                flush()
            }

            override fun visitForIn(x: JsForIn) {
                handleTopLevel(x.objectExpression)
                flush()
                accept(x.body)
                flush()
            }

            override fun visitFor(x: JsFor) {
                x.initVars?.let { accept(it) }
                x.initExpression?.let { handleTopLevel(it) }

                flush()

                x.condition?.let { accept(it) }
                x.incrementExpression?.let { accept(it) }

                flush()
                accept(x.body)
                flush()
            }

            override fun visitTry(x: JsTry) {
                flush()
                super.visitTry(x)
                flush()
            }

            override fun visitCatch(x: JsCatch) {
                flush()
                super.visitCatch(x)
                flush()
            }

            override fun visitLabel(x: JsLabel) {
                flush()
                super.visitLabel(x)
                flush()
            }

            override fun visitVars(x: JsVars) {
                for (v in x.vars) {
                    val initializer = v.initExpression
                    if (initializer != null) {
                        val name = v.name
                        if (shouldConsiderTemporary(name)) {
                            handleTopLevel(initializer, false)
                            if (isTrivial(initializer) && name !in inconsistent) {
                                statementsToRemove += v
                                namesToSubstitute += name
                            }
                            else {
                                lastAssignedVars.clear()
                                lastAssignedVars += Pair(name, v)
                                sideEffectOccurred = false
                            }
                        }
                        else {
                            if (shouldConsiderUnused(name)) {
                                variablesToRemove += name
                            }
                            handleTopLevel(initializer)
                        }
                    }
                }
            }

            override fun visitFunction(x: JsFunction) { }

            override fun visitBreak(x: JsBreak) { }

            override fun visitContinue(x: JsContinue) { }

            private fun flush() {
                lastAssignedVars.clear()
            }

            private fun handleTopLevel(expression: JsExpression, respectSideEffects: Boolean = true) {
                lastProperlyUsedIndex = -1
                firstUsedIndex = lastAssignedVars.size
                nextExpectedIndex = -1
                sideEffectOccurred = false
                accept(expression)

                if (lastProperlyUsedIndex >= 0 && nextExpectedIndex == lastAssignedVars.size) {
                    lastAssignedVars.asSequence()
                            .drop(lastProperlyUsedIndex)
                            .forEach {
                                val (name, statement) = it
                                statementsToRemove += statement
                                namesToSubstitute += name
                            }
                }

                if (respectSideEffects && sideEffectOccurred) {
                    lastAssignedVars.clear()
                }
                else {
                    lastAssignedVars.subList(firstUsedIndex, lastAssignedVars.size).clear()
                }
            }
        }.accept(root)
    }

    private fun cleanUp() {
        object : JsVisitorWithContextImpl() {
            override fun visit(x: JsVars, ctx: JsContext<JsNode>): Boolean {
                if (x.vars.removeAll(statementsToRemove)) {
                    hasChanges = true
                }

                val ranges = x.vars.splitToRanges { it.name in variablesToRemove }
                if (ranges.size == 1 && !ranges[0].second) return super.visit(x, ctx)

                hasChanges = true
                for ((subList, isRemoved) in ranges) {
                    val initializers = subList.mapNotNull { it.initExpression }
                    initializers.forEach { accept(it) }
                    if (isRemoved) {
                        for (initializer in initializers) {
                            ctx.addPrevious(JsExpressionStatement(initializer).apply { synthetic = x.synthetic })
                        }
                    }
                    else {
                        ctx.addPrevious(JsVars(*subList.toTypedArray()).apply { synthetic = x.synthetic })
                    }
                }
                ctx.removeMe()
                return false
            }

            override fun visit(x: JsExpressionStatement, ctx: JsContext<JsNode>): Boolean {
                if (x in statementsToRemove) {
                    ctx.removeMe()
                    hasChanges = true
                    return false
                }

                val assignment = JsAstUtils.decomposeAssignmentToVariable(x.expression)
                if (assignment != null) {
                    val (name, value) = assignment
                    if (name in variablesToRemove) {
                        hasChanges = true
                        ctx.replaceMe(JsExpressionStatement(value).run {
                            synthetic = true
                            accept(this)
                        })
                        return false
                    }
                }

                return super.visit(x, ctx)
            }

            override fun visit(x: JsObjectLiteral, ctx: JsContext<*>): Boolean {
                for (initializer in x.propertyInitializers) {
                    accept(initializer.valueExpr)
                }
                return super.visit(x, ctx)
            }

            override fun visit(x: JsNameRef, ctx: JsContext<JsNode>): Boolean {
                val name = x.name
                if (name != null && x.qualifier == null && name in namesToSubstitute) {
                    val replacement = accept(definedValues[name]!!)
                    ctx.replaceMe(replacement)
                    return false
                }
                return super.visit(x, ctx)
            }

            override fun visit(x: JsFunction, ctx: JsContext<*>) = false

            override fun visit(x: JsBreak, ctx: JsContext<*>) = false

            override fun visit(x: JsContinue, ctx: JsContext<*>) = false
        }.accept(root)
    }

    private fun assignVariable(name: JsName, value: JsExpression) {
        definitions[name] = (definitions[name] ?: 0) + 1
        definedValues[name] = value
    }

    private fun useVariable(name: JsName) {
        usages[name] = (usages[name] ?: 0) + 1
    }

    private fun shouldConsiderUnused(name: JsName) = (definitions[name] ?: 0) == 1 && (usages[name] ?: 0) == 0 && name in temporary

    private fun shouldConsiderTemporary(name: JsName): Boolean {
        if (definitions[name] ?: 0 != 1 || name !in temporary) return false

        val expr = definedValues[name]
        return (expr != null && isTrivial(expr)) || usages[name] ?: 0 == 1
    }

    private fun isTrivial(expr: JsExpression): Boolean = when (expr) {
        is JsNameRef -> {
            val qualifier = expr.qualifier
            if (expr.sideEffects == SideEffectKind.PURE && (qualifier == null || isTrivial(qualifier))) {
                true
            }
            else {
                val name = expr.name
                name in namesToProcess && when (definitions[name]) {
                    null, 0 -> true
                    1 -> name !in namesToSubstitute || definedValues[name]?.let { isTrivial(it) } ?: false
                    else -> false
                }
            }
        }
        is JsLiteral.JsValueLiteral -> true
        is JsInvocation -> expr.sideEffects == SideEffectKind.PURE && isTrivial(expr.qualifier) && expr.arguments.all { isTrivial(it) }
        is JsArrayAccess -> isTrivial(expr.arrayExpression) && isTrivial(expr.indexExpression)
        else -> false
    }
}
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

package org.jetbrains.kotlin.js.inline.util

import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.inline.util.collectors.InstanceCollector
import org.jetbrains.kotlin.js.translate.expression.InlineMetadata
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils

fun collectUsedNames(scope: JsNode): Set<JsName> {
    val references = mutableSetOf<JsName>()

    object : RecursiveJsVisitor() {
        override fun visitBreak(x: JsBreak) { }

        override fun visitContinue(x: JsContinue) { }

        override fun visit(x: JsVars.JsVar) {
            val initializer = x.initExpression
            if (initializer != null) {
                accept(initializer)
            }
        }

        override fun visitNameRef(nameRef: JsNameRef) {
            super.visitNameRef(nameRef)
            val name = nameRef.name
            if (name != null && nameRef.qualifier == null) {
                references.add(name)
            }
        }

        override fun visitFunction(x: JsFunction) {
            references += x.collectFreeVariables()
        }
    }.accept(scope)

    return references
}

fun collectDefinedNames(scope: JsNode) = collectDefinedNames(scope, false)

fun collectDefinedNames(scope: JsNode, skipLabelsAndCatches: Boolean): Set<JsName> {
    val names = mutableSetOf<JsName>()

    object : RecursiveJsVisitor() {
        override fun visit(x: JsVars.JsVar) {
            val initializer = x.initExpression
            if (initializer != null) {
                accept(initializer)
            }
            names += x.name
        }

        override fun visitExpressionStatement(x: JsExpressionStatement) {
            val expression = x.expression
            if (expression is JsFunction) {
                val name = expression.name
                if (name != null) {
                    names += name
                }
            }
            super.visitExpressionStatement(x)
        }

        override fun visitLabel(x: JsLabel) {
            if (!skipLabelsAndCatches) {
                x.name?.let { names += it }
            }
            super.visitLabel(x)
        }

        override fun visitCatch(x: JsCatch) {
            if (!skipLabelsAndCatches) {
                names += x.parameter.name
            }
            super.visitCatch(x)
        }

        // Skip function expression, since it does not introduce name in scope of containing function.
        // The only exception is function statement, that is handled with the code above.
        override fun visitFunction(x: JsFunction) { }
    }.accept(scope)

    return names
}

fun JsFunction.collectFreeVariables() = collectUsedNames(body) - collectDefinedNames(body) - parameters.map { it.name }

fun JsFunction.collectLocalVariables(skipLabelsAndCatches: Boolean = false) = collectDefinedNames(body, skipLabelsAndCatches) + parameters.map { it.name }

fun collectNamedFunctions(scope: JsNode) = collectNamedFunctionsAndMetadata(scope).mapValues { it.value.first.function }

fun collectNamedFunctionsOrMetadata(scope: JsNode) = collectNamedFunctionsAndMetadata(scope).mapValues { it.value.second }

fun collectNamedFunctions(fragments: List<JsProgramFragment>): Map<JsName, JsFunction> {
    val result = mutableMapOf<JsName, JsFunction>()
    for (fragment in fragments) {
        result += collectNamedFunctions(fragment.declarationBlock)
        result += collectNamedFunctions(fragment.initializerBlock)
    }
    return result
}

fun collectNamedFunctionsAndMetadata(scope: JsNode): Map<JsName, Pair<FunctionWithWrapper, JsExpression>> {
    val namedFunctions = mutableMapOf<JsName, Pair<FunctionWithWrapper, JsExpression>>()

    scope.accept(object : RecursiveJsVisitor() {
        override fun visitBinaryExpression(x: JsBinaryOperation) {
            val assignment = JsAstUtils.decomposeAssignment(x)
            if (assignment != null) {
                val (left, right) = assignment
                if (left is JsNameRef) {
                    val name = left.name
                    if (name != null) {
                        extractFunction(right)?.let { (function, wrapper) ->
                            namedFunctions[name] = Pair(FunctionWithWrapper(function, wrapper), right)
                        }
                    }
                }
            }
            super.visitBinaryExpression(x)
        }

        override fun visit(x: JsVars.JsVar) {
            val initializer = x.initExpression
            val name = x.name
            if (initializer != null && name != null) {
                extractFunction(initializer)?.let { function ->
                    namedFunctions[name] = Pair(function, initializer)
                }
            }
            super.visit(x)
        }

        override fun visitFunction(x: JsFunction) {
            val name = x.name
            if (name != null) {
                namedFunctions[name] = Pair(FunctionWithWrapper(x, null), x)
            }
            super.visitFunction(x)
        }
    })

    return namedFunctions
}

data class FunctionWithWrapper(val function: JsFunction, val wrapperBody: JsBlock?)

fun extractFunction(expression: JsExpression) = when (expression) {
    is JsFunction -> FunctionWithWrapper(expression, null)
    else -> InlineMetadata.decompose(expression)?.function ?: InlineMetadata.tryExtractFunction(expression)
}

fun <T : JsNode> collectInstances(klass: Class<T>, scope: JsNode, visitNestedDeclarations: Boolean = false): List<T> {
    return with(InstanceCollector(klass, visitNestedDeclarations)) {
        accept(scope)
        collected
    }
}

fun JsNode.collectBreakContinueTargets(): Map<JsContinue, JsStatement> {
    val targets = mutableMapOf<JsContinue, JsStatement>()

    accept(object : RecursiveJsVisitor() {
        var defaultBreakTarget: JsStatement? = null
        var breakTargets = mutableMapOf<JsName, JsStatement?>()
        var defaultContinueTarget: JsStatement? = null
        var continueTargets = mutableMapOf<JsName, JsStatement?>()

        override fun visitLabel(x: JsLabel) {
            val inner = x.statement
            when (inner) {
                is JsDoWhile -> handleLoop(inner, inner.body, x.name)

                is JsWhile -> handleLoop(inner, inner.body, x.name)

                is JsFor -> handleLoop(inner, inner.body, x.name)

                is JsSwitch -> handleSwitch(inner, x.name)

                else -> {
                    withBreakAndContinue(x.name, x.statement, null) {
                        accept(inner)
                    }
                }
            }
        }

        override fun visitWhile(x: JsWhile) = handleLoop(x, x.body, null)

        override fun visitDoWhile(x: JsDoWhile) = handleLoop(x, x.body, null)

        override fun visitFor(x: JsFor) = handleLoop(x, x.body, null)

        override fun visit(x: JsSwitch) = handleSwitch(x, null)

        private fun handleSwitch(statement: JsSwitch, label: JsName?) {
            withBreakAndContinue(label, statement) {
                statement.cases.forEach { accept(it) }
            }
        }

        private fun handleLoop(loop: JsStatement, body: JsStatement, label: JsName?) {
            withBreakAndContinue(label, loop, loop) {
                body.accept(this)
            }
        }

        override fun visitBreak(x: JsBreak) {
            val targetLabel = x.label?.name
            targets[x] = if (targetLabel == null) {
                defaultBreakTarget!!
            }
            else {
                breakTargets[targetLabel]!!
            }
        }

        override fun visitContinue(x: JsContinue) {
            val targetLabel = x.label?.name
            targets[x] = if (targetLabel == null) {
                defaultContinueTarget!!
            }
            else {
                continueTargets[targetLabel]!!
            }
        }

        private fun withBreakAndContinue(
                label: JsName?,
                breakTargetStatement: JsStatement,
                continueTargetStatement: JsStatement? = null,
                action: () -> Unit
        ) {
            val oldDefaultBreakTarget = defaultBreakTarget
            val oldDefaultContinueTarget = defaultContinueTarget
            val (oldBreakTarget, oldContinueTarget) = if (label != null) {
                Pair(breakTargets[label], continueTargets[label])
            }
            else {
                Pair(null, null)
            }

            defaultBreakTarget = breakTargetStatement
            if (label != null) {
                breakTargets[label] = breakTargetStatement
                continueTargets[label] = continueTargetStatement
            }
            if (continueTargetStatement != null) {
                defaultContinueTarget = continueTargetStatement
            }

            action()

            defaultBreakTarget = oldDefaultBreakTarget
            defaultContinueTarget = oldDefaultContinueTarget
            if (label != null) {
                breakTargets[label] = oldBreakTarget
                continueTargets[label] = oldContinueTarget
            }
        }
    })

    return targets
}

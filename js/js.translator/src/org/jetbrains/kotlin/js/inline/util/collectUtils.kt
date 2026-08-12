/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.inline.util

import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.inline.util.collectors.InstanceCollector
import kotlin.reflect.KClass

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
            names += x.declarable.names
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
                names += x.parameter.declarable.names
            }
            super.visitCatch(x)
        }

        // Skip function expression, since it does not introduce name in scope of containing function.
        // The only exception is function statement, that is handled with the code above.
        override fun visitFunction(x: JsFunction) { }
    }.accept(scope)

    return names
}

fun JsFunction.collectFreeVariables() = collectUsedNames(body) - collectDefinedNames(body) - parameters.mapNotNull { it.name }.toSet()

fun JsFunction.collectLocalVariables(skipLabelsAndCatches: Boolean = false) = collectDefinedNames(body, skipLabelsAndCatches) + parameters.mapNotNull { it.name }.toSet()

fun collectNamedFunctions(scope: JsNode) = collectNamedFunctionsAndMetadata(scope).mapValues { it.value.first.function }

private fun collectNamedFunctionsAndMetadata(scope: JsNode): Map<JsName, Pair<FunctionWithWrapper, JsExpression>> {
    val namedFunctions = mutableMapOf<JsName, Pair<FunctionWithWrapper, JsExpression>>()

    scope.accept(object : RecursiveJsVisitor() {
        override fun visitSimpleAssignment(x: JsAssignmentOperation.Simple) {
            val left = x.target
            val right = x.value
            if (left is JsNameRef) {
                val name = left.name
                if (name != null) {
                    extractFunction(right)?.let { (val function, val wrapper = wrapperBody) ->
                        namedFunctions[name] = Pair(FunctionWithWrapper(function, wrapper), right)
                    }
                }
            }
            super.visitSimpleAssignment(x)
        }

        override fun visit(x: JsVars.JsVar) {
            val name = x.name
            val initializer = x.initExpression
            if (name != null && initializer != null) {
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

private fun extractFunction(expression: JsExpression) = when (expression) {
    is JsFunction -> FunctionWithWrapper(expression, null)
    else -> null
}

fun <T : JsNode> collectInstances(klass: KClass<T>, scope: JsNode, visitNestedDeclarations: Boolean = false): List<T> {
    return with(InstanceCollector(klass, visitNestedDeclarations)) {
        accept(scope)
        collected
    }
}


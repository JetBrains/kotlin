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

import org.jetbrains.kotlin.js.backend.JsToStringGenerationVisitor
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.imported
import org.jetbrains.kotlin.js.inline.util.collectors.InstanceCollector
import org.jetbrains.kotlin.js.translate.expression.InlineMetadata
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils

fun collectReferencedNames(scope: JsNode): Set<JsName> {
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
            if (name != null) {
                references += name
            }
        }
    }.accept(scope)

    return references
}

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

fun collectDefinedNames(scope: JsNode): Set<JsName> {
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

        override fun visitCatch(x: JsCatch) {
            names += x.parameter.name
            super.visitCatch(x)
        }

        // Skip function expression, since it does not introduce name in scope of containing function.
        // The only exception is function statement, that is handled with the code above.
        override fun visitFunction(x: JsFunction) { }
    }.accept(scope)

    return names
}

fun collectDefinedNamesInAllScopes(scope: JsNode): Set<JsName> {
    val names = mutableSetOf<JsName>()

    object : RecursiveJsVisitor() {
        override fun visit(x: JsVars.JsVar) {
            super.visit(x)
            names += x.name
        }

        override fun visitFunction(x: JsFunction) {
            super.visitFunction(x)
            x.name?.let { names += it }
            names += x.parameters.map { it.name }
        }
    }.accept(scope)

    return names
}

fun JsFunction.collectFreeVariables() = collectUsedNames(body) - collectDefinedNames(body) - parameters.map { it.name }

fun JsFunction.collectLocalVariables() = collectDefinedNames(body) + parameters.map { it.name }

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

fun collectNamedFunctionsAndWrappers(fragments: List<JsProgramFragment>): Map<JsName, FunctionWithWrapper> {
    val result = mutableMapOf<JsName, FunctionWithWrapper>()
    for (fragment in fragments) {
        result += collectNamedFunctionsAndMetadata(fragment.declarationBlock).mapValues { it.value.first }
        result += collectNamedFunctionsAndMetadata(fragment.initializerBlock).mapValues { it.value.first }
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

fun collectAccessors(scope: JsNode): Map<String, FunctionWithWrapper> {
    val accessors = hashMapOf<String, FunctionWithWrapper>()

    scope.accept(object : RecursiveJsVisitor() {
        override fun visitInvocation(invocation: JsInvocation) {
            InlineMetadata.decompose(invocation)?.let {
                accessors[it.tag.value] = it.function
            }
            super.visitInvocation(invocation)
        }
    })

    return accessors
}

fun collectAccessors(fragments: List<JsProgramFragment>): Map<String, FunctionWithWrapper> {
    val result = mutableMapOf<String, FunctionWithWrapper>()
    for (fragment in fragments) {
        result += collectAccessors(fragment.declarationBlock)
    }
    return result
}

fun collectNameBindings(fragments: List<JsProgramFragment>): Map<JsName, String> {
    val result = mutableMapOf<JsName, String>()
    for (fragment in fragments) {
        for (binding in fragment.nameBindings) {
            result[binding.name] = binding.key
        }
    }
    return result
}

fun extractFunction(expression: JsExpression) = when (expression) {
    is JsFunction -> FunctionWithWrapper(expression, null)
    else -> InlineMetadata.decompose(expression)?.function ?: InlineMetadata.tryExtractFunction(expression)
}

fun <T : JsNode> collectInstances(klass: Class<T>, scope: JsNode): List<T> {
    return with(InstanceCollector(klass, visitNestedDeclarations = false)) {
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

fun getImportTag(jsVars: JsVars): String? {
    if (jsVars.vars.size == 1) {
        val jsVar = jsVars.vars[0]
        if (jsVar.name.imported) {
            return extractImportTag(jsVar)
        }
    }

    return null
}

fun extractImportTag(jsVar: JsVars.JsVar): String? {
    val initExpression = jsVar.initExpression ?: return null

    val sb = StringBuilder()

    // Handle Long const val import
    if (initExpression is JsInvocation || initExpression is JsNew) {
        sb.append(jsVar.name.toString()).append(":")
    }

    return if (extractImportTagImpl(initExpression, sb)) sb.toString() else null
}

private fun extractImportTagImpl(expression: JsExpression, sb: StringBuilder): Boolean {
    when (expression) {
        is JsNameRef -> {
            val nameRef = expression
            if (nameRef.qualifier != null) {
                if (!extractImportTagImpl(nameRef.qualifier!!, sb)) return false
                sb.append('.')
            }
            sb.append(JsToStringGenerationVisitor.javaScriptString(nameRef.ident))
            return true
        }
        is JsArrayAccess -> {
            val arrayAccess = expression
            if (!extractImportTagImpl(arrayAccess.arrayExpression, sb)) return false
            sb.append(".")
            val stringLiteral = arrayAccess.indexExpression as? JsStringLiteral ?: return false
            sb.append(JsToStringGenerationVisitor.javaScriptString(stringLiteral.value))
            return true
        }
        is JsInvocation -> {
            val invocation = expression
            if (!extractImportTagImpl(invocation.qualifier, sb)) return false
            if (!appendArguments(invocation.arguments, sb)) return false
            return true
        }
        is JsNew -> {
            val newExpr = expression
            if (!extractImportTagImpl(newExpr.constructorExpression, sb)) return false
            if (!appendArguments(newExpr.arguments, sb)) return false
            return true
        }
        else -> return false
    }
}

private fun appendArguments(arguments: List<JsExpression>, sb: StringBuilder): Boolean {
    arguments.forEachIndexed { index, arg ->
        if (arg !is JsIntLiteral) {
            return false
        }
        sb.append(if (index == 0) "(" else ",")
        sb.append(arg.value)
    }
    sb.append(")")
    return true
}
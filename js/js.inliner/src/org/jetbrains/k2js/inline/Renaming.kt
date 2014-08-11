/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.k2js.inline

import com.google.dart.compiler.backend.js.ast.*
import com.google.dart.compiler.backend.js.ast.JsVars.JsVar
import org.jetbrains.k2js.inline.util.collectLocalNames

import java.util.ArrayList
import java.util.HashMap
import java.util.IdentityHashMap

import kotlin.test.assertTrue

fun aliasArgumentsIfNeeded(
        context: RenamingContext<*>,
        arguments: List<JsExpression>,
        parameters: List<JsParameter>
) {
    assertTrue { arguments.size <= parameters.size }

    for ((arg, param) in arguments zip parameters) {
        val paramName = param.getName()
        val replacement =
                if (needToAlias(arg)) {
                    val freshName = context.getFreshName(paramName)
                    context.newVar(freshName, arg)
                    freshName.makeRef()
                } else {
                    arg
                }

        context.replaceName(paramName, replacement)
    }

    val defaultParams = parameters.subList(arguments.size, parameters.size)
    for (defaultParam in defaultParams) {
        val paramName = defaultParam.getName()
        val freshName = context.getFreshName(paramName)
        context.newVar(freshName)

        context.replaceName(paramName, freshName.makeRef())
    }
}

/**
 * Makes function local names fresh in context
 */
fun renameLocalNames(
        context: RenamingContext<*>,
        function: JsFunction
) {
    for (name in collectLocalNames(function)) {
        val freshName = context.getFreshName(name)
        context.replaceName(name, freshName.makeRef())
    }
}

private fun isLambdaConstructor(x: JsInvocation): Boolean {
    val staticRef = (x.getQualifier() as? HasName)?.getName()?.getStaticRef()
    return when (staticRef) {
        is JsFunction -> isLambdaConstructor(staticRef)
        else -> false
    }
}

private fun isLambdaConstructor(x: JsFunction): Boolean {
    return InvocationUtil.getInnerFunction(x) != null;
}

private fun needToAlias(x: JsExpression): Boolean {
    val visitor = ShouldBeAliasedVisitor()
    visitor.accept(x)
    return visitor.shouldBeAliased
}

private class ShouldBeAliasedVisitor(): RecursiveJsVisitor() {
    public var shouldBeAliased: Boolean = false
        private set

    override fun visitElement(node: JsNode?) {
        if (!shouldBeAliased) {
            super<RecursiveJsVisitor>.visitElement(node)
        }
    }
    override fun visitBinaryExpression(x: JsBinaryOperation?) {
        shouldBeAliased = true
    }

    override fun visitInvocation(invocation: JsInvocation?) {
        if (invocation != null && !isLambdaConstructor(invocation)) {
            shouldBeAliased = true
        }
    }

    override fun visitPostfixOperation(x: JsPostfixOperation?) {
        shouldBeAliased = true
    }

    override fun visitPrefixOperation(x: JsPrefixOperation?) {
        shouldBeAliased = true
    }

    override fun visitObjectLiteral(x: JsObjectLiteral?) {
        shouldBeAliased = true
    }

    override fun visitNew(x: JsNew?) {
        shouldBeAliased = true
    }

    override fun visitThis(x: JsLiteral.JsThisRef?) {
        shouldBeAliased = true
    }

    override fun visitArray(x: JsArrayLiteral?) {
        shouldBeAliased = true
    }
}

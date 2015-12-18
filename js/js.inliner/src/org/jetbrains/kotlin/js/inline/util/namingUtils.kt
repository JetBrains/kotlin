/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

import com.google.dart.compiler.backend.js.ast.*

import org.jetbrains.kotlin.js.inline.context.NamingContext
import org.jetbrains.kotlin.js.inline.util.rewriters.LabelNameRefreshingVisitor

public fun aliasArgumentsIfNeeded(
        context: NamingContext,
        arguments: List<JsExpression>,
        parameters: List<JsParameter>
) {
    require(arguments.size <= parameters.size) { "arguments.size (${arguments.size}) should be less or equal to parameters.size (${parameters.size})" }

    for ((arg, param) in arguments.zip(parameters)) {
        val paramName = param.getName()
        val replacement =
                if (arg.needToAlias()) {
                    val freshName = context.getFreshName(paramName)
                    context.newVar(freshName, arg)
                    freshName.makeRef()
                } else {
                    arg
                }

        context.replaceName(paramName, replacement)
    }

    val defaultParams = parameters.subList(arguments.size(), parameters.size())
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
public fun renameLocalNames(
        context: NamingContext,
        function: JsFunction
) {
    for (name in collectLocalNames(function)) {
        val freshName = context.getFreshName(name)
        context.replaceName(name, freshName.makeRef())
    }
}

public fun refreshLabelNames(
        context: NamingContext,
        function: JsFunction
) {
    val scope = function.getScope()
    if (scope !is JsFunctionScope) throw AssertionError("JsFunction is expected to have JsFunctionScope")

    val visitor = LabelNameRefreshingVisitor(context, scope)
    visitor.accept(function.getBody())
    context.applyRenameTo(function)
}

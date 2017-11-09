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

package com.google.gwt.dev.js

import org.jetbrains.kotlin.js.backend.ast.*
import java.util.*

class ScopeContext(scope: JsScope) {
    private val rootScope = generateSequence(scope) { it.parent }.first { it is JsRootScope }
    private val scopes = Stack<JsScope>()

    init {
        scopes.push(scope)
    }

    fun enterFunction(): JsFunction {
        val fn = JsFunction(currentScope, "<js function>")
        enterScope(fn.scope)
        return fn
    }

    fun exitFunction() {
        assert(currentScope is JsDeclarationScope)
        exitScope()
    }

    fun enterCatch(ident: String): JsCatch {
        val jsCatch = JsCatch(currentScope, ident)
        enterScope(jsCatch.scope)
        return jsCatch
    }

    fun exitCatch() {
        assert(currentScope is JsCatchScope)
        exitScope()
    }

    fun enterLabel(ident: String, outputName: String): JsName =
            (currentScope as JsDeclarationScope).enterLabel(ident, outputName)

    fun exitLabel() =
            (currentScope as JsDeclarationScope).exitLabel()

    fun labelFor(ident: String): JsName? =
            (currentScope as JsDeclarationScope).findLabel(ident)

    fun globalNameFor(ident: String): JsName =
            currentScope.findName(ident) ?: rootScope.declareName(ident)

    fun localNameFor(ident: String): JsName =
            currentScope.findOwnNameOrDeclare(ident)

    fun referenceFor(ident: String): JsNameRef =
            JsNameRef(ident)

    private fun enterScope(scope: JsScope) = scopes.push(scope)

    private fun exitScope() = scopes.pop()

    private val currentScope: JsScope
        get() = scopes.peek()
}

/**
 * Overrides JsFunctionScope declareName as it's mapped to declareFreshName
 */
private fun JsScope.findOwnNameOrDeclare(ident: String): JsName =
        when (this) {
            is JsFunctionScope -> declareNameUnsafe(ident)
            else -> declareName(ident)
        }
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

package org.jetbrains.kotlin.script.jsr223.core

import java.io.Reader
import javax.script.Bindings
import javax.script.ScriptContext
import javax.script.ScriptEngine
import javax.script.ScriptException

abstract class AbstractIoFriendlyScriptEngine() : ScriptEngine {
    constructor(n: Bindings?) : this() {
        if (n == null) {
            throw NullPointerException("n is null")
        }
        context.setBindings(n, ScriptContext.ENGINE_SCOPE)
    }

    protected var _context: ScriptContext = IoFriendlyScriptContext()

    override fun setContext(ctxt: ScriptContext?) {
        if (ctxt == null) {
            throw NullPointerException("null context")
        }
        _context = ctxt
    }

    override fun getContext(): ScriptContext {
        return _context
    }

    override fun getBindings(scope: Int): Bindings? {
        if (scope == ScriptContext.GLOBAL_SCOPE) {
            return context.getBindings(ScriptContext.GLOBAL_SCOPE)
        }
        else if (scope == ScriptContext.ENGINE_SCOPE) {
            return context.getBindings(ScriptContext.ENGINE_SCOPE)
        }
        else {
            throw IllegalArgumentException("Invalid scope value.")
        }
    }

    override fun setBindings(bindings: Bindings, scope: Int) {
        if (scope == ScriptContext.GLOBAL_SCOPE) {
            context.setBindings(bindings, ScriptContext.GLOBAL_SCOPE)
        }
        else if (scope == ScriptContext.ENGINE_SCOPE) {
            context.setBindings(bindings, ScriptContext.ENGINE_SCOPE)
        }
        else {
            throw IllegalArgumentException("Invalid scope value.")
        }
    }

    override fun put(key: String, value: Any) {
        val nn = getBindings(ScriptContext.ENGINE_SCOPE)
        nn?.put(key, value)
    }

    override fun get(key: String): Any? {
        val nn = getBindings(ScriptContext.ENGINE_SCOPE)
        return nn?.get(key)
    }

    @Throws(ScriptException::class)
    override fun eval(reader: Reader, bindings: Bindings): Any {
        val ctxt = getScriptContext(bindings)
        return eval(reader, ctxt)
    }

    @Throws(ScriptException::class)
    override fun eval(script: String, bindings: Bindings): Any {
        val ctxt = getScriptContext(bindings)
        return eval(script, ctxt)
    }

    @Throws(ScriptException::class)
    override fun eval(reader: Reader): Any {
        return eval(reader, context)
    }

    @Throws(ScriptException::class)
    override fun eval(script: String): Any {
        return eval(script, context)
    }

    protected fun getScriptContext(nn: Bindings?): ScriptContext {
        val ctxt = IoFriendlyScriptContext()
        val gs = getBindings(ScriptContext.GLOBAL_SCOPE)

        if (gs != null) {
            ctxt.setBindings(gs, ScriptContext.GLOBAL_SCOPE)
        }

        if (nn != null) {
            ctxt.setBindings(nn,
                             ScriptContext.ENGINE_SCOPE)
        }
        else {
            throw NullPointerException("Engine scope Bindings may not be null.")
        }

        ctxt.reader = context.reader
        ctxt.writer = context.writer
        ctxt.errorWriter = context.errorWriter

        return ctxt
    }
}

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

import org.apache.commons.io.input.ReaderInputStream
import org.apache.commons.io.output.WriterOutputStream
import org.jetbrains.kotlin.cli.common.repl.InvokeWrapper
import org.jetbrains.kotlin.script.util.InOutTrapper
import org.jetbrains.kotlin.script.util.RerouteScriptIoInvoker
import java.io.*
import javax.script.Bindings
import javax.script.ScriptContext
import javax.script.SimpleBindings

fun makeBestIoTrappingInvoker(context: ScriptContext): InvokeWrapper {
    // for recognizable things that are NOT looping back to possible our own thread aware
    // IO trapper, we can do nice thread specific capturing, otherwise assume the worst and
    // use old style unsafe IO trapping.  A caller can easily tag their reader/writers with
    // a marker interface to tell us to do otherwise.
    return if (isFriendly(context.reader) &&
               isFriendly(context.writer) &&
               isFriendly(context.errorWriter)) {
        ContextRerouteScriptIoInvoker(context)
    }
    else {
        UnfriendlyContextRerouteScriptIoInvoker(context)
    }
}

class UnfriendlyContextRerouteScriptIoInvoker(val context: ScriptContext) : InvokeWrapper {
    override fun <T> invoke(body: () -> T): T {
        // TODO: this is so unsafe for multi-threaded environment, we don't know what each
        // thread is capturing too, but it is likely ok since most threads in a script context
        // would be using the same input and output streams.  The bigger problem is that
        // we might leave it set to something other than the original stdin/out

        // TODO: alternative is that since most people do not change stdin/out we could always
        // set back to the original stdin/out seen at the time of the first call.  But it is hard
        // to determine the best default behavior.

        // The user can wrap their reader/writer with the IoCaptureFriendly interface and then
        // none of this evil will happen.

        val (oldIn, oldOut, oldErr) = synchronized(this.javaClass) {
            val oldIn = System.`in`
            val oldOut = System.out
            val oldErr = System.err
            System.setIn(ReaderInputStream(context.reader))
            System.setOut(PrintStream(WriterOutputStream(context.writer), true))
            System.setErr(PrintStream(WriterOutputStream(context.errorWriter), true))
            Triple(oldIn, oldOut, oldErr)
        }
        try {
            return body()
        }
        finally {
            synchronized(this.javaClass) {
                System.setIn(oldIn)
                System.setOut(oldOut)
                System.setErr(oldErr)
            }
        }
    }
}

class ContextRerouteScriptIoInvoker(val context: ScriptContext)
    : RerouteScriptIoInvoker(wrapFriendlyReader(context.reader),
                             wrapFriendlyWriter(context.writer),
                             wrapFriendlyWriter(context.errorWriter))

fun isFriendly(reader: Reader): Boolean {
    return reader is IoCaptureFriendly
           || reader is StringReader
           || reader is CharArrayReader
           || reader is FileReader
}

fun isFriendly(writer: Writer): Boolean {
    return writer is IoCaptureFriendly
           || writer is StringWriter
           || writer is CharArrayWriter
           || writer is FileWriter
}

fun wrapFriendlyReader(reader: Reader): InputStream {
    return if (isFriendly(reader)) MarkedFriendlyReaderInputStream(reader)
    else ReaderInputStream(reader)
}

fun wrapFriendlyWriter(writer: Writer): PrintStream {
    return if (isFriendly(writer)) MarkedFriendlyPrintStream(WriterOutputStream(writer), true)
    else PrintStream(WriterOutputStream(writer), true)
}

interface IoCaptureFriendly

class MarkedFriendlyReaderInputStream(reader: Reader) : ReaderInputStream(reader), IoCaptureFriendly
class MarkedFriendlyInputStreamReader(inputStream: InputStream) : InputStreamReader(inputStream), IoCaptureFriendly
class MarkedFriendlyPrintWriter(outputStream: OutputStream) : PrintWriter(outputStream), IoCaptureFriendly
class MarkedFriendlyPrintStream(outputStream: OutputStream, autoFlush: Boolean) : PrintStream(outputStream, autoFlush), IoCaptureFriendly


open class IoFriendlyScriptContext : ScriptContext {
    protected var _writer: Writer = MarkedFriendlyPrintWriter(InOutTrapper.originalSystemOut)
    protected var _errorWriter: Writer = MarkedFriendlyPrintWriter(InOutTrapper.originalSystemErr)
    protected var _reader: Reader = MarkedFriendlyInputStreamReader(InOutTrapper.originalSystemIn)

    protected var _engineScope: Bindings = SimpleBindings()
    protected var _globalScope: Bindings? = null

    override fun setBindings(bindings: Bindings?, scope: Int) {
        when (scope) {
            ScriptContext.ENGINE_SCOPE -> {
                if (bindings == null) {
                    throw NullPointerException("Engine scope cannot be null.")
                }
                _engineScope = bindings
            }
            ScriptContext.GLOBAL_SCOPE -> _globalScope = bindings
            else -> throw IllegalArgumentException("Invalid scope value.")
        }
    }

    override fun getAttribute(name: String): Any? {
        checkName(name)
        return _engineScope.get(name) ?: _globalScope?.get(name)
    }

    override fun getAttribute(name: String, scope: Int): Any? {
        checkName(name)
        return when (scope) {
            ScriptContext.ENGINE_SCOPE -> _engineScope[name]
            ScriptContext.GLOBAL_SCOPE -> _globalScope?.get(name)
            else -> throw IllegalArgumentException("Illegal scope value.")
        }
    }

    override fun removeAttribute(name: String, scope: Int): Any? {
        checkName(name)
        return when (scope) {
            ScriptContext.ENGINE_SCOPE -> _engineScope.remove(name)
            ScriptContext.GLOBAL_SCOPE -> _globalScope?.remove(name)
            else -> throw IllegalArgumentException("Illegal scope value.")
        }
    }

    override fun setAttribute(name: String, value: Any, scope: Int) {
        checkName(name)
        when (scope) {
            ScriptContext.ENGINE_SCOPE -> _engineScope.put(name, value)
            ScriptContext.GLOBAL_SCOPE -> _globalScope?.put(name, value)
            else -> throw IllegalArgumentException("Illegal scope value.")
        }
    }

    override fun getWriter(): Writer {
        return _writer
    }

    override fun getReader(): Reader {
        return _reader
    }

    override fun setReader(reader: Reader) {
        _reader = reader
    }

    override fun setWriter(writer: Writer) {
        _writer = writer
    }

    override fun getErrorWriter(): Writer {
        return _errorWriter
    }

    override fun setErrorWriter(writer: Writer) {
        _errorWriter = writer
    }

    override fun getAttributesScope(name: String): Int {
        checkName(name)
        if (_engineScope.containsKey(name)) {
            return ScriptContext.ENGINE_SCOPE
        }
        else if (_globalScope?.containsKey(name) ?: false) {
            return ScriptContext.GLOBAL_SCOPE
        }
        else {
            return -1
        }
    }

    override fun getBindings(scope: Int): Bindings? {
        if (scope == ScriptContext.ENGINE_SCOPE) {
            return _engineScope
        }
        else if (scope == ScriptContext.GLOBAL_SCOPE) {
            return _globalScope
        }
        else {
            throw IllegalArgumentException("Illegal scope value.")
        }
    }

    override fun getScopes(): List<Int> {
        return _scopes
    }

    private fun checkName(name: String) {
        if (name.isEmpty()) {
            throw IllegalArgumentException("name cannot be empty")
        }
    }

    companion object {
        private var _scopes: List<Int> = listOf(ScriptContext.ENGINE_SCOPE, ScriptContext.GLOBAL_SCOPE)
    }
}


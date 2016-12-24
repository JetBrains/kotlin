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

package kotlin.io

private abstract class BaseOutput {
    open fun println() {
        print("\n")
    }

    open fun println(message: Any?) {
        print(message)
        println()
    }

    abstract fun print(message: Any?)

    open fun flush() {}
}

/** JsName used to make the declaration available outside of module to test it */
@JsName("NodeJsOutput")
private class NodeJsOutput(val outputStream: dynamic) : BaseOutput() {
    override fun print(message: Any?) = outputStream.write(message)
}

/** JsName used to make the declaration available outside of module to test it */
@JsName("OutputToConsoleLog")
private class OutputToConsoleLog : BaseOutput() {
    override fun print(message: Any?) {
        console.log(message)
    }

    override fun println(message: Any?) {
        console.log(message)
    }

    override fun println() {
        console.log()
    }
}

/** JsName used to make the declaration available outside of module to test it and use at try.kotl.in */
@JsName("BufferedOutput")
private open class BufferedOutput : BaseOutput() {
    var buffer = ""

    override fun print(message: Any?) {
        buffer += String(message)
    }

    override fun flush() {
        buffer = ""
    }
}

/** JsName used to make the declaration available outside of module to test it */
@JsName("BufferedOutputToConsoleLog")
private class BufferedOutputToConsoleLog : BufferedOutput() {
    override fun print(message: Any?) {
        var s = String(message)
        val i = s.lastIndexOf('\n')
        if (i >= 0) {
            buffer += s.substring(0, i)
            flush()
            s = s.substring(i + 1)
        }
        buffer += s
    }

    override fun flush() {
        console.log(buffer)
        buffer = ""
    }
}

/** JsName used to make the declaration available outside of module to test it and use at try.kotl.in */
@JsName("output")
private var output = run {
    val isNode: Boolean = js("typeof process !== 'undefined' && process.versions && !!process.versions.node")
    if (isNode) NodeJsOutput(js("process.stdout")) else BufferedOutputToConsoleLog()
}

private inline fun String(value: Any?): String = js("String")(value)

/** Prints a newline to the standard output stream. */
public fun println() {
    output.println()
}

/** Prints the given message and newline to the standard output stream. */
public fun println(message: Any?) {
    output.println(message)
}

/** Prints the given message to the standard output stream. */
public fun print(message: Any?) {
    output.print(message)
}

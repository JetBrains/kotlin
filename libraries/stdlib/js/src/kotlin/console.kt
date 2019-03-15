/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.io

internal abstract class BaseOutput {
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
internal class NodeJsOutput(val outputStream: dynamic) : BaseOutput() {
    override fun print(message: Any?) = outputStream.write(String(message))
}

/** JsName used to make the declaration available outside of module to test it */
@JsName("OutputToConsoleLog")
internal class OutputToConsoleLog : BaseOutput() {
    override fun print(message: Any?) {
        console.log(message)
    }

    override fun println(message: Any?) {
        console.log(message)
    }

    override fun println() {
        console.log("")
    }
}

/** JsName used to make the declaration available outside of module to test it and use at try.kotl.in */
@JsName("BufferedOutput")
internal open class BufferedOutput : BaseOutput() {
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
internal class BufferedOutputToConsoleLog : BufferedOutput() {
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
internal var output = run {
    val isNode: Boolean = js("typeof process !== 'undefined' && process.versions && !!process.versions.node")
    if (isNode) NodeJsOutput(js("process.stdout")) else BufferedOutputToConsoleLog()
}

@kotlin.internal.InlineOnly
private inline fun String(value: Any?): String = js("String")(value)

/** Prints the line separator to the standard output stream. */
public actual fun println() {
    output.println()
}

/** Prints the given [message] and the line separator to the standard output stream. */
public actual fun println(message: Any?) {
    output.println(message)
}

/** Prints the given [message] to the standard output stream. */
public actual fun print(message: Any?) {
    output.print(message)
}

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

@JsName("BaseOutput")
internal abstract class BaseOutput {
    @JsName("println")
    open fun println(a: Any?) {
        if (jsTypeOf(a) != "undefined") {
            print(a)
        }
        print("\n")
    }

    @JsName("print")
    abstract fun print(a: Any?)

    @JsName("flush")
    open fun flush() {}
}

@JsName("NodeJsOutput")
internal class NodeJsOutput(val outputStream: dynamic) : BaseOutput() {
    override fun print(a: dynamic) = outputStream.write(a)
}

@JsName("OutputToConsoleLog")
internal class OutputToConsoleLog : BaseOutput() {
    override fun print(a: Any?) {
        console.log(a)
    }

    override fun println(a: Any?) {
        console.log(if (jsTypeOf(a) != "undefined") a else "")
    }
}

@JsName("BufferedOutput")
internal open class BufferedOutput : BaseOutput() {
    var buffer = ""

    override fun print(a: Any?) {
        buffer += String(a)
    }

    override fun flush() {
        buffer = ""
    }
}

@JsName("BufferedOutputToConsoleLog")
internal class BufferedOutputToConsoleLog : BufferedOutput() {
    override fun print(a: Any?) {
        var s = String(a)
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

@JsName("out")
internal var `out` = {
    val isNode: Boolean = js("typeof process !== 'undefined' && process.versions && !!process.versions.node")
    if (isNode) NodeJsOutput(js("process.stdout")) else BufferedOutputToConsoleLog()
}()

private inline fun String(value: Any?): String = js("String")(value)

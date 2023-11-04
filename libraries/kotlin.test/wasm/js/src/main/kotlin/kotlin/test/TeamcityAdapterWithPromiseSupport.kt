/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.test

import kotlin.math.abs
import kotlin.js.*

// Using 'globalThis.arguments' because 'arguments' can refer to current JS function arguments
@JsFun("() => globalThis.arguments?.join?.(' ') ?? ''")
private external fun d8Arguments(): String
@JsFun("() => (typeof process != 'undefined' && typeof process.argv != 'undefined') ? process.argv.slice(2).join(' ') : ''")
private external fun nodeArguments(): String

internal actual fun getArguments(): List<String> = (d8Arguments().ifEmpty { nodeArguments() }).split(' ')

internal class TeamcityAdapterWithPromiseSupport : TeamcityAdapter() {
    private var scheduleNextTaskAfter: Promise<JsAny?>? = null
    override fun runOrScheduleNext(block: () -> Unit) {
        if (scheduleNextTaskAfter == null) {
            block()
        } else {
            scheduleNextTaskAfter = scheduleNextTaskAfter!!.finally(block)
        }
    }

    override fun runOrScheduleNextWithResult(block: () -> Any?) {
        val nextTask = scheduleNextTaskAfter
        if (nextTask == null) {
            val result = block()
            if (result is Promise<JsAny?>)
                scheduleNextTaskAfter = result
        } else {
            scheduleNextTaskAfter = nextTask.then {
                block() as? Promise<JsAny?>
            }
        }
    }

    override fun tryProcessResult(result: Any?, name: String): Any? {
        if (result == null) return null
        if (result !is Promise<*>) return null

        return result.then(
            onFulfilled = { value ->
                MessageType.Finished.report(name)
                value
            },
            onRejected = { e ->
                val throwable = e.toThrowableOrNull()
                if (throwable != null) {
                    MessageType.Failed.report(name, throwable)
                } else {
                    MessageType.Failed.report(name, e.toString())
                }
                MessageType.Finished.report(name)
                null
            }
        )
    }
}
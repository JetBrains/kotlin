/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.test

import kotlin.test.FrameworkAdapter
import kotlin.js.Promise

// Need to wrap into additional lambdas so that js launcher will work without Mocha or any other testing framework
private fun describe(name: String, fn: () -> Unit): Unit =
    js("describe(name, fn)")

private fun xdescribe(name: String, fn: () -> Unit): Unit =
    js("xdescribe(name, fn)")

private fun it(name: String, fn: () -> JsAny?): Unit =
    js("it(name, fn)")

private fun xit(name: String, fn: () -> JsAny?): Unit =
    js("xit(name, fn)")

private fun jsThrow(jsException: JsAny) {
    js("throw e")
}

private fun throwableToJsError(message: String, stack: String): JsAny {
    js("var e = new Error(); e.message = message; e.stack = stack; return e;")
}

private fun Throwable.toJsError(): JsAny =
    throwableToJsError(message ?: "", stackTraceToString())

/**
 * [Jasmine](https://github.com/jasmine/jasmine) adapter.
 * Also used for [Mocha](https://mochajs.org/) and [Jest](https://facebook.github.io/jest/).
 */
internal class JasmineLikeAdapter : FrameworkAdapter {
    override fun suite(name: String, ignored: Boolean, suiteFn: () -> Unit) {
        if (ignored) {
            xdescribe(name, suiteFn)
        } else {
            describe(name, suiteFn)
        }
    }

    private fun callTest(testFn: () -> Any?): JsAny? =
        try {
            (testFn() as? Promise<*>)?.catch { exception ->
                val jsException = exception
                    .toThrowableOrNull()
                    ?.let { it.toJsError() }
                    ?: exception
                Promise.reject(jsException)
            }
        } catch (exception: Throwable) {
            jsThrow(exception.toJsError())
            null
        }

    override fun test(name: String, ignored: Boolean, testFn: () -> Any?) {
        if (ignored) {
            xit(name) {
                callTest(testFn)
            }
        } else {
            it(name) {
                callTest(testFn)
            }
        }
    }
}
/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.debugger

/**
 * An interface for invoking [Chrome DevTools protocol](https://chromedevtools.github.io/devtools-protocol/) methods.
 *
 * An instance of this interface is passed to the `block` closure in [NodeJsInspectorClient.run].
 */
interface NodeJsInspectorClientContext {

    /**
     * The [`Debugger` domain](https://chromedevtools.github.io/devtools-protocol/tot/Debugger/) of Chrome DevTools protocol.
     */
    val debugger: Debugger

    /**
     * The [`Runtime` domain](https://chromedevtools.github.io/devtools-protocol/tot/Runtime/) of Chrome DevTools protocol.
     */
    val runtime: Runtime

    @Deprecated("Only for debugging purposes", level = DeprecationLevel.WARNING)
    suspend fun sendPlainTextMessage(methodName: String, paramsJson: String): String

    /**
     * On each incoming message invokes [predicate], and returns only when [predicate] returns `true`.
     */
    suspend fun waitForConditionToBecomeTrue(predicate: () -> Boolean)
}

/**
 * On each incoming message checks whether [test] returns `null`, and returns only when [test] returns non-`null` value.
 */
suspend inline fun <T> NodeJsInspectorClientContext.waitForValueToBecomeNonNull(crossinline test: () -> T?): T {
    var value: T? = null
    waitForConditionToBecomeTrue {
        test()?.also {
            value = it
        } != null
    }
    return value!!
}

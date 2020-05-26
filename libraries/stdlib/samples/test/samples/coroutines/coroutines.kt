/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package samples.coroutines

import samples.Sample
import samples.assertPrints
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.*
import kotlin.test.assertTrue

class Coroutines {

    @Sample
    fun completableFuture() {
        class FutureContinuation<T> : CompletableFuture<T>(), Continuation<T> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<T>) {
                result.onSuccess { complete(it) }.onFailure { completeExceptionally(it) }
            }
        }

        suspend fun <T> CompletableFuture<T>.await(): T =
            suspendCoroutine { continuation ->
                whenComplete { result, exception ->
                    if (exception == null) // the future has been completed normally
                        continuation.resume(result)
                    else // the future has completed with an exception
                        continuation.resumeWithException(exception)
                }
            }

        val block: suspend () -> String = {
            val bar = CompletableFuture.supplyAsync { "bar" }.await()
            "foo$bar"
        }
        val result = FutureContinuation<String>().also { block.startCoroutine(it) }.join()
        assertPrints(result, "foobar")
    }

    @Sample
    fun codeFlow() {
        // list to track the order of executed code
        val list = mutableListOf<Int>()

        // suspending lambda which stores its suspension point continuation in a local variable
        var suspensionPoint: Continuation<Short>? = null
        val block: suspend () -> Int = {
            list += 2
            val resumed = suspendCoroutine<Short> { suspensionPoint = it }
            list += resumed.toInt()
            5
        }

        // create a coroutine from the lambda, start and resume it after the suspension point
        list += 0
        val onComplete = Continuation<Int>(EmptyCoroutineContext) { list += it.getOrThrow() }
        val init = block.createCoroutine(onComplete)
        list += 1
        init.resume(Unit) // starts the coroutine
        list += 3
        suspensionPoint!!.resume(4)
        list += 6

        assertPrints(list, "[0, 1, 2, 3, 4, 5, 6]")
    }

    @Sample
    fun restrictsSuspension() {
        @RestrictsSuspension
        class SuspensionScope {
            var result: String? = null
            var continuation: Continuation<Unit>? = null
            suspend fun suspend() {
                suspendCoroutine<Unit> { continuation = it }
                result = "foo"
            }
        }

        val block: suspend SuspensionScope.() -> Unit = {
            // this block can only invoke suspend functions on this receiver
            // suspendCoroutine<Unit> {} // does no compile
            this.suspend() // compiles
        }

        val receiver = SuspensionScope()
        block.startCoroutine(receiver, Continuation(EmptyCoroutineContext) {})
        receiver.continuation!!.resume(Unit)
        assertPrints(receiver.result, "foo")
    }

    @Sample
    fun auth() {
        abstract class AuthElement(val name: String) : CoroutineContext.Element

        val domainFoo = object : CoroutineContext.Key<AuthElement> {}
        val domainBar = object : CoroutineContext.Key<AuthElement> {}

        class AuthFoo(name: String) : AuthElement(name) {
            override val key get() = domainFoo
        }

        class AuthBar(name: String) : AuthElement(name) {
            override val key get() = domainBar
        }

        val authorize: suspend () -> Boolean = {
            listOf(domainFoo, domainBar).any { coroutineContext[it]?.name == "admin" }
        }
        val combinedAuth = AuthFoo("user") + AuthBar("admin")
        authorize.startCoroutine(Continuation(combinedAuth) { isAuthorized ->
            assertTrue(isAuthorized.getOrDefault(false))
        })
    }

    @Sample
    fun interceptor() {
        class LoggingContinuation<T>(
            val cont: Continuation<T>,
            val logger: (String) -> Unit
        ) : Continuation<T> {
            override val context: CoroutineContext = cont.context
            override fun resumeWith(result: Result<T>) {
                logger(result.getOrNull().toString())
                cont.resumeWith(result)
            }
        }

        class LoggingInterceptor :
            AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {
            val logs = mutableListOf<String>()
            override fun <T> interceptContinuation(continuation: Continuation<T>) =
                LoggingContinuation(continuation, logs::add)
        }

        val logger = LoggingInterceptor()
        val block: suspend () -> Unit = {}
        block.startCoroutine(Continuation(logger) {})
        // the initial continuation is intercepted and the Unit object is logged
        assertPrints(logger.logs, "[kotlin.Unit]")
    }

}

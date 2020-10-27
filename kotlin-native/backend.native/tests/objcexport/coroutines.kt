/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package coroutines

import kotlin.coroutines.*
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.intrinsics.*
import kotlin.native.concurrent.isFrozen
import kotlin.native.internal.ObjCErrorException
import kotlin.test.*

class CoroutineException : Throwable()

suspend fun suspendFun() = 42

@Throws(CoroutineException::class, CancellationException::class)
suspend fun suspendFun(result: Any?, doSuspend: Boolean, doThrow: Boolean): Any? {
    if (doSuspend) {
        suspendCoroutineUninterceptedOrReturn<Unit> {
            it.resume(Unit)
            COROUTINE_SUSPENDED
        }
    }

    if (doThrow) throw CoroutineException()

    return result
}

class ContinuationHolder<T> {
    internal lateinit var continuation: Continuation<T>

    fun resume(value: T) {
        continuation.resume(value)
    }

    fun resumeWithException(exception: Throwable) {
        continuation.resumeWithException(exception)
    }
}

@Throws(CoroutineException::class, CancellationException::class)
suspend fun suspendFunAsync(result: Any?, continuationHolder: ContinuationHolder<Any?>): Any? =
        suspendCoroutineUninterceptedOrReturn<Any?> {
            continuationHolder.continuation = it
            COROUTINE_SUSPENDED
        } ?: result

@Throws(CoroutineException::class, CancellationException::class)
fun throwException(exception: Throwable) {
    throw exception
}

interface SuspendFun {
    @Throws(CoroutineException::class, CancellationException::class)
    suspend fun suspendFun(doYield: Boolean, doThrow: Boolean): Int
}

class ResultHolder<T> {
    var completed: Int = 0
    var result: T? = null
    var exception: Throwable? = null

    internal fun complete(result: Result<T>) {
        this.result = result.getOrNull()
        this.exception = result.exceptionOrNull()
        this.completed += 1
    }
}

private class ResultHolderCompletion<T>(val resultHolder: ResultHolder<T>) : Continuation<T> {
    override val context: CoroutineContext
        get() = EmptyCoroutineContext

    override fun resumeWith(result: Result<T>) {
        resultHolder.complete(result)
    }
}

fun callSuspendFun(suspendFun: SuspendFun, doYield: Boolean, doThrow: Boolean, resultHolder: ResultHolder<Int>) {
    suspend { suspendFun.suspendFun(doYield = doYield, doThrow = doThrow) }
            .startCoroutine(ResultHolderCompletion(resultHolder))
}

@Throws(CoroutineException::class, CancellationException::class)
suspend fun callSuspendFun2(suspendFun: SuspendFun, doYield: Boolean, doThrow: Boolean): Int {
    return suspendFun.suspendFun(doYield = doYield, doThrow = doThrow)
}

interface SuspendBridge<T> {
    suspend fun int(value: T): Int
    suspend fun intAsAny(value: T): Any?

    suspend fun unit(value: T): Unit
    suspend fun unitAsAny(value: T): Any?

    @Throws(Throwable::class) suspend fun nothing(value: T): Nothing
    @Throws(Throwable::class) suspend fun nothingAsInt(value: T): Int
    @Throws(Throwable::class) suspend fun nothingAsAny(value: T): Any?
    @Throws(Throwable::class) suspend fun nothingAsUnit(value: T): Unit
}

abstract class AbstractSuspendBridge : SuspendBridge<Int> {
    override suspend fun intAsAny(value: Int): Int = TODO()

    override suspend fun unitAsAny(value: Int): Unit = TODO()

    override suspend fun nothingAsInt(value: Int): Nothing = TODO()
    override suspend fun nothingAsAny(value: Int): Nothing = TODO()
    override suspend fun nothingAsUnit(value: Int): Nothing = TODO()
}

private suspend fun callSuspendBridgeImpl(bridge: SuspendBridge<Int>) {
    assertEquals(1, bridge.intAsAny(1))

    assertSame(Unit, bridge.unitAsAny(2))

    assertFailsWith<ObjCErrorException> { bridge.nothingAsInt(3) }
    assertFailsWith<ObjCErrorException> { bridge.nothingAsAny(4) }
    assertFailsWith<ObjCErrorException> { bridge.nothingAsUnit(5) }
}

private suspend fun callAbstractSuspendBridgeImpl(bridge: AbstractSuspendBridge) {
    assertEquals(6, bridge.intAsAny(6))

    assertSame(Unit, bridge.unitAsAny(7))

    assertFailsWith<ObjCErrorException> { bridge.nothingAsInt(8) }
    assertFailsWith<ObjCErrorException> { bridge.nothingAsAny(9) }
    assertFailsWith<ObjCErrorException> { bridge.nothingAsUnit(10) }
}

@Throws(Throwable::class)
fun callSuspendBridge(bridge: AbstractSuspendBridge, resultHolder: ResultHolder<Unit>) {
    suspend {
        callSuspendBridgeImpl(bridge)
        callAbstractSuspendBridgeImpl(bridge)
    }.startCoroutine(ResultHolderCompletion(resultHolder))
}

suspend fun throwCancellationException(): Unit {
    val exception = CancellationException("coroutine is cancelled")

    // Note: frontend checker hardcodes fq names of CancellationException super classes (see NativeThrowsChecker).
    // This is our best effort to keep that list in sync with actual stdlib code:
    assertTrue(exception is kotlin.Throwable)
    assertTrue(exception is kotlin.Exception)
    assertTrue(exception is kotlin.RuntimeException)
    assertTrue(exception is kotlin.IllegalStateException)
    assertTrue(exception is kotlin.coroutines.cancellation.CancellationException)

    throw exception
}

abstract class ThrowCancellationException {
    internal abstract suspend fun throwCancellationException()
}

class ThrowCancellationExceptionImpl : ThrowCancellationException() {
    public override suspend fun throwCancellationException() {
        throw CancellationException()
    }
}

fun getSuspendLambda0(): suspend () -> String = { "lambda 0" }

private suspend fun suspendCallableReference0Target(): String = "callable reference 0"
fun getSuspendCallableReference0(): suspend () -> String = ::suspendCallableReference0Target

fun getSuspendLambda1(): suspend (String) -> String = { "$it 1" }

private suspend fun suspendCallableReference1Target(str: String): String = "$str 1"
fun getSuspendCallableReference1(): suspend (String) -> String = ::suspendCallableReference1Target

suspend fun invoke1(block: suspend (Any?) -> Any?, argument: Any?): Any? = block(argument)

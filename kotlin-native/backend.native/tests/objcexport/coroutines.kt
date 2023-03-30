/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)

package coroutines

import kotlin.coroutines.*
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.intrinsics.*
import kotlin.native.concurrent.isFrozen
import kotlin.native.internal.ObjCErrorException
import kotlin.test.*
import kotlin.reflect.*

class CoroutineException : Throwable()

suspend fun suspendFun() = 42
suspend fun unitSuspendFun() = Unit

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

@Throws(CoroutineException::class, CancellationException::class)
suspend fun unitSuspendFun(doSuspend: Boolean, doThrow: Boolean) {
    if (doSuspend) {
        suspendCoroutineUninterceptedOrReturn<Unit> {
            it.resume(Unit)
            COROUTINE_SUSPENDED
        }
    }

    if (doThrow) throw CoroutineException()
}

class ContinuationHolder<T> {
    internal var continuation: Continuation<T>? = null

    fun resume(value: T) {
        continuation!!.resume(value)
        continuation = null
    }

    fun resumeWithException(exception: Throwable) {
        continuation!!.resumeWithException(exception)
        continuation = null
    }
}

@Throws(CoroutineException::class, CancellationException::class)
suspend fun suspendFunAsync(result: Any?, continuationHolder: ContinuationHolder<Any?>): Any? =
        suspendCoroutineUninterceptedOrReturn<Any?> {
            continuationHolder.continuation = it
            COROUTINE_SUSPENDED
        } ?: result

@Throws(CoroutineException::class, CancellationException::class)
suspend fun unitSuspendFunAsync(continuationHolder: ContinuationHolder<Unit>): Unit =
        suspendCoroutineUninterceptedOrReturn<Unit> {
            continuationHolder.continuation = it
            COROUTINE_SUSPENDED
        }

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
    suspend fun nullableUnit(value: T): Unit?

    @Throws(Throwable::class) suspend fun nothing(value: T): Nothing
    @Throws(Throwable::class) suspend fun nothingAsInt(value: T): Int
    @Throws(Throwable::class) suspend fun nothingAsAny(value: T): Any?
    @Throws(Throwable::class) suspend fun nothingAsUnit(value: T): Unit
}

abstract class AbstractSuspendBridge : SuspendBridge<Int> {
    override suspend fun intAsAny(value: Int): Int = TODO()

    override suspend fun unit(value: Int): Unit = TODO()
    override suspend fun unitAsAny(value: Int): Unit = TODO()
    override suspend fun nullableUnit(value: Int): Unit? = TODO()

    override suspend fun nothingAsInt(value: Int): Nothing = TODO()
    override suspend fun nothingAsAny(value: Int): Nothing = TODO()
    override suspend fun nothingAsUnit(value: Int): Nothing = TODO()
}

private suspend fun callSuspendBridgeImpl(bridge: SuspendBridge<Int>) {
    assertEquals(1, bridge.intAsAny(1))

    assertSame(Unit, bridge.unit(2))
    assertSame(Unit, bridge.unitAsAny(3))
    assertSame(Unit, bridge.nullableUnit(4))

    assertFailsWith<ObjCErrorException> { bridge.nothingAsInt(5) }
    assertFailsWith<ObjCErrorException> { bridge.nothingAsAny(6) }
    assertFailsWith<ObjCErrorException> { bridge.nothingAsUnit(7) }
}

private suspend fun callAbstractSuspendBridgeImpl(bridge: AbstractSuspendBridge) {
    assertEquals(8, bridge.intAsAny(8))

    assertSame(Unit, bridge.unit(9))
    assertSame(Unit, bridge.unitAsAny(10))
    assertSame(Unit, bridge.nullableUnit(11))

    assertFailsWith<ObjCErrorException> { bridge.nothingAsInt(12) }
    assertFailsWith<ObjCErrorException> { bridge.nothingAsAny(13) }
    assertFailsWith<ObjCErrorException> { bridge.nothingAsUnit(14) }
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

class suspendFunctionChild0: suspend () -> String {
    override suspend fun invoke(): String = "child 0"
}

class suspendFunctionChild1: suspend (String) -> String {
    override suspend fun invoke(s: String): String = "$s 1"
}

fun getSuspendLambda0(): suspend () -> String = { "lambda 0" }

private suspend fun suspendCallableReference0Target(): String = "callable reference 0"
fun getSuspendCallableReference0(): suspend () -> String = ::suspendCallableReference0Target

fun getSuspendChild0() = suspendFunctionChild0()

fun getSuspendLambda1(): suspend (String) -> String = { "$it 1" }

private suspend fun suspendCallableReference1Target(str: String): String = "$str 1"
fun getSuspendCallableReference1(): suspend (String) -> String = ::suspendCallableReference1Target
fun getSuspendChild1() = suspendFunctionChild1()


suspend fun invoke1(block: suspend (Any?) -> Any?, argument: Any?): Any? = block(argument)

fun getKSuspendCallableReference0(): KSuspendFunction0<String> = ::suspendCallableReference0Target
fun getKSuspendCallableReference1(): KSuspendFunction1<String, String> = ::suspendCallableReference1Target

@Throws(Throwable::class)
fun startCoroutineUninterceptedOrReturn(fn: suspend () -> Any?, resultHolder: ResultHolder<Any?>) =
        fn.startCoroutineUninterceptedOrReturn(ResultHolderCompletion(resultHolder))

@Throws(Throwable::class)
fun startCoroutineUninterceptedOrReturn(fn: suspend Any?.() -> Any?, receiver: Any?, resultHolder: ResultHolder<Any?>) =
        fn.startCoroutineUninterceptedOrReturn(receiver, ResultHolderCompletion(resultHolder))

@Throws(Throwable::class)
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun startCoroutineUninterceptedOrReturn(fn: suspend Any?.(Any?) -> Any?, receiver: Any?, param: Any?, resultHolder: ResultHolder<Any?>) =
        fn.startCoroutineUninterceptedOrReturn(receiver, param, ResultHolderCompletion(resultHolder))

@Throws(Throwable::class)
fun createCoroutineUninterceptedAndResume(fn: suspend () -> Any?, resultHolder: ResultHolder<Any?>) =
        fn.createCoroutine(ResultHolderCompletion(resultHolder)).resume(Unit)

@Throws(Throwable::class)
fun createCoroutineUninterceptedAndResume(fn: suspend Any?.() -> Any?, receiver: Any?, resultHolder: ResultHolder<Any?>) =
        fn.createCoroutine(receiver, ResultHolderCompletion(resultHolder)).resume(Unit)

fun gc() = kotlin.native.runtime.GC.collect()

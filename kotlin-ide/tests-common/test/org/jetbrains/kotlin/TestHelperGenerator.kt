/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.test.TargetBackend

// Add the directive `// WITH_COROUTINES` to use these helpers in codegen tests (see TestFiles.java).
fun createTextForCoroutineHelpers(isReleaseCoroutines: Boolean, checkStateMachine: Boolean, checkTailCallOptimization: Boolean): String {
    val coroutinesPackage =
        if (isReleaseCoroutines)
            StandardNames.COROUTINES_PACKAGE_FQ_NAME_RELEASE.asString()
        else
            StandardNames.COROUTINES_PACKAGE_FQ_NAME_EXPERIMENTAL.asString()

    fun continuationBody(t: String, useResult: (String) -> String) =
        if (isReleaseCoroutines)
            """
                |override fun resumeWith(result: Result<$t>) {
                |   ${useResult("result.getOrThrow()")}
                |}
            """.trimMargin()
        else
            """
                |override fun resume(data: $t) { ${useResult("data")} }
                |override fun resumeWithException(exception: Throwable) { throw exception }
            """.trimMargin()

    val handleExceptionContinuationBody =
        if (isReleaseCoroutines)
            """
                |override fun resumeWith(result: Result<Any?>) {
                |   result.exceptionOrNull()?.let(x)
                |}
            """.trimMargin()
        else
            """
                |override fun resumeWithException(exception: Throwable) {
                |   x(exception)
                |}
                |
                |override fun resume(data: Any?) {}
            """.trimMargin()

    val continuationAdapterBody =
        if (isReleaseCoroutines)
            """
                |override fun resumeWith(result: Result<T>) {
                |   if (result.isSuccess) {
                |       resume(result.getOrThrow())
                |   } else {
                |       resumeWithException(result.exceptionOrNull()!!)
                |   }
                |}
                |
                |abstract fun resumeWithException(exception: Throwable)
                |abstract fun resume(value: T)
            """.trimMargin()
        else
            ""

    val checkStateMachineString = """
    class StateMachineCheckerClass {
        private var counter = 0
        var finished = false

        var proceed: () -> Unit = {}

        fun reset() {
            counter = 0
            finished = false
            proceed = {}
        }

        suspend fun suspendHere() = suspendCoroutine<Unit> { c ->
            counter++
            proceed = { c.resume(Unit) }
        }

        fun check(numberOfSuspensions: Int, checkFinished: Boolean = true) {
            for (i in 1..numberOfSuspensions) {
                if (counter != i) error("Wrong state-machine generated: suspendHere should be called exactly once in one state. Expected " + i + ", got " + counter)
                proceed()
            }
            if (counter != numberOfSuspensions)
                error("Wrong state-machine generated: wrong number of overall suspensions. Expected " + numberOfSuspensions + ", got " + counter)
            if (finished) error("Wrong state-machine generated: it is finished early")
            proceed()
            if (checkFinished && !finished) error("Wrong state-machine generated: it is not finished yet")
        }
    }
    val StateMachineChecker = StateMachineCheckerClass()
    object CheckStateMachineContinuation: ContinuationAdapter<Unit>() {
        override val context: CoroutineContext
            get() = EmptyCoroutineContext

        override fun resume(value: Unit) {
            StateMachineChecker.proceed = {
                StateMachineChecker.finished = true
            }
        }

        override fun resumeWithException(exception: Throwable) {
            throw exception
        }
    }
    """.trimIndent()

    // TODO: Find a way to check for tail-call optimization on JS and Native
    val checkTailCallOptimizationString =
        """
        class TailCallOptimizationCheckerClass {
            private val stackTrace = arrayListOf<StackTraceElement?>()

            suspend fun saveStackTrace() = suspendCoroutineUninterceptedOrReturn<Unit> {
                saveStackTrace(it)
            }

            fun saveStackTrace(c: Continuation<*>) {
                if (c !is CoroutineStackFrame) error("Continuation " + c + " is not subtype of CoroutineStackFrame")
                stackTrace.clear()
                var csf: CoroutineStackFrame? = c
                while (csf != null) {
                    stackTrace.add(csf.getStackTraceElement())
                    csf = csf.callerFrame
                }
            }

            fun checkNoStateMachineIn(method: String) {
                stackTrace.find { it?.methodName?.startsWith(method) == true }?.let { error("tail-call optimization miss: method at " + it + " has state-machine " +
                    stackTrace.joinToString(separator = "\n")) }
            }

            fun checkStateMachineIn(method: String) {
                stackTrace.find { it?.methodName?.startsWith(method) == true } ?: error("tail-call optimization hit: method " + method + " has no state-machine " +
                    stackTrace.joinToString(separator = "\n"))
            }
        }

        val TailCallOptimizationChecker = TailCallOptimizationCheckerClass()
        """.trimIndent()

    return """
            |package helpers
            |import $coroutinesPackage.*
            |import $coroutinesPackage.intrinsics.*
            |${if (checkTailCallOptimization) "import $coroutinesPackage.jvm.internal.*" else ""}
            |
            |fun <T> handleResultContinuation(x: (T) -> Unit): Continuation<T> = object: Continuation<T> {
            |    override val context = EmptyCoroutineContext
            |    ${continuationBody("T") { "x($it)" }}
            |}
            |
            |fun handleExceptionContinuation(x: (Throwable) -> Unit): Continuation<Any?> = object: Continuation<Any?> {
            |    override val context = EmptyCoroutineContext
            |    $handleExceptionContinuationBody
            |}
            |
            |open class EmptyContinuation(override val context: CoroutineContext = EmptyCoroutineContext) : Continuation<Any?> {
            |    companion object : EmptyContinuation()
            |    ${continuationBody("Any?") { it }}
            |}
            |
            |class ResultContinuation : Continuation<Any?> {
            |    override val context = EmptyCoroutineContext
            |    ${continuationBody("Any?") { "this.result = $it" }}
            |
            |    var result: Any? = null
            |}
            |
            |abstract class ContinuationAdapter<in T> : Continuation<T> {
            |    override val context: CoroutineContext = EmptyCoroutineContext
            |    $continuationAdapterBody
            |}
            |
            |${if (checkStateMachine) checkStateMachineString else ""}
            |${if (checkTailCallOptimization) checkTailCallOptimizationString else ""}
        """.trimMargin()
}

// Add the directive `// WITH_HELPERS` to use these helpers in codegen tests (see CodegenTestCase.java).
fun createTextForCodegenTestHelpers(backend: TargetBackend) =
    """
        |package helpers
        |
        |fun isIR() = ${backend.isIR}
    """.trimMargin()

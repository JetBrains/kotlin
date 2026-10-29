/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compiler.plugins

import org.jetbrains.kotlin.TestsError
import org.jetbrains.kotlin.js.engine.ScriptExecutionException
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.directives.assertEqualsToDump
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestFailureSuppressor
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.defaultsProvider
import org.jetbrains.kotlin.test.services.moduleStructure
import org.opentest4j.AssertionFailedError
import org.opentest4j.MultipleFailuresError
import java.lang.reflect.InvocationTargetException

object PluginInteractionDirectives : SimpleDirectivesContainer() {
    val EXPECT_EXCEPTION by enumDirective<TargetBackend>(
        """
        The test is expected to crash with an exception.
        The root cause of each exception is written to the `<test>.exception.txt` file and compared with it.
        If the dump matches, the exceptions are suppressed.
        Failed checks of the test infrastructure (for example, a wrong `box()` result) are never suppressed.
        Exceptions thrown by the `box()` function are never suppressed.
        Temporary: use it only until the exception becomes a proper diagnostic.
        """.trimIndent()
    )
}

/**
 * Supports [PluginInteractionDirectives.EXPECT_EXCEPTION].
 *
 * The suppressor sorts each failure by its innermost cause:
 * - A failed check of the test infrastructure: an `opentest4j` assertion error, which `testServices.assertions` throws.
 *   These failures stay, so a matching dump cannot hide a wrong `box()` result or a wrong diagnostics dump.
 * - An exception thrown by the `box()` function on the JVM. These failures stay, because the dump is only for compiler crashes.
 * - An exception: all other throwables, including a plain [AssertionError] from an `assert` call in the compiler.
 *   These failures go to the dump.
 *
 * @param tolerateMissingException If true, a missing exception isn't a failure.
 */
class ExpectedExceptionSuppressor(
    testServices: TestServices,
    private val tolerateMissingException: Boolean = false,
) : TestFailureSuppressor(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(PluginInteractionDirectives)

    private val isEnabled: Boolean
        get() = PluginInteractionDirectives.EXPECT_EXCEPTION in testServices.moduleStructure.allDirectives

    private val expectedBackends: Set<TargetBackend>
        get() = testServices.moduleStructure.allDirectives[PluginInteractionDirectives.EXPECT_EXCEPTION].toSet()

    private val currentBackendIsExpectedToThrow: Boolean
        get() {
            val backends = expectedBackends
            if (TargetBackend.ANY in backends) return true
            var current = testServices.defaultsProvider.targetBackend ?: return false
            while (current != TargetBackend.ANY) {
                if (current in backends) return true
                current = current.compatibleWith
            }
            return false
        }

    private val dumpExtension: String get() = "exception.txt"

    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        if (!isEnabled) return failedAssertions
        if (!currentBackendIsExpectedToThrow) return failedAssertions

        val [checkFailures, exceptions] = failedAssertions.partition {
            it.rootCause().isTestInfrastructureCheck() || it.isThrownByBox()
        }
        if (exceptions.isEmpty()) return failedAssertions

        val dump = exceptions
            .map { it.rootCause().render() }
            .distinct()

        return try {
            testServices.assertions.assertEqualsToDump(testServices.moduleStructure, dumpExtension, dump.joinToString("\n"))
            checkFailures
        } catch (e: Throwable) {
            failedAssertions + e.wrap()
        }
    }

    override fun checkIfTestShouldBeUnmuted() {
        if (!isEnabled) return
        if (tolerateMissingException) return
        if (!currentBackendIsExpectedToThrow) return

        throw AssertionError(
            "Test has the ${PluginInteractionDirectives.EXPECT_EXCEPTION} directive, but no exception was thrown. " +
                    "Remove the directive and the $dumpExtension file"
        )
    }

    private fun Throwable.rootCause(): Throwable = causeChain().last()

    /** This throwable and all its causes, from the outermost to the innermost. */
    private fun Throwable.causeChain(): Sequence<Throwable> = sequence {
        var current = this@causeChain
        while (true) {
            // `TestsError` hides the wrapped exception, but returns the cause of that exception.
            if (current is TestsError) current = current.original
            yield(current)
            val cause = current.cause ?: break
            if (cause === current) break
            current = cause
        }
    }

    private fun Throwable.isThrownByBox(): Boolean = causeChain().any { t ->
        t is InvocationTargetException && t.stackTrace.any { it.methodName == BOX_RUNNER_METHOD_NAME }
                || t is ScriptExecutionException
    }

    private fun Throwable.isTestInfrastructureCheck(): Boolean =
        this is AssertionFailedError || this is MultipleFailuresError

    private fun Throwable.render(): String {
        val message = message?.trim()?.takeIf { it.isNotEmpty() }?.let(::sanitize)
        return listOfNotNull(this::class.java.name, message).joinToString(": ")
    }

    /**
     * Removes the parts of the message that change from run to run:
     * - stack traces in the message (a JS engine error contains the JS stack trace and the executed code),
     * - temporary directories,
     * - identity hash codes.
     */
    private fun sanitize(message: String): String =
        message.lineSequence()
            .takeWhile { !STACK_TRACE_START_REGEX.matches(it) }
            .joinToString("\n")
            .trim()
            .replace(TEMPORARY_PATH_REGEX, "$1")
            .replace(HASH_CODE_REGEX, "@<hash>")

    private companion object {
        const val BOX_RUNNER_METHOD_NAME = "runBoxInCurrentProcess"
        val TEMPORARY_PATH_REGEX = Regex("""(?:/[^/\s:]+)+/([^/\s:]+\.kt)""")
        val HASH_CODE_REGEX = Regex("""@[0-9a-f]{4,}\b""")
        val STACK_TRACE_START_REGEX = Regex("""\s+at .*|CODE:""")
    }
}

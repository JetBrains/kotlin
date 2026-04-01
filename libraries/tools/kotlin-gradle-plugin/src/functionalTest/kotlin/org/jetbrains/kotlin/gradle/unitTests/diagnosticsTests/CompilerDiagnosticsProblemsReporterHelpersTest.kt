/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.gradle.api.problems.ProblemSpec
import org.gradle.api.problems.Severity
import org.jetbrains.kotlin.buildtools.api.CompilerMessageRenderer
import org.jetbrains.kotlin.gradle.plugin.diagnostics.DiagnosticGroup
import org.jetbrains.kotlin.gradle.plugin.diagnostics.applySourceLocation
import org.jetbrains.kotlin.gradle.plugin.diagnostics.locationLength
import org.jetbrains.kotlin.gradle.plugin.diagnostics.problemId
import org.jetbrains.kotlin.gradle.plugin.diagnostics.toDiagnosticGroup
import org.jetbrains.kotlin.gradle.plugin.diagnostics.toDisplayName
import org.jetbrains.kotlin.gradle.plugin.diagnostics.toGradleSeverity
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class CompilerDiagnosticsProblemsReporterHelpersTest {

    @Test
    fun `severity helpers should map to expected values`() {
        assertEquals("compiler-error", CompilerMessageRenderer.Severity.ERROR.problemId)
        assertEquals("compiler-warning", CompilerMessageRenderer.Severity.WARNING.problemId)
        assertEquals("compiler-info", CompilerMessageRenderer.Severity.INFO.problemId)
        assertEquals("compiler-debug", CompilerMessageRenderer.Severity.DEBUG.problemId)

        assertEquals(DiagnosticGroup.Compiler.Error, CompilerMessageRenderer.Severity.ERROR.toDiagnosticGroup())
        assertEquals(DiagnosticGroup.Compiler.Warning, CompilerMessageRenderer.Severity.WARNING.toDiagnosticGroup())
        assertEquals(DiagnosticGroup.Compiler.Default, CompilerMessageRenderer.Severity.INFO.toDiagnosticGroup())
        assertEquals(DiagnosticGroup.Compiler.Default, CompilerMessageRenderer.Severity.DEBUG.toDiagnosticGroup())

        assertEquals("Kotlin compiler error", CompilerMessageRenderer.Severity.ERROR.toDisplayName())
        assertEquals("Kotlin compiler warning", CompilerMessageRenderer.Severity.WARNING.toDisplayName())
        assertEquals("Kotlin compiler info", CompilerMessageRenderer.Severity.INFO.toDisplayName())
        assertEquals("Kotlin compiler debug", CompilerMessageRenderer.Severity.DEBUG.toDisplayName())

        assertEquals(Severity.ERROR, CompilerMessageRenderer.Severity.ERROR.toGradleSeverity())
        assertEquals(Severity.WARNING, CompilerMessageRenderer.Severity.WARNING.toGradleSeverity())
        assertNull(CompilerMessageRenderer.Severity.INFO.toGradleSeverity())
        assertNull(CompilerMessageRenderer.Severity.DEBUG.toGradleSeverity())
    }

    @Test
    fun `applySourceLocation should preserve ProblemSpec when location is null`() {
        val handler = RecordingProblemSpecHandler()
        val spec = createProblemSpecProxy(handler)

        val result = spec.applySourceLocation(null)

        assertSame(spec, result)
        assertNull(handler.lastInvocation)
    }

    @Test
    fun `applySourceLocation should use fileLocation when no line is available`() {
        val handler = RecordingProblemSpecHandler()
        val spec = createProblemSpecProxy(handler)

        spec.applySourceLocation(CompilerMessageRenderer.SourceLocation("/tmp/src/Main.kt", 0, 0, -1, -1, null))

        assertEquals("fileLocation", handler.lastInvocation?.name)
        assertEquals(listOf("/tmp/src/Main.kt"), handler.lastInvocation?.args)
    }

    @Test
    fun `applySourceLocation should use lineInFileLocation with line only when column is unavailable`() {
        val handler = RecordingProblemSpecHandler()
        val spec = createProblemSpecProxy(handler)

        spec.applySourceLocation(CompilerMessageRenderer.SourceLocation("/tmp/src/Main.kt", 7, 0, -1, -1, null))

        assertEquals("lineInFileLocation", handler.lastInvocation?.name)
        assertEquals(listOf("/tmp/src/Main.kt", 7), handler.lastInvocation?.args)
    }

    @Test
    fun `applySourceLocation should use lineInFileLocation with column and length when line and column are available`() {
        val handler = RecordingProblemSpecHandler()
        val spec = createProblemSpecProxy(handler)

        spec.applySourceLocation(CompilerMessageRenderer.SourceLocation("/tmp/src/Main.kt", 10, 5, 10, 9, null))

        assertEquals("lineInFileLocation", handler.lastInvocation?.name)
        assertEquals(listOf("/tmp/src/Main.kt", 10, 5, 4), handler.lastInvocation?.args)
    }

    @Test
    fun `locationLength should compute expected values across edge cases`() {
        val sameLineRange = CompilerMessageRenderer.SourceLocation("/tmp/src/Main.kt", 10, 5, 10, 9, null)
        val invalidRange = CompilerMessageRenderer.SourceLocation("/tmp/src/Main.kt", 10, 8, 10, 7, null)
        val multiLineRange = CompilerMessageRenderer.SourceLocation("/tmp/src/Main.kt", 10, 8, 11, 1, null)
        val unknownEnd = CompilerMessageRenderer.SourceLocation("/tmp/src/Main.kt", 10, 8, -1, -1, null)

        assertEquals(4, sameLineRange.locationLength)
        assertEquals(1, invalidRange.locationLength)
        assertEquals(1, multiLineRange.locationLength)
        assertEquals(1, unknownEnd.locationLength)
    }

    private fun createProblemSpecProxy(handler: RecordingProblemSpecHandler): ProblemSpec {
        return Proxy.newProxyInstance(
            javaClass.classLoader,
            arrayOf(ProblemSpec::class.java),
            handler,
        ) as ProblemSpec
    }

    private class RecordingProblemSpecHandler : InvocationHandler {
        data class Invocation(val name: String, val args: List<Any?>)

        var lastInvocation: Invocation? = null

        override fun invoke(proxy: Any, method: Method, args: Array<Any?>?): Any? {
            if (method.declaringClass == Any::class.java) {
                return when (method.name) {
                    "toString" -> "RecordingProblemSpec"
                    "hashCode" -> System.identityHashCode(proxy)
                    "equals" -> proxy === args?.firstOrNull()
                    else -> null
                }
            }

            val invocationArgs = args?.toList().orEmpty()
            if (method.name == "lineInFileLocation" || method.name == "fileLocation") {
                lastInvocation = Invocation(method.name, invocationArgs)
            }

            return when (method.returnType) {
                Boolean::class.javaPrimitiveType -> false
                Int::class.javaPrimitiveType -> 0
                Long::class.javaPrimitiveType -> 0L
                Double::class.javaPrimitiveType -> 0.0
                Float::class.javaPrimitiveType -> 0f
                Short::class.javaPrimitiveType -> 0.toShort()
                Byte::class.javaPrimitiveType -> 0.toByte()
                Char::class.javaPrimitiveType -> 0.toChar()
                Void.TYPE -> null
                else -> if (method.returnType.isAssignableFrom(proxy.javaClass)) proxy else null
            }
        }
    }
}

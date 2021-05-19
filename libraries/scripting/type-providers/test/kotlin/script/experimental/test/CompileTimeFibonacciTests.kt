/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.test

import kotlin.script.experimental.api.*
import kotlin.script.experimental.typeProviders.AnnotationBasedTypeProvider
import kotlin.script.experimental.typeProviders.generatedCode.GeneratedCode
import kotlin.script.experimental.typeProviders.generatedCode.impl.*

internal class CompileTimeFibonacciTests : TypeProviderTests(
    "libraries/scripting/type-providers/testData/compileTimeFibonacci"
) {

    fun testSupported() {
        val out = runScriptOrFail("simple.kts", CompileTimeFibonacciTypeProvider)
        val lines = out.lines().filter { it.isNotBlank() }
        assertEquals(lines.count(), 4)
        assertEquals(lines[0], "fib(1)=1")
        assertEquals(lines[1], "fib(2)=1")
        assertEquals(lines[2], "fib(3)=2")
        assertEquals(lines[3], "fib(4)=3")
    }

}

@Target(AnnotationTarget.FILE)
@Repeatable
@Retention(AnnotationRetention.SOURCE)
annotation class Fib(val number: Int)

object CompileTimeFibonacciTypeProvider : AnnotationBasedTypeProvider<Fib> {

    private fun fibUntil(number: Int): List<Int> {
        require(number > 0)
        if (number == 1) {
            return listOf(1)
        }
        if (number == 2) {
            return listOf(1, 1)
        }

        val previous = fibUntil(number - 1)
        return previous + (previous.secondToLast() + previous.last())
    }

    override fun invoke(
        collectedAnnotations: List<ScriptSourceAnnotation<Fib>>,
        context: AnnotationBasedTypeProvider.Context
    ): ResultWithDiagnostics<GeneratedCode> {

        // Validate all annotations
        for ((annotation, location) in collectedAnnotations) {
            if (annotation.number < 1)
                return makeFailureResult("Cannot generate Fibonacci sequence of non-positive length ${annotation.number})", location)
        }

        val fib = collectedAnnotations
            .map { it.annotation }
            .maxByOrNull { it.number } ?: return GeneratedCode.Empty.asSuccess()

        return GeneratedCode {
            fibUntil(fib.number).forEachIndexed { index, value ->
                lazyProperty("FIB_${index + 1}") { value }
            }
        }.asSuccess()
    }

}

private fun <T> List<T>.secondToLast(): T = this[count() - 2]
/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.test

import java.nio.file.Files
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.util.filterByAnnotationType

@KotlinScript(
    fileExtension = "fib.kts",
    compilationConfiguration = CompileTimeFibonacciConfiguration::class
)
abstract class CompileTimeFibonacci

object CompileTimeFibonacciConfiguration : ScriptCompilationConfiguration(
    {
        fun fibUntil(number: Int): List<Int> {
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

        defaultImports(Fib::class)
        jvm {
            dependenciesFromCurrentContext(wholeClasspath = true)
        }
        refineConfiguration {
            onAnnotations(Fib::class) { context: ScriptConfigurationRefinementContext ->
                val maxFibonacciNumber = context
                    .collectedData
                    ?.get(ScriptCollectedData.collectedAnnotations)
                    ?.filterByAnnotationType<Fib>()
                    ?.mapSuccess { (val fib = annotation, val location) ->
                        fib.number.takeIf { it > 0 }?.asSuccess()
                            ?: makeFailureResult(
                                message = "Fibonacci of non-positive numbers like ${fib.number} are not supported",
                                locationWithId = location
                            )
                    }
                    ?.valueOr { return@onAnnotations it }
                    ?.maxOrNull() ?: return@onAnnotations context.compilationConfiguration.asSuccess()

                val sourceCode = fibUntil(maxFibonacciNumber)
                    .mapIndexed { index, number -> "val FIB_${index + 1} = $number" }
                    .joinToString("\n")

                val file = Files.createTempFile("CompileTimeFibonacci", ".fib.kts").toFile()
                    .apply {
                        deleteOnExit()
                        writeText(sourceCode)
                    }

                ScriptCompilationConfiguration(context.compilationConfiguration) {
                    importScripts.append(file.toScriptSource())
                }.asSuccess()
            }
        }
    }
)

@Target(AnnotationTarget.FILE)
@Repeatable
@Retention(AnnotationRetention.SOURCE)
annotation class Fib(val number: Int)

private fun <T> List<T>.secondToLast(): T = this[count() - 2]

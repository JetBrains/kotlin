/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.dokka.kotlinlang

import org.jetbrains.dokka.analysis.kotlin.sample.FunctionCallRewriter
import org.jetbrains.dokka.analysis.kotlin.sample.SampleRewriter
import org.jetbrains.dokka.plugability.DokkaContext

@Suppress("UNUSED_PARAMETER")
class KotlinWebsiteSampleRewriter(ctx: DokkaContext) : SampleRewriter {
    private val importsToIgnore = setOf("samples.*", "samples.Sample")

    private val functionCallRewriters: Map<String, FunctionCallRewriter> = mapOf(
        "assertTrue" to object : FunctionCallRewriter {
            // rewrites `assertTrue(actual)` or `assertTrue(actual, message)
            override fun rewrite(arguments: List<String>, typeArguments: List<String>): String = buildString {
                val actual = arguments[0]
                val message = arguments.getOrNull(1)

                if (message != null) {
                    appendLine("// $message")
                }
                append("println(\"$actual is \${$actual}\") // true")
            }
        },
        "assertFalse" to object : FunctionCallRewriter {
            // rewrites `assertFalse(actual)` or `assertFalse(actual, message)
            override fun rewrite(arguments: List<String>, typeArguments: List<String>): String = buildString {
                val actual = arguments[0]
                val message = arguments.getOrNull(1)

                if (message != null) {
                    appendLine("// $message")
                }
                append("println(\"$actual is \${$actual}\") // false")
            }
        },
        "assertPrints" to object : FunctionCallRewriter {
            // rewrites `assertPrints(expression, expectedOutput)`
            override fun rewrite(arguments: List<String>, typeArguments: List<String>): String {
                val expression = arguments[0]
                val expectedOutput = arguments[1].removeSurrounding("\"")
                return "println($expression) // $expectedOutput"
            }
        },
        "assertFails" to object : FunctionCallRewriter {
            // rewrites `assertFails { body }` or `assertFails(message) { body }
            override fun rewrite(arguments: List<String>, typeArguments: List<String>): String {
                val body = arguments.last().removeSurrounding("{", "}").trim()
                // there is no usage of `assertFails(message) { body }` in current samples,
                // but it was supported in old transformer
                val message = if (arguments.size == 1) "will fail" else arguments[0] + " will fail"
                return "// $body // $message"
            }
        },
        "assertFailsWith" to object : FunctionCallRewriter {
            // rewrites `assertFailsWith<exceptionType> { body }` or `assertFailsWith<exceptionType>(message) { body }
            override fun rewrite(arguments: List<String>, typeArguments: List<String>): String {
                val body = arguments.last().removeSurrounding("{", "}").trim()
                val exceptionType = typeArguments[0]
                // there is no usage of `assertFailsWith<exceptionType>(message) { body }` in current samples,
                // supported for consistency with `assertFails`
                val message = if (arguments.size == 1) "will fail" else arguments[0] + " will fail"
                return "// $body // $message with $exceptionType"
            }
        }
    )

    override fun rewriteImportDirective(importPath: String): String? {
        return if (importPath in importsToIgnore) null else importPath
    }

    override fun getFunctionCallRewriter(name: String): FunctionCallRewriter? {
        return functionCallRewriters[name]
    }
}

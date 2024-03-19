/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.sample

import org.jetbrains.dokka.analysis.kotlin.KotlinAnalysisPlugin
import org.jetbrains.dokka.analysis.kotlin.sample.FunctionCallRewriter
import org.jetbrains.dokka.analysis.kotlin.sample.SampleRewriter
import org.jetbrains.dokka.analysis.kotlin.sample.ShortFunctionName
import org.jetbrains.dokka.analysis.test.api.kotlinJvmTestProject
import org.jetbrains.dokka.analysis.test.api.useServices
import org.jetbrains.dokka.analysis.test.api.util.CollectingDokkaConsoleLogger
import org.jetbrains.dokka.analysis.test.api.util.singleSourceSet
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.PluginApiPreviewAcknowledgement
import kotlin.test.*

class SampleRewriterTest {

    /**
     *  The same plugin rewriter should be used in stdlib
     *
     * For StdLib, Dokka should have a possibility to
     * - rewrite `assertTrue(_)` to `println(_ is _ ${_}) \\ true`.
     * - rewrite `assertFalse(_)` to `println(_ is _ ${_}) \\ false`.
     * - rewrite `assertFails<T>(_)` or `assertFails<T>{ _ }` to `_ \\ will fail with T`
     * - rewrite stdlib's `assertPrints(_)` to `println(_) \\ _`.
     * _Note:_ it would be nice to rewrite it to `for(i in it) { println(i) }`
     * - ignore import directive `samples.*` and  `samples.Sample`
     */
    class SomeSampleRewriterPlugin : DokkaPlugin() {
        @Suppress("UNUSED_PARAMETER")
        class DefaultSampleRewriter(ctx: DokkaContext) : SampleRewriter {
            private val importsToIgnore = arrayOf("samples.*", "samples.Sample")

            private val functionCallRewriters: Map<ShortFunctionName, FunctionCallRewriter> = mapOf(
                "assertTrue" to object : FunctionCallRewriter {
                    override fun rewrite(
                        arguments: List<String>,
                        typeArguments: List<String>
                    ): String {
                        val argument = arguments[0]
                        return "println(\"$argument is \${$argument}\") // true"
                    }
                },
                "assertFalse" to object : FunctionCallRewriter {
                    override fun rewrite(
                        arguments: List<String>,
                        typeArguments: List<String>
                    ): String {
                        val argument = arguments[0]
                        return "println(\"$argument is \${$argument}\") // false"
                    }
                },
                "assertPrints" to object : FunctionCallRewriter {
                    override fun rewrite(
                        arguments: List<String>,
                        typeArguments: List<String>
                    ): String {
                        val argument = arguments[0]
                        val expected = arguments[1].removeSurrounding("\"")
                        return "println($argument) // $expected"
                    }
                },
                "assertFails" to object : FunctionCallRewriter {
                    override fun rewrite(
                        arguments: List<String>,
                        typeArguments: List<String>
                    ): String {
                        val funcBody =
                            (if (arguments.size > 1) arguments.last() else arguments.first()).removeSurrounding(
                                "{",
                                "}"
                            ).trim()
                        val message =
                            if (arguments.size > 1) arguments.first() + " will fail" else "will fail" // in current stdlib, there is no sample with a message, but it was in the old sample transform
                        return "// $funcBody // $message"
                    }
                },
                "assertFailsWith" to object : FunctionCallRewriter {
                    override fun rewrite(
                        arguments: List<String>,
                        typeArguments: List<String>
                    ): String {
                        val funcBody = arguments.first().removeSurrounding("{", "}").trim()
                        val exceptionType = typeArguments.first()
                        return "// $funcBody // will fail with $exceptionType"
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

        private val kotlinAnalysisPlugin by lazy { plugin<KotlinAnalysisPlugin>() }

        @Suppress("unused")
        val stdLibKotlinAnalysis by extending {
            kotlinAnalysisPlugin.sampleRewriter providing ::DefaultSampleRewriter
        }

        @OptIn(DokkaPluginApiPreview::class)
        override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement =
            PluginApiPreviewAcknowledgement
    }

    private val sampleUtilsKT = """
                    import kotlin.test.assertEquals

                    typealias Sample = org.junit.Test
                    typealias RunWith = org.junit.runner.RunWith
                    typealias Enclosed = org.junit.experimental.runners.Enclosed
                    
                    fun assertPrints(expression: Any?, expectedOutput: String) = assertEquals(expectedOutput, expression.toString())
                """
    @Test
    fun `should rewrite assertTrue and assertPrints`() {
        val testProject = kotlinJvmTestProject {
            plugin(SomeSampleRewriterPlugin())
            dokkaConfiguration {
                kotlinSourceSet {
                    samples = setOf("/samples/collections/collections.kt", "/samples/_sampleUtils.kt")
                }
            }
            sampleFile("/samples/_sampleUtils.kt", fqPackageName = "samples") {
                +sampleUtilsKT
            }
            // the sample is from https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/is-not-empty.html
            sampleFile("/samples/collections/collections.kt", fqPackageName = "samples.collections") {
                +"""
                    import samples.*
                    import kotlin.test.*

                    @RunWith(Enclosed::class)
                    class Collections {
                        class Collections {
                    
                            @Sample
                            fun indicesOfCollection() {
                                val empty = emptyList<Any>()
                                assertTrue(empty.indices.isEmpty())
                                val collection = listOf('a', 'b', 'c')
                                assertPrints(collection.indices, "0..2")
                            }
                        }
                    }
                """
            }
        }

        testProject.useServices { context ->
            val sample = sampleAnalysisEnvironmentCreator.use {
                resolveSample(
                    sourceSet = context.singleSourceSet(),
                    fullyQualifiedLink = "samples.collections.Collections.Collections.indicesOfCollection"
                )
            }
            assertNotNull(sample)

            val expectedImports = listOf(
                "kotlin.test.*"
            )

            val expectedBody = "val empty = emptyList<Any>()\n" +
                    "println(\"empty.indices.isEmpty() is \${empty.indices.isEmpty()}\") // true\n" +
                    "val collection = listOf('a', 'b', 'c')\n" +
                    "println(collection.indices) // 0..2"

            assertEquals(expectedImports, sample.imports)
            assertEquals(expectedBody, sample.body)
        }
    }

    @Test
    fun `should rewrite assertFalse and assertPrints`() {
        val testProject = kotlinJvmTestProject {
            plugin(SomeSampleRewriterPlugin())
            dokkaConfiguration {
                kotlinSourceSet {
                    samples = setOf("/samples/collections/collections.kt", "/samples/_sampleUtils.kt")
                }
            }
            sampleFile("/samples/_sampleUtils.kt", fqPackageName = "samples") {
                +sampleUtilsKT
            }
            // the sample is from https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/indices.html
            sampleFile("/samples/collections/collections.kt", fqPackageName = "samples.collections") {
                +"""
                    import samples.*
                    import kotlin.test.*

                    @RunWith(Enclosed::class)
                    class Collections {
                        class Collections {
                    
                            @Sample
                            fun collectionIsNotEmpty() {
                                val empty = emptyList<Any>()
                                assertFalse(empty.isNotEmpty())
                    
                                val collection = listOf('a', 'b', 'c')
                                assertTrue(collection.isNotEmpty())
                            }
                        }
                    }
                """
            }
        }

        testProject.useServices { context ->
            val sample = sampleAnalysisEnvironmentCreator.use {
                resolveSample(
                    sourceSet = context.singleSourceSet(),
                    fullyQualifiedLink = "samples.collections.Collections.Collections.collectionIsNotEmpty"
                )
            }
            assertNotNull(sample)

            val expectedImports = listOf(
                "kotlin.test.*"
            )

            val expectedBody = "val empty = emptyList<Any>()\n" +
                    "println(\"empty.isNotEmpty() is \${empty.isNotEmpty()}\") // false\n" +
                    "\n" +
                    "val collection = listOf('a', 'b', 'c')\n" +
                    "println(\"collection.isNotEmpty() is \${collection.isNotEmpty()}\") // true"

            assertEquals(expectedImports, sample.imports)
            assertEquals(expectedBody, sample.body)
        }
    }


    @Test
    fun `should rewrite assertFails and assertPrints`() {
        val testProject = kotlinJvmTestProject {
            plugin(SomeSampleRewriterPlugin())
            dokkaConfiguration {
                kotlinSourceSet {
                    samples = setOf("/samples/collections/collections.kt", "/samples/_sampleUtils.kt")
                }
            }
            sampleFile("/samples/_sampleUtils.kt", fqPackageName = "samples") {
                +sampleUtilsKT
            }
            // the sample is from https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/last.html
            sampleFile("/samples/collections/collections.kt", fqPackageName = "samples.collections") {
                +"""
                    import samples.*
                    import kotlin.test.*

                    @RunWith(Enclosed::class)
                    class Collections {
                        class Elements {
                    
                            @Sample
                            fun last() {
                                val list = listOf(1, 2, 3, 4)
                                assertPrints(list.last(), "4")
                                assertPrints(list.last { it % 2 == 1 }, "3")
                                assertPrints(list.lastOrNull { it < 0 }, "null")
                                assertFails { list.last { it < 0 } }
                    
                                val emptyList = emptyList<Int>()
                                assertPrints(emptyList.lastOrNull(), "null")
                                assertFails { emptyList.last() }
                            }
                        }
                    }
                """
            }
        }

        testProject.useServices { context ->
            val sample = sampleAnalysisEnvironmentCreator.use {
                resolveSample(
                    sourceSet = context.singleSourceSet(),
                    fullyQualifiedLink = "samples.collections.Collections.Elements.last"
                )
            }
            assertNotNull(sample)

            val expectedImports = listOf(
                "kotlin.test.*"
            )

            val expectedBody = """
                val list = listOf(1, 2, 3, 4)
                println(list.last()) // 4
                println(list.last { it % 2 == 1 }) // 3
                println(list.lastOrNull { it < 0 }) // null
                // list.last { it < 0 } // will fail

                val emptyList = emptyList<Int>()
                println(emptyList.lastOrNull()) // null
                // emptyList.last() // will fail
            """.trimIndent()

            assertEquals(expectedImports, sample.imports)
            assertEquals(expectedBody, sample.body)
        }
    }

    @Test
    fun `should rewrite assertFailsWith and assertPrints`() {
        val testProject = kotlinJvmTestProject {
            plugin(SomeSampleRewriterPlugin())
            dokkaConfiguration {
                kotlinSourceSet {
                    samples = setOf("/samples/collections/collections.kt", "/samples/_sampleUtils.kt")
                }
            }
            sampleFile("/samples/_sampleUtils.kt", fqPackageName = "samples") {
                +sampleUtilsKT
            }
            // the sample is from https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/element-at.html
            sampleFile("/samples/collections/collections.kt", fqPackageName = "samples.collections") {
                +"""
                    import samples.*
                    import kotlin.test.*

                    @RunWith(Enclosed::class)
                    class Collections {
                        class Elements {
                    
                            @Sample
                            fun elementAt() {
                                val list = listOf(1, 2, 3)
                                assertPrints(list.elementAt(0), "1")
                                assertPrints(list.elementAt(2), "3")
                                assertFailsWith<IndexOutOfBoundsException> { list.elementAt(3) }
                    
                                val emptyList = emptyList<Int>()
                                assertFailsWith<IndexOutOfBoundsException> { emptyList.elementAt(0) }
                            }
                        }
                    }
                """
            }
        }

        testProject.useServices { context ->
            val sample = sampleAnalysisEnvironmentCreator.use {
                resolveSample(
                    sourceSet = context.singleSourceSet(),
                    fullyQualifiedLink = "samples.collections.Collections.Elements.elementAt"
                )
            }
            assertNotNull(sample)

            val expectedImports = listOf(
                "kotlin.test.*"
            )

            val expectedBody = """
                    val list = listOf(1, 2, 3)
                    println(list.elementAt(0)) // 1
                    println(list.elementAt(2)) // 3
                    // list.elementAt(3) // will fail with IndexOutOfBoundsException
                    
                    val emptyList = emptyList<Int>()
                    // emptyList.elementAt(0) // will fail with IndexOutOfBoundsException
            """.trimIndent()

            assertEquals(expectedImports, sample.imports)
            assertEquals(expectedBody, sample.body)
        }
    }

    @Test
    fun `should rewrite call of assertFalse with fully qualified name`() {
        val testProject = kotlinJvmTestProject {
            plugin(SomeSampleRewriterPlugin())
            dokkaConfiguration {
                kotlinSourceSet {
                    samples = setOf("/samples/collections/collections.kt")
                }
            }

            sampleFile("/samples/collections/collections.kt", fqPackageName = "samples.collections") {
                +"""
                    import samples.*
                    import kotlin.test.*

                    @RunWith(Enclosed::class)
                    class Collections {
                            fun someSample() {
                                kotlin.test.assertFalse(empty.isNotEmpty())
                            }
                        }
                    }
                """
            }
        }
        testProject.useServices { context ->
            val sample = sampleAnalysisEnvironmentCreator.use {
                resolveSample(
                    sourceSet = context.singleSourceSet(),
                    fullyQualifiedLink = "samples.collections.Collections.someSample"
                )
            }
            assertNotNull(sample)

            val expectedImports = listOf(
                "kotlin.test.*"
            )

            val expectedBody = "kotlin.test.println(\"empty.isNotEmpty() is \${empty.isNotEmpty()}\") // false"

            assertEquals(expectedImports, sample.imports)
            assertEquals(expectedBody, sample.body)
        }
    }

    class ConstructorSampleRewriterPlugin : DokkaPlugin() {
        @Suppress("UNUSED_PARAMETER")
        class ConstructorSampleRewriter(ctx: DokkaContext) : SampleRewriter {

            private val functionCallRewriters: Map<ShortFunctionName, FunctionCallRewriter> = mapOf(
                "IntArray" to object : FunctionCallRewriter {
                    override fun rewrite(
                        arguments: List<String>,
                        typeArguments: List<String>
                    ): String {
                        val argument = arguments[0]
                        return "arrayOf(${"0,".repeat(argument.toInt())})"
                    }
                }
            )

            override fun getFunctionCallRewriter(name: String): FunctionCallRewriter? {
                return functionCallRewriters[name]
            }
        }

        private val kotlinAnalysisPlugin by lazy { plugin<KotlinAnalysisPlugin>() }

        @Suppress("unused")
        val stdLibKotlinAnalysis by extending {
            kotlinAnalysisPlugin.sampleRewriter providing ::ConstructorSampleRewriter
        }

        @OptIn(DokkaPluginApiPreview::class)
        override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement =
            PluginApiPreviewAcknowledgement
    }

    @Test
    fun `should rewrite call of constructor`() {
        val testProject = kotlinJvmTestProject {
            plugin(ConstructorSampleRewriterPlugin())
            dokkaConfiguration {
                kotlinSourceSet {
                    samples = setOf("/samples/collections/collections.kt")
                }
            }

            sampleFile("/samples/collections/collections.kt", fqPackageName = "samples.collections") {
                +"""                    import kotlin.test.*

                    class Collections {
                            fun someSample() {
                                IntArray(10)
                            }
                        }
                    }
                """
            }
        }
        testProject.useServices { context ->
            val sample = sampleAnalysisEnvironmentCreator.use {
                resolveSample(
                    sourceSet = context.singleSourceSet(),
                    fullyQualifiedLink = "samples.collections.Collections.someSample"
                )
            }
            assertNotNull(sample)

            val expectedImports = listOf(
                "kotlin.test.*"
            )

            val expectedBody = "arrayOf(0,0,0,0,0,0,0,0,0,0,)"

            assertEquals(expectedImports, sample.imports)
            assertEquals(expectedBody, sample.body)
        }
    }



    class ExceptionSampleRewriterPlugin : DokkaPlugin() {
        @Suppress("UNUSED_PARAMETER")
        class ConstructorSampleRewriter(ctx: DokkaContext) : SampleRewriter {

            private val functionCallRewriters: Map<ShortFunctionName, FunctionCallRewriter> = mapOf(
                "IntArray" to object : FunctionCallRewriter {
                    override fun rewrite(
                        arguments: List<String>,
                        typeArguments: List<String>
                    ): String {
                        throw IllegalStateException("error text")
                    }
                }
            )

            override fun getFunctionCallRewriter(name: String): FunctionCallRewriter? {
                return functionCallRewriters[name]
            }
        }

        private val kotlinAnalysisPlugin by lazy { plugin<KotlinAnalysisPlugin>() }

        @Suppress("unused")
        val stdLibKotlinAnalysis by extending {
            kotlinAnalysisPlugin.sampleRewriter providing ::ConstructorSampleRewriter
        }

        @OptIn(DokkaPluginApiPreview::class)
        override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement =
            PluginApiPreviewAcknowledgement
    }

    @Test
    fun `should print warning`() {
        val testProject = kotlinJvmTestProject {
            plugin(ExceptionSampleRewriterPlugin())
            dokkaConfiguration {
                kotlinSourceSet {
                    samples = setOf("/samples/collections/collections.kt")
                }
            }

            sampleFile("/samples/collections/collections.kt", fqPackageName = "samples.collections") {
                +"""                    import kotlin.test.*

                    class Collections {
                            fun someSample() {
                                IntArray(10)
                            }
                        }
                    }
                """
            }
        }
        val logger = CollectingDokkaConsoleLogger()
        testProject.useServices(logger) { context ->
            val sample = sampleAnalysisEnvironmentCreator.use {
                resolveSample(
                    sourceSet = context.singleSourceSet(),
                    fullyQualifiedLink = "samples.collections.Collections.someSample"
                )
            }
            assertNotNull(sample)

            val expectedImports = listOf(
                "kotlin.test.*"
            )

            val expectedBody = "IntArray(10)"

            assertEquals(expectedImports, sample.imports)
            assertEquals(expectedBody, sample.body)
            assertEquals(1, logger.warningsCount)
            assertNotNull(logger.collectedLogMessages.firstOrNull {
                it.startsWith(
                    "Exception thrown while sample rewriting at collections.kt"
                )
            })
        }
    }
}

/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.sample

import org.jetbrains.dokka.analysis.kotlin.sample.SampleSnippet
import org.jetbrains.dokka.analysis.test.api.kotlinJvmTestProject
import org.jetbrains.dokka.analysis.test.api.mixedJvmTestProject
import org.jetbrains.dokka.analysis.test.api.useServices
import org.jetbrains.dokka.analysis.test.api.util.CollectingDokkaConsoleLogger
import org.jetbrains.dokka.analysis.test.api.util.singleSourceSet
import kotlin.test.*

class SampleAnalysisTest {

    @Test
    fun `should resolve a valid sample if set via the samples option`() {
        val testProject = kotlinJvmTestProject {
            dokkaConfiguration {
                kotlinSourceSet {
                    samples = setOf("/samples/collections.kt")
                }
            }
            sampleFile("/samples/collections.kt", fqPackageName = "org.jetbrains.dokka.sample.collections") {
                +"""
                    import org.jetbrains.dokka.DokkaConfiguration
                    import org.jetbrains.dokka.DokkaGenerator
                    import org.jetbrains.dokka.utilities.DokkaLogger
                    
                    fun specificPositionOperations() {
                        val numbers = mutableListOf(1, 2, 3, 4)
                        numbers.add(5)
                        numbers.removeAt(1)
                        numbers[0] = 0
                        numbers.shuffle()
                        if (numbers.size > 0) {
                            println(numbers)
                        }
                    }
                """
            }
        }

        testProject.useServices { context ->
            val sample = sampleAnalysisEnvironmentCreator.use {
                resolveSample(
                    sourceSet = context.singleSourceSet(),
                    fullyQualifiedLink = "org.jetbrains.dokka.sample.collections.specificPositionOperations"
                )
            }
            assertNotNull(sample)

            val expectedImports = listOf(
                "org.jetbrains.dokka.DokkaConfiguration",
                "org.jetbrains.dokka.DokkaGenerator",
                "org.jetbrains.dokka.utilities.DokkaLogger"
            )

            val expectedBody = """
                val numbers = mutableListOf(1, 2, 3, 4)
                numbers.add(5)
                numbers.removeAt(1)
                numbers[0] = 0
                numbers.shuffle()
                if (numbers.size > 0) {
                    println(numbers)
                }
            """.trimIndent()

            assertEquals(expectedImports, sample.imports)
            assertEquals(expectedBody, sample.body)
        }
    }

    @Test
    fun `should resolve a valid sample if set via the additionalSourceRoots option`() {
        val testProject = kotlinJvmTestProject {
            dokkaConfiguration {
                kotlinSourceSet {
                    additionalSourceRoots = setOf("/samples")
                }
            }
            sampleFile("/samples/collections.kt", fqPackageName = "org.jetbrains.dokka.sample.collections") {
                +"""
                    import org.jetbrains.dokka.DokkaConfiguration
                    import org.jetbrains.dokka.DokkaGenerator
                    import org.jetbrains.dokka.utilities.DokkaLogger
                    
                    fun specificPositionOperations() {
                        val numbers = mutableListOf(1, 2, 3, 4)
                        numbers.add(5)
                        numbers.removeAt(1)
                        numbers[0] = 0
                        numbers.shuffle()
                        if (numbers.size > 0) {
                            println(numbers)
                        }
                    }
                """
            }
        }

        testProject.useServices { context ->
            val sample = sampleAnalysisEnvironmentCreator.use {
                resolveSample(
                    sourceSet = context.singleSourceSet(),
                    fullyQualifiedLink = "org.jetbrains.dokka.sample.collections.specificPositionOperations"
                )
            }
            assertNotNull(sample)

            val expectedImports = listOf(
                "org.jetbrains.dokka.DokkaConfiguration",
                "org.jetbrains.dokka.DokkaGenerator",
                "org.jetbrains.dokka.utilities.DokkaLogger"
            )

            val expectedBody = """
                val numbers = mutableListOf(1, 2, 3, 4)
                numbers.add(5)
                numbers.removeAt(1)
                numbers[0] = 0
                numbers.shuffle()
                if (numbers.size > 0) {
                    println(numbers)
                }
            """.trimIndent()

            assertEquals(expectedImports, sample.imports)
            assertEquals(expectedBody, sample.body)
        }
    }

    @Test
    fun `should resolve a valid sample function that exists in the main source set`() {
        val testProject = kotlinJvmTestProject {
            ktFile("org/jetbrains/dokka/test/MyKotlinFile.kt") {
                +"""
                    import org.jetbrains.dokka.DokkaConfiguration
                    
                    fun myAverageTopLevelFunction() {
                        println("hello from the average top level function")
                    }
                """
            }
        }

        testProject.useServices { context ->
            val sample = sampleAnalysisEnvironmentCreator.use {
                resolveSample(context.singleSourceSet(), "org.jetbrains.dokka.test.myAverageTopLevelFunction")
            }
            assertNotNull(sample)

            val expectedImports = listOf("org.jetbrains.dokka.DokkaConfiguration")
            val expectedBody = "println(\"hello from the average top level function\")"

            assertEquals(expectedImports, sample.imports)
            assertEquals(expectedBody, sample.body)
        }
    }

    @Test
    fun `should resolve a valid sample in the root package`() {
        val testProject = kotlinJvmTestProject {
            dokkaConfiguration {
                kotlinSourceSet {
                    samples = setOf("/samples/TopLevelSample.kt")
                }
            }

            sampleFile("/samples/TopLevelSample.kt", fqPackageName = "") {
                +"""
                    import org.jetbrains.dokka.DokkaConfiguration
                    
                    fun foo() {
                        println("hello from the root")
                    }
                """
            }
        }

        testProject.useServices { context ->
            val sample = sampleAnalysisEnvironmentCreator.use {
                resolveSample(context.singleSourceSet(), "foo")
            }
            assertNotNull(sample)

            val expectedImports = listOf("org.jetbrains.dokka.DokkaConfiguration")
            val expectedBody = "println(\"hello from the root\")"

            assertEquals(expectedImports, sample.imports)
            assertEquals(expectedBody, sample.body)
        }
    }

    @Test
    fun `should resolve a valid sample function from a class in the root package`() {
        val testProject = kotlinJvmTestProject {
            dokkaConfiguration {
                kotlinSourceSet {
                    samples = setOf("/samples/RootClassSample.kt")
                }
            }

            sampleFile("/samples/RootClassSample.kt", fqPackageName = "") {
                +"""
                    import org.jetbrains.dokka.DokkaConfiguration
                    
                    class RootClass {
                        fun foo() {
                            println("hello from within a root class")
                        }
                    }
                """
            }
        }

        testProject.useServices { context ->
            val sample = sampleAnalysisEnvironmentCreator.use {
                resolveSample(context.singleSourceSet(), "RootClass.foo")
            }
            assertNotNull(sample)

            val expectedImports = listOf("org.jetbrains.dokka.DokkaConfiguration")
            val expectedBody = "println(\"hello from within a root class\")"

            assertEquals(expectedImports, sample.imports)
            assertEquals(expectedBody, sample.body)
        }
    }

    @Test
    fun `should resolve a valid sample function from a class`() {
        val testProject = kotlinJvmTestProject {
            dokkaConfiguration {
                kotlinSourceSet {
                    samples = setOf("/samples/SampleWithinClass.kt")
                }
            }

            sampleFile("/samples/SampleWithinClass.kt", fqPackageName = "samples") {
                +"""
                    import org.jetbrains.dokka.DokkaConfiguration
                    
                    package samples
                    
                    class SampleWithinClass {
                        fun foo() {
                            println("hello from within a class")
                        }
                    }
                """
            }
        }

        testProject.useServices { context ->
            val sample = sampleAnalysisEnvironmentCreator.use {
                resolveSample(context.singleSourceSet(), "samples.SampleWithinClass.foo")
            }
            assertNotNull(sample)

            val expectedImports = listOf("org.jetbrains.dokka.DokkaConfiguration")
            val expectedBody = "println(\"hello from within a class\")"

            assertEquals(expectedImports, sample.imports)
            assertEquals(expectedBody, sample.body)
        }
    }

    @Test
    fun `should return null for non-existing sample`() {
        val testProject = kotlinJvmTestProject {
            // nothing
        }

        testProject.useServices { context ->
            val nonExistingSample = sampleAnalysisEnvironmentCreator.use {
                resolveSample(context.singleSourceSet(), "com.example.non.existing.sampleFunction")
            }

            assertNull(nonExistingSample)
        }
    }

    @Test
    fun `should return null if sample is resolved just by class name`() {
        val testProject = kotlinJvmTestProject {
            dokkaConfiguration {
                kotlinSourceSet {
                    samples = setOf("/samples/FooSampleFile.kt")
                }
            }
            sampleFile("/samples/FooSampleFile.kt", fqPackageName = "org.jetbrains.dokka.sample") {
                +"""
                    import org.jetbrains.dokka.DokkaConfiguration
                    
                    fun topLevelFunction() {}
                    
                    class FooSampleClass {
                        fun foo() {
                            println("foo")
                        }
                    }
                """
            }
        }

        val collectingLogger = CollectingDokkaConsoleLogger()
        testProject.useServices(collectingLogger) { context ->
            val sampleByClassName = sampleAnalysisEnvironmentCreator.use {
                resolveSample(context.singleSourceSet(), "org.jetbrains.dokka.sample.FooSampleClass")
            }
            assertNull(sampleByClassName)
        }

        val containsNonKotlinSampleLinkLog = collectingLogger.collectedLogMessages.contains(
            "Unable to process a @sample link: \"org.jetbrains.dokka.sample.FooSampleClass\". " +
                    "Only function links allowed."
        )
        assertTrue(containsNonKotlinSampleLinkLog)
    }

    @Test
    fun `should return null if trying to resolve a non-kotlin sample link`() {
        val testProject = mixedJvmTestProject {
            kotlinSourceDirectory {
                javaFile("org/jetbrains/test/sample/JavaClass.java") {
                    +"""
                        public class JavaClass {
                            public void foo() {
                                System.out.println("foo");
                            }
                        }
                    """
                }
                ktFile("org/jetbrains/test/sample/KotlinFile.kt") {
                    +"""
                        fun foo() {}
                    """
                }
            }
        }

        val collectingLogger = CollectingDokkaConsoleLogger()
        testProject.useServices(collectingLogger) { context ->
            sampleAnalysisEnvironmentCreator.use {
                val kotlinSourceSet = context.singleSourceSet()

                val byClassName = resolveSample(kotlinSourceSet, "org.jetbrains.test.sample.JavaClass")
                assertNull(byClassName)

                val byClassFunctionName = resolveSample(kotlinSourceSet, "org.jetbrains.test.sample.JavaClass.foo")
                assertNull(byClassFunctionName)
            }
        }

        val containsNonKotlinSampleLinkLog = collectingLogger.collectedLogMessages.contains(
            "Unable to resolve non-Kotlin @sample links: \"org.jetbrains.test.sample.JavaClass\""
        )
        assertTrue(containsNonKotlinSampleLinkLog)
    }

    @Test
    fun `should filter out empty import statement lines`() {
        val testProject = kotlinJvmTestProject {
            ktFile("org/jetbrains/dokka/test/MyKotlinFile.kt") {
                +"""
                    import org.jetbrains.dokka.DokkaConfiguration
                    
                    import org.jetbrains.dokka.DokkaGenerator
                    
                    import org.jetbrains.dokka.utilities.DokkaLogger
                    
                    fun sample() {
                        println("hello from sample")
                    }
                """
            }
        }

        testProject.useServices { context ->
            val sample = sampleAnalysisEnvironmentCreator.use {
                resolveSample(context.singleSourceSet(), "org.jetbrains.dokka.test.sample")
            }
            assertNotNull(sample)

            val expectedImports = listOf(
                "org.jetbrains.dokka.DokkaConfiguration",
                "org.jetbrains.dokka.DokkaGenerator",
                "org.jetbrains.dokka.utilities.DokkaLogger",
            )
            val expectedBody = "println(\"hello from sample\")"

            assertEquals(expectedImports, sample.imports)
            assertEquals(expectedBody, sample.body)
        }
    }

    @Test
    fun `should return an empty list of imports if sample file has none`() {
        val testProject = kotlinJvmTestProject {
            ktFile("org/jetbrains/dokka/test/MyKotlinFile.kt") {
                +"""
                    fun sample() {
                        println("hello from sample")
                    }
                """
            }
        }

        testProject.useServices { context ->
            val sample = sampleAnalysisEnvironmentCreator.use {
                resolveSample(context.singleSourceSet(), "org.jetbrains.dokka.test.sample")
            }
            assertNotNull(sample)

            assertTrue(sample.imports.isEmpty())

            val expectedBody = "println(\"hello from sample\")"
            assertEquals(expectedBody, sample.body)

        }
    }

    @Test
    fun `should filter out leading and trailing line breaks`() {
        val testProject = kotlinJvmTestProject {
            ktFile("org/jetbrains/dokka/test/MyKotlinFile.kt") {
                +"""
                    fun sample() {
                    
                        
                        println("hello from sample")
                        
                        
                        
                    }
                """
            }
        }

        testProject.useServices { context ->
            val sample = sampleAnalysisEnvironmentCreator.use {
                resolveSample(context.singleSourceSet(), "org.jetbrains.dokka.test.sample")
            }
            assertNotNull(sample)

            val expectedBody = "println(\"hello from sample\")"
            assertEquals(expectedBody, sample.body)
        }
    }

    @Test
    fun `should filter out trailing whitespace`() {
        val testProject = kotlinJvmTestProject {
            ktFile("org/jetbrains/dokka/test/MyKotlinFile.kt") {
                +"""
                    fun sample() {
                        println("hello from sample")                              
                    }
                """
            }
        }

        testProject.useServices { context ->
            val sample = sampleAnalysisEnvironmentCreator.use {
                resolveSample(context.singleSourceSet(), "org.jetbrains.dokka.test.sample")
            }
            assertNotNull(sample)

            val expectedBody = "println(\"hello from sample\")"
            assertEquals(expectedBody, sample.body)
        }
    }

    @Test
    fun `should see two identical snippets as equal`() {
        val firstSnippet = createHardcodedSnippet()
        val secondSnippet = createHardcodedSnippet()

        assertEquals(firstSnippet, secondSnippet)
    }

    @Test
    fun `should return same hashcode for two equal sample snippets`() {
        val firstSnippet = createHardcodedSnippet()
        val secondSnippet = createHardcodedSnippet()

        assertEquals(firstSnippet.hashCode(), secondSnippet.hashCode())
    }

    private fun createHardcodedSnippet(): SampleSnippet {
        return SampleSnippet(
            imports = listOf(
                "org.jetbrains.dokka.DokkaConfiguration",
                "org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet",
            ),
            body = """
                class Foo {
                    fun bar(): String = TODO()
                }
            """.trimIndent()
        )
    }

    @Test
    @Ignore // TODO [beresnev] should be implemented when there's api for KMP projects
    fun `should return null for existing sample when resolving with the wrong source set`() {}
}

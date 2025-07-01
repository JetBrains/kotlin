/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.kotlin.dsl.provideDelegate
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.internal.dsl.KotlinMultiplatformSourceSetConventionsImpl.jvmMain
import org.jetbrains.kotlin.gradle.internal.dsl.KotlinMultiplatformSourceSetConventionsImpl.jvmTest
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.configurationResult
import org.jetbrains.kotlin.gradle.util.assertContainsDependencies
import org.jetbrains.kotlin.gradle.util.assertNoDiagnostics
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.runLifecycleAwareTest
import org.jetbrains.kotlin.tooling.core.extrasReadWriteProperty
import org.jetbrains.kotlin.util.assertDoesNotThrow
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class KotlinMultiplatformSourceSetConventionsTest {
    private var KotlinSourceSet.testMarker by extrasReadWriteProperty<String>("testMarker")

    @Test
    fun `test - invoke`() = buildProjectWithMPP().runLifecycleAwareTest {

        val mainConfigurationCalled = AtomicBoolean(false)
        val testConfigurationCalled = AtomicBoolean(false)

        multiplatformExtension.apply {
            jvm()

            sourceSets.jvmMain {
                testMarker = "jvmMain"
                assertFalse(mainConfigurationCalled.getAndSet(true))
            }

            sourceSets.jvmTest {
                testMarker = "jvmTest"
                assertFalse(testConfigurationCalled.getAndSet(true))
            }

            configurationResult.await()
            assertEquals("jvmMain", sourceSets.jvmMain.get().testMarker)
            assertEquals("jvmTest", sourceSets.jvmTest.get().testMarker)
        }
    }

    @Test
    fun `test - invoke - allows creating new source set in closure`() {
        val project = buildProjectWithMPP()
        project.multiplatformExtension.apply {
            sourceSets.jvmMain {
                /*
                When done wrong, expect:
                org.gradle.api.internal.AbstractMutationGuard$IllegalMutationException:
                    NamedDomainObjectContainer#create(String) on KotlinSourceSet container cannot be executed in the current context
                 */
                dependsOn(sourceSets.create("foo"))
            }

            assertEquals(setOf("foo"), sourceSets.jvmMain.get().dependsOn.map { it.name }.toSet())
        }
    }

    @Test
    fun `test - languageSettings`() {
        val project = buildProjectWithMPP()
        project.runLifecycleAwareTest {
            multiplatformExtension.apply {
                jvm()

                sourceSets.jvmMain.languageSettings {
                    optIn("jvmMain.optIn")
                }

                sourceSets.jvmTest.languageSettings {
                    this.optIn("jvmTest.optIn")
                }
            }
        }

        assertEquals(
            setOf("jvmMain.optIn"),
            project.multiplatformExtension.sourceSets.jvmMain.get().languageSettings.optInAnnotationsInUse
        )
        assertEquals(
            setOf("jvmTest.optIn"),
            project.multiplatformExtension.sourceSets.jvmTest.get().languageSettings.optInAnnotationsInUse
        )
    }

    @Test
    fun `test - dependencies`() = buildProjectWithMPP().runLifecycleAwareTest {
        multiplatformExtension.apply {
            jvm()

            sourceSets.jvmMain.dependencies {
                implementation("my:jvmMain:library")
            }

            sourceSets.jvmTest.dependencies {
                implementation("my:jvmTest:library")
            }

            assertContainsDependencies(
                sourceSets.jvmMain.get().implementationConfigurationName,
                "my:jvmMain:library"
            )

            assertContainsDependencies(
                sourceSets.jvmTest.get().implementationConfigurationName,
                "my:jvmTest:library"
            )
        }
    }

    @Test
    fun `test - KMP default SourceSets dsl`() {
        val project = buildProjectWithMPP {
            with(multiplatformExtension) {
                jvm("jvm6")

                js("nodeJs") {
                    nodejs()
                }
                @OptIn(ExperimentalWasmDsl::class)
                wasmJs {
                    nodejs()
                }

                linuxX64("linux64")
                mingwX64("mingw64")
                macosX64("macos64")
                macosArm64("macosArm64")

                applyDefaultHierarchyTemplate()
            }
        }

        data class SourceSetDetails(
            val target: String,
            val compilation: String,
            val sourceSet: String,
        ) : Comparable<SourceSetDetails> {
            override fun toString(): String =
                """
                target      : $target
                compilation : $compilation
                sourceSet   : $sourceSet
                """.trimIndent()

            override fun compareTo(other: SourceSetDetails): Int = toString().compareTo(other.toString())
        }

        fun getCompilationDetails(): List<SourceSetDetails> =
            project.multiplatformExtension.targets
                .flatMap { target ->
                    target.compilations.map { compilation ->
                        SourceSetDetails(
                            target = compilation.target.name,
                            compilation = compilation.name,
                            sourceSet = compilation.defaultSourceSet.name,
                        )
                    }
                }

        val actualDetailsBeforeEvaluate = getCompilationDetails()
        val expectedDetailsBeforeEvaluate: List<SourceSetDetails> =
            mutableListOf<SourceSetDetails>().apply {
                add(SourceSetDetails("metadata", "main", "commonMain"))
                listOf(
                    "jvm6", "nodeJs", "mingw64", "linux64", "macos64", "macosArm64", "wasmJs"
                ).forEach { target ->
                    add(SourceSetDetails(target, "main", "${target}Main"))
                    add(SourceSetDetails(target, "test", "${target}Test"))
                }
            }

        assertEquals(
            expectedDetailsBeforeEvaluate.sorted(),
            actualDetailsBeforeEvaluate.sorted(),
        )

        project.evaluate()

        val compilationDetailsAfterEvaluate = getCompilationDetails()
        val expectedDetailsAfterEvaluate: List<SourceSetDetails> =
            mutableListOf<SourceSetDetails>().apply {
                addAll(expectedDetailsBeforeEvaluate)
                listOf(
                    "commonMain", "appleMain", "macosMain", "nativeMain", "webMain",
                ).forEach { target ->
                    add(SourceSetDetails("metadata", target, target))
                }
            }

        assertEquals(
            expectedDetailsAfterEvaluate.sorted(),
            compilationDetailsAfterEvaluate.sorted(),
        )
    }

    @Test
    fun `test - register new source set and access convention one from configure block - commonMain`() {
        val project = buildProjectWithMPP()
        project.multiplatformExtension.apply {
            jvm()
            linuxX64()

            assertDoesNotThrow {
                sourceSets.register("fooBar") {
                    it.dependsOn(sourceSets.commonMain.get())
                }
            }
        }

        project.evaluate()
    }

    @Test
    fun `test - register new source set and access convention one from configure block - linuxMain before creation`() {
        val project = buildProjectWithMPP()
        project.multiplatformExtension.apply {
            val error = assertFailsWith<IllegalStateException> {
                sourceSets.register("fooBar") {
                    it.dependsOn(sourceSets.linuxMain.get())
                }
            }
            assertEquals(
                "Could not create domain object 'fooBar' (KotlinSourceSet)",
                error.message
            )

            assertEquals(
                "Kotlin Source Set 'linuxMain' was attempted to be created during registration or configuration of another source set. " +
                        "Please ensure Kotlin Source Set 'linuxMain' is first accessed outside configuration code block.",
                error.cause!!.message
            )

            jvm()
            linuxX64()
            linuxArm64()
        }

        project.evaluate()
    }

    @Test
    fun `test - create new source set and access convention one from configure block - linuxMain before creation`() {
        val project = buildProjectWithMPP()
        project.multiplatformExtension.apply {
            assertDoesNotThrow {
                sourceSets.create("fooBar") {
                    it.dependsOn(sourceSets.linuxMain.get())
                }
            }

            jvm()
            linuxX64()
            linuxArm64()
        }

        project.evaluate()
    }

    @Test
    fun `test - register new source set and access convention one from configure block - linuxMain after creation`() {
        val project = buildProjectWithMPP()
        project.multiplatformExtension.apply {
            jvm()
            linuxX64()
            linuxArm64()

            assertDoesNotThrow {
                sourceSets.linuxMain.get()
                sourceSets.register("fooBar") {
                    it.dependsOn(sourceSets.linuxMain.get())
                }
            }
        }

        project.evaluate()
    }

    @Test
    fun `test - configure source set and access convention one from configure block - linuxMain after creation`() {
        val project = buildProjectWithMPP()
        project.multiplatformExtension.apply {
            jvm()
            linuxX64()
            linuxArm64()

            val fooBar = sourceSets.register("fooBar")

            assertFailsWith<IllegalStateException> {
                fooBar.configure {
                    it.dependsOn(sourceSets.linuxMain.get())
                }
            }

            sourceSets.linuxMain.get()
            assertDoesNotThrow {
                fooBar.configure {
                    it.dependsOn(sourceSets.linuxMain.get())
                }
            }
        }

        project.evaluate()
    }


    @Test
    fun `test - register new source set and access convention one from configure block - linuxMain after creation but before targets initialization`() {
        val project = buildProjectWithMPP()
        project.multiplatformExtension.apply {
            assertDoesNotThrow {
                sourceSets.linuxMain.get()
                sourceSets.register("fooBar") {
                    it.dependsOn(sourceSets.linuxMain.get())
                }
            }

            jvm()
            linuxX64()
            linuxArm64()
        }

        project.evaluate()
    }

    @OptIn(ExperimentalWasmDsl::class)
    @Test
    fun `test - check all web targets do not throw warning`() {
        val project = buildProjectWithMPP()
        project.multiplatformExtension.apply {
            sourceSets.jsMain.get()
            sourceSets.wasmJsMain.get()
            sourceSets.wasmWasiMain.get()

            js {
                nodejs()
            }
            wasmJs {
                nodejs()
            }
            wasmWasi {
                nodejs()
            }
        }

        project.evaluate()

        project.assertNoDiagnostics()
    }
}

/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName", "DuplicatedCode")
@file:OptIn(ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetHierarchyDescriptor
import org.jetbrains.kotlin.gradle.plugin.mpp.targetHierarchy.buildKotlinTargetHierarchy
import org.jetbrains.kotlin.gradle.plugin.mpp.targetHierarchy.defaultKotlinTargetHierarchy
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.util.*
import kotlin.test.*

class KotlinTargetHierarchyDslTest {


    private val project = buildProjectWithMPP()
    private val kotlin = project.multiplatformExtension


    @Test
    @Suppress("DEPRECATION") // deprecated K/N targets
    fun `test - hierarchy default - targets from all families`() {
        kotlin.apply {
            targetHierarchy.default()
            iosArm32()
            iosArm64()
            iosX64()
            iosSimulatorArm64()

            tvosArm64()
            tvosX64()

            watchosArm32()
            watchosArm64()

            macosX64()
            macosArm64()

            linuxX64()
            linuxArm32Hfp()

            mingwX64()
            mingwX86()

            androidNativeArm32()
            androidNativeArm64()
        }

        assertEquals(
            stringSetOf("nativeMain"),
            kotlin.dependingSourceSetNames("commonMain")
        )

        assertEquals(
            stringSetOf("nativeTest"), kotlin.dependingSourceSetNames("commonTest")
        )

        assertEquals(
            stringSetOf("androidNativeMain", "appleMain", "linuxMain", "mingwMain"),
            kotlin.dependingSourceSetNames("nativeMain")
        )

        assertEquals(
            stringSetOf("androidNativeTest", "appleTest", "linuxTest", "mingwTest"),
            kotlin.dependingSourceSetNames("nativeTest")
        )

        assertEquals(
            stringSetOf("iosMain", "macosMain", "tvosMain", "watchosMain"),
            kotlin.dependingSourceSetNames("appleMain")
        )

        assertEquals(
            stringSetOf("iosTest", "macosTest", "tvosTest", "watchosTest"),
            kotlin.dependingSourceSetNames("appleTest")
        )

        assertEquals(
            stringSetOf("iosArm32Main", "iosArm64Main", "iosSimulatorArm64Main", "iosX64Main"),
            kotlin.dependingSourceSetNames("iosMain")
        )

        assertEquals(
            stringSetOf("iosArm32Test", "iosArm64Test", "iosSimulatorArm64Test", "iosX64Test"),
            kotlin.dependingSourceSetNames("iosTest")
        )

        assertEquals(
            stringSetOf("tvosArm64Main", "tvosX64Main"),
            kotlin.dependingSourceSetNames("tvosMain")
        )

        assertEquals(
            stringSetOf("tvosArm64Test", "tvosX64Test"),
            kotlin.dependingSourceSetNames("tvosTest")
        )

        assertEquals(
            stringSetOf("watchosArm32Main", "watchosArm64Main"),
            kotlin.dependingSourceSetNames("watchosMain")
        )

        assertEquals(
            stringSetOf("watchosArm32Test", "watchosArm64Test"),
            kotlin.dependingSourceSetNames("watchosTest")
        )

        assertEquals(
            stringSetOf("linuxArm32HfpMain", "linuxX64Main"),
            kotlin.dependingSourceSetNames("linuxMain")
        )

        assertEquals(
            stringSetOf("linuxArm32HfpTest", "linuxX64Test"),
            kotlin.dependingSourceSetNames("linuxTest")
        )
    }

    @Test
    fun `test - hierarchy default - only linuxX64`() {
        kotlin.apply {
            targetHierarchy.default()
            kotlin.linuxX64()
        }

        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val commonTest = kotlin.sourceSets.getByName("commonTest")
        val nativeMain = kotlin.sourceSets.getByName("nativeMain")
        val nativeTest = kotlin.sourceSets.getByName("nativeTest")
        val linuxMain = kotlin.sourceSets.getByName("linuxMain")
        val linuxTest = kotlin.sourceSets.getByName("linuxTest")
        val linuxX64Main = kotlin.sourceSets.getByName("linuxX64Main")
        val linuxX64Test = kotlin.sourceSets.getByName("linuxX64Test")

        assertEquals(
            setOf(commonMain, commonTest, nativeMain, nativeTest, linuxMain, linuxTest, linuxX64Main, linuxX64Test),
            kotlin.sourceSets.toSet()
        )
    }

    @Test
    fun `test - hierarchy default - is only applied to main and test compilations`() = project.runLifecycleAwareTest {
        assertNotNull(defaultKotlinTargetHierarchy.buildKotlinTargetHierarchy(kotlin.linuxX64().compilations.main))
        assertNotNull(defaultKotlinTargetHierarchy.buildKotlinTargetHierarchy(kotlin.linuxX64().compilations.test))
        assertNull(defaultKotlinTargetHierarchy.buildKotlinTargetHierarchy(kotlin.linuxX64().compilations.maybeCreate("custom")))

        kotlin.targetHierarchy.default()
        kotlin.linuxX64().compilations.maybeCreate("custom").defaultSourceSet.let { customSourceSet ->
            if (customSourceSet.dependsOn.isNotEmpty()) {
                fail("Expected no dependsOn SourceSets for $customSourceSet (${customSourceSet.dependsOn})")
            }
        }
    }

    @Test
    fun `test - hierarchy - jvm and android`() {
        val project = buildProjectWithMPP {
            setMultiplatformAndroidSourceSetLayoutVersion(2)
        }

        val kotlin = project.multiplatformExtension

        assumeAndroidSdkAvailable()
        project.androidLibrary { compileSdk = 31 }

        kotlin.targetHierarchy.default {
            common {
                group("jvmAndAndroid") {
                    withJvm()
                    withAndroid()
                }
            }
        }

        kotlin.android()
        kotlin.jvm()

        project.evaluate()

        assertEquals(
            stringSetOf("jvmAndAndroidMain"),
            kotlin.dependingSourceSetNames("commonMain")
        )

        assertEquals(
            stringSetOf("jvmMain", "androidMain", "androidDebug", "androidRelease"),
            kotlin.dependingSourceSetNames("jvmAndAndroidMain")
        )

        assertEquals(
            stringSetOf("jvmAndAndroidTest"),
            kotlin.dependingSourceSetNames("commonTest")
        )

        assertEquals(
            stringSetOf("androidUnitTest", "androidUnitTestDebug", "androidUnitTestRelease", "jvmTest"),
            kotlin.dependingSourceSetNames("jvmAndAndroidTest")
        )

        /* Check all source sets: All from jvm and android target + expected common source sets */
        assertEquals(
            setOf("commonMain", "commonTest", "jvmAndAndroidMain", "jvmAndAndroidTest") +
                    kotlin.android().compilations.flatMap { it.kotlinSourceSets }.map { it.name } +
                    kotlin.jvm().compilations.flatMap { it.kotlinSourceSets }.map { it.name },
            kotlin.sourceSets.map { it.name }.toStringSet()
        )
    }

    @Test
    fun `test - hierarchy apply - extend`() {
        val descriptor = KotlinTargetHierarchyDescriptor {
            group("common") {
                group("base")
            }
        }

        kotlin.apply {
            targetHierarchy.apply(descriptor) {
                group("base") {
                    group("extension") {
                        withLinuxX64()
                    }
                }
            }
            linuxX64()
        }

        assertEquals(
            stringSetOf("baseMain"), kotlin.dependingSourceSetNames("commonMain")
        )

        assertEquals(
            stringSetOf("extensionMain"), kotlin.dependingSourceSetNames("baseMain")
        )

        assertEquals(
            stringSetOf("linuxX64Main"), kotlin.dependingSourceSetNames("extensionMain")
        )

        assertEquals(
            stringSetOf(), kotlin.dependingSourceSetNames("linuxX64Main")
        )
    }

    @Test
    fun `test - hierarchy custom`() {
        kotlin.targetHierarchy.custom {
            common {
                group("native") {
                    withNative()
                }

                group("nix") {
                    withLinux()
                    withMacos()
                }
            }
        }

        kotlin.linuxX64()
        kotlin.macosX64()
        kotlin.mingwX64()

        assertEquals(
            stringSetOf("nativeMain", "nixMain"),
            kotlin.dependingSourceSetNames("commonMain")
        )

        assertEquals(
            stringSetOf("linuxX64Main", "macosX64Main", "mingwX64Main"),
            kotlin.dependingSourceSetNames("nativeMain")
        )

        assertEquals(
            stringSetOf("linuxX64Main", "macosX64Main"),
            kotlin.dependingSourceSetNames("nixMain")
        )
    }

    @Test
    fun `test - hierarchy js and wasm`() {
        kotlin.targetHierarchy.custom {
            common {
                group("web") {
                    withJs()
                    withWasm()
                }
            }
        }

        kotlin.wasm()
        kotlin.js()

        assertEquals(
            stringSetOf("webMain"),
            kotlin.dependingSourceSetNames("commonMain")
        )

        assertEquals(
            stringSetOf("jsMain", "wasmMain"),
            kotlin.dependingSourceSetNames("webMain")
        )
    }

    @Test
    fun `test - hierarchy js and wasm split`() {
        kotlin.targetHierarchy.custom {
            common {
                group("jsAndJvm") {
                    withJs()
                    withJvm()
                }
                group("wasmAndLinux") {
                    withWasm()
                    withLinuxX64()
                }
            }
        }

        kotlin.wasm()
        kotlin.js()
        kotlin.jvm()
        kotlin.linuxX64()

        assertEquals(
            stringSetOf("jsAndJvmMain", "wasmAndLinuxMain"),
            kotlin.dependingSourceSetNames("commonMain")
        )

        assertEquals(
            stringSetOf("jsMain", "jvmMain"),
            kotlin.dependingSourceSetNames("jsAndJvmMain")
        )

        assertEquals(
            stringSetOf("wasmMain", "linuxX64Main"),
            kotlin.dependingSourceSetNames("wasmAndLinuxMain")
        )
    }

    @Test
    fun `test - hierarchy set - extend - with new root`() {
        val descriptor = KotlinTargetHierarchyDescriptor {
            group("common") {
                group("base")
            }
        }

        kotlin.apply {
            targetHierarchy.apply(descriptor) {
                group("newRoot") {
                    group("base") {
                        group("extension") {
                            withLinuxX64()
                        }
                    }
                }
            }
            linuxX64()
        }

        assertEquals(
            stringSetOf("baseMain"), kotlin.dependingSourceSetNames("newRootMain")
        )

        assertEquals(
            stringSetOf("baseMain"), kotlin.dependingSourceSetNames("commonMain")
        )

        assertEquals(
            stringSetOf("extensionMain"), kotlin.dependingSourceSetNames("baseMain")
        )

        assertEquals(
            stringSetOf("linuxX64Main"), kotlin.dependingSourceSetNames("extensionMain")
        )

        assertEquals(
            stringSetOf(), kotlin.dependingSourceSetNames("linuxX64Main")
        )
    }

}

private fun KotlinMultiplatformExtension.dependingSourceSetNames(sourceSetName: String) =
    dependingSourceSetNames(sourceSets.getByName(sourceSetName))

private fun KotlinMultiplatformExtension.dependingSourceSetNames(sourceSet: KotlinSourceSet) =
    sourceSets.filter { sourceSet in it.dependsOn }.map { it.name }.toStringSet()


/* StringSet: Special Set implementation, which makes it easy to copy and paste after assertions fail */

private fun stringSetOf(vararg values: String) = StringSet(values.toSet())

private fun Iterable<String>.toStringSet() = StringSet(this.toSet())

private data class StringSet(private val set: Set<String>) : Set<String> by set {
    override fun toString(): String {
        return "stringSetOf(" + set.joinToString(", ") { "\"$it\"" } + ")"
    }
}

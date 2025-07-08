/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName", "JUnitTestCaseWithNoTests")
@file:OptIn(ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinHierarchyTemplate
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.hierarchy.buildHierarchy
import org.jetbrains.kotlin.gradle.util.*
import kotlin.reflect.KFunction
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType
import kotlin.test.*

class KotlinHierarchyDslTest {

    private val project = buildProjectWithMPP()
    private val kotlin = project.multiplatformExtension

    @Test
    fun `test - hierarchy default - targets from all families`() {
        kotlin.apply {
            applyHierarchyTemplate(KotlinHierarchyTemplate.default)

            enableAllKotlinTargets()
        }

        assertEquals(
            stringSetOf("jvmMain", "nativeMain", "wasmWasiMain", "webMain"),
            kotlin.dependingSourceSetNames("commonMain")
        )

        assertEquals(
            stringSetOf("jvmTest", "nativeTest", "wasmWasiTest", "webTest"),
            kotlin.dependingSourceSetNames("commonTest")
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
            stringSetOf("iosArm64Main", "iosSimulatorArm64Main", "iosX64Main"),
            kotlin.dependingSourceSetNames("iosMain")
        )

        assertEquals(
            stringSetOf("iosArm64Test", "iosSimulatorArm64Test", "iosX64Test"),
            kotlin.dependingSourceSetNames("iosTest")
        )

        assertEquals(
            stringSetOf("tvosArm64Main", "tvosSimulatorArm64Main", "tvosX64Main"),
            kotlin.dependingSourceSetNames("tvosMain")
        )

        assertEquals(
            stringSetOf("tvosArm64Test", "tvosSimulatorArm64Test", "tvosX64Test"),
            kotlin.dependingSourceSetNames("tvosTest")
        )

        assertEquals(
            stringSetOf("watchosArm32Main", "watchosArm64Main", "watchosDeviceArm64Main", "watchosSimulatorArm64Main", "watchosX64Main"),
            kotlin.dependingSourceSetNames("watchosMain")
        )

        assertEquals(
            stringSetOf("watchosArm32Test", "watchosArm64Test", "watchosDeviceArm64Test", "watchosSimulatorArm64Test", "watchosX64Test"),
            kotlin.dependingSourceSetNames("watchosTest")
        )

        assertEquals(
            stringSetOf("linuxArm64Main", "linuxX64Main"),
            kotlin.dependingSourceSetNames("linuxMain")
        )

        assertEquals(
            stringSetOf("linuxArm64Test", "linuxX64Test"),
            kotlin.dependingSourceSetNames("linuxTest")
        )

        assertEquals(
            stringSetOf("macosArm64Main", "macosX64Main"),
            kotlin.dependingSourceSetNames("macosMain")
        )

        assertEquals(
            stringSetOf("macosArm64Test", "macosX64Test"),
            kotlin.dependingSourceSetNames("macosTest")
        )

        assertEquals(
            stringSetOf("mingwX64Main"),
            kotlin.dependingSourceSetNames("mingwMain")
        )

        assertEquals(
            stringSetOf("mingwX64Test"),
            kotlin.dependingSourceSetNames("mingwTest")
        )

        assertEquals(
            stringSetOf("androidNativeArm32Main", "androidNativeArm64Main", "androidNativeX64Main", "androidNativeX86Main"),
            kotlin.dependingSourceSetNames("androidNativeMain")
        )

        assertEquals(
            stringSetOf("androidNativeArm32Test", "androidNativeArm64Test", "androidNativeX64Test", "androidNativeX86Test"),
            kotlin.dependingSourceSetNames("androidNativeTest")
        )

        assertEquals(
            stringSetOf("jsMain", "wasmJsMain"),
            kotlin.dependingSourceSetNames("webMain")
        )

        assertEquals(
            stringSetOf("jsTest", "wasmJsTest"),
            kotlin.dependingSourceSetNames("webTest")
        )
    }

    @Test
    fun `test - tree layout for all targets`() {
        kotlin.apply {
            applyHierarchyTemplate(KotlinHierarchyTemplate.default)

            enableAllKotlinTargets()
        }

        assertEquals(
            /* language=text */ """
            commonMain
                ├── jvmMain
                ├── nativeMain
                │   ├── androidNativeMain
                │   │   ├── androidNativeArm32Main
                │   │   ├── androidNativeArm64Main
                │   │   ├── androidNativeX64Main
                │   │   └── androidNativeX86Main
                │   ├── appleMain
                │   │   ├── iosMain
                │   │   │   ├── iosArm64Main
                │   │   │   ├── iosSimulatorArm64Main
                │   │   │   └── iosX64Main
                │   │   ├── macosMain
                │   │   │   ├── macosArm64Main
                │   │   │   └── macosX64Main
                │   │   ├── tvosMain
                │   │   │   ├── tvosArm64Main
                │   │   │   ├── tvosSimulatorArm64Main
                │   │   │   └── tvosX64Main
                │   │   └── watchosMain
                │   │       ├── watchosArm32Main
                │   │       ├── watchosArm64Main
                │   │       ├── watchosDeviceArm64Main
                │   │       ├── watchosSimulatorArm64Main
                │   │       └── watchosX64Main
                │   ├── linuxMain
                │   │   ├── linuxArm64Main
                │   │   └── linuxX64Main
                │   └── mingwMain
                │       └── mingwX64Main
                ├── wasmWasiMain
                └── webMain
                    ├── jsMain
                    └── wasmJsMain
            """.trimIndent(),
            renderSourceSetsAsTree(kotlin, "commonMain")
        )

        assertEquals(
            /* language=text */ """
            commonTest
                ├── jvmTest
                ├── nativeTest
                │   ├── androidNativeTest
                │   │   ├── androidNativeArm32Test
                │   │   ├── androidNativeArm64Test
                │   │   ├── androidNativeX64Test
                │   │   └── androidNativeX86Test
                │   ├── appleTest
                │   │   ├── iosTest
                │   │   │   ├── iosArm64Test
                │   │   │   ├── iosSimulatorArm64Test
                │   │   │   └── iosX64Test
                │   │   ├── macosTest
                │   │   │   ├── macosArm64Test
                │   │   │   └── macosX64Test
                │   │   ├── tvosTest
                │   │   │   ├── tvosArm64Test
                │   │   │   ├── tvosSimulatorArm64Test
                │   │   │   └── tvosX64Test
                │   │   └── watchosTest
                │   │       ├── watchosArm32Test
                │   │       ├── watchosArm64Test
                │   │       ├── watchosDeviceArm64Test
                │   │       ├── watchosSimulatorArm64Test
                │   │       └── watchosX64Test
                │   ├── linuxTest
                │   │   ├── linuxArm64Test
                │   │   └── linuxX64Test
                │   └── mingwTest
                │       └── mingwX64Test
                ├── wasmWasiTest
                └── webTest
                    ├── jsTest
                    └── wasmJsTest
            """.trimIndent(),
            renderSourceSetsAsTree(kotlin, "commonTest")
        )
    }

    @Test
    fun `test - hierarchy default - only linuxX64`() {
        kotlin.apply {
            applyDefaultHierarchyTemplate()
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
        assertNotNull(KotlinHierarchyTemplate.default.buildHierarchy(kotlin.linuxX64().compilations.main))
        assertNotNull(KotlinHierarchyTemplate.default.buildHierarchy(kotlin.linuxX64().compilations.test))
        assertNull(KotlinHierarchyTemplate.default.buildHierarchy(kotlin.linuxX64().compilations.maybeCreate("custom")))

        kotlin.applyHierarchyTemplate(KotlinHierarchyTemplate.default)

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

        assertAndroidSdkAvailable()
        project.androidLibrary { compileSdk = 31 }

        kotlin.applyHierarchyTemplate(KotlinHierarchyTemplate.default) {
            common {
                group("jvmAndAndroid") {
                    withJvm()
                    withAndroidTarget()
                }
            }
        }

        kotlin.androidTarget()
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
                    kotlin.androidTarget().compilations.flatMap { it.kotlinSourceSets }.map { it.name } +
                    kotlin.jvm().compilations.flatMap { it.kotlinSourceSets }.map { it.name },
            kotlin.sourceSets.map { it.name }.toStringSet()
        )
    }

    @Test
    fun `test - hierarchy apply - extend`() {
        val descriptor = KotlinHierarchyTemplate {
            group("common") {
                group("base")
            }
        }

        kotlin.apply {
            applyHierarchyTemplate(descriptor) {
                group("base") {
                    group("extension") {
                        withLinuxX64()
                    }
                }
            }
            linuxX64()
        }

        @Suppress("DuplicatedCode")
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
        kotlin.applyHierarchyTemplate {
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
        kotlin.applyHierarchyTemplate {
            common {
                group("web") {
                    withJs()
                    withWasmJs()
                }
            }
        }

        kotlin.wasmJs()
        kotlin.js()

        assertEquals(
            stringSetOf("webMain"),
            kotlin.dependingSourceSetNames("commonMain")
        )

        assertEquals(
            stringSetOf("jsMain", "wasmJsMain"),
            kotlin.dependingSourceSetNames("webMain")
        )
    }

    @Test
    fun `test - hierarchy js and wasm split`() {
        kotlin.applyHierarchyTemplate {
            common {
                group("jsAndJvm") {
                    withJs()
                    withJvm()
                }
                group("wasmAndLinux") {
                    withWasmJs()
                    withLinuxX64()
                }
            }
        }

        kotlin.wasmJs()
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
            stringSetOf("wasmJsMain", "linuxX64Main"),
            kotlin.dependingSourceSetNames("wasmAndLinuxMain")
        )
    }

    @Test
    fun `test - hierarchy set - extend - with new root`() {
        val descriptor = KotlinHierarchyTemplate {
            group("common") {
                group("base")
            }
        }

        kotlin.apply {
            applyHierarchyTemplate(descriptor) {
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

    /**
     * Example from the documentation is supposed to create
     *
     * ```
     *                       commonMain
     *                           |
     *              +------------+----------+
     *              |                       |
     *          frontendMain            appleMain
     *              |                        |
     *    +---------+------------+-----------+----------+
     *    |                      |                      |
     * jvmMain                iosMain               macosX64Main
     *                           |
     *                           |
     *                      +----+----+
     *                      |         |
     *                iosX64Main   iosArm64Main
     * ```
     */
    @Test
    fun `test - diamond hierarchy from documentation example`() {
        kotlin.applyHierarchyTemplate {
            common {
                group("ios") {
                    withIos()
                }
                group("frontend") {
                    withJvm()
                    group("ios") // <- ! We can again reference the 'ios' group
                }
                group("apple") {
                    withMacos()
                    group("ios") // <- ! We can again reference the 'ios' group
                }
            }
        }

        kotlin.iosX64()
        kotlin.iosArm64()
        kotlin.macosX64()
        kotlin.jvm()

        assertEquals(
            stringSetOf("frontendMain", "appleMain"), kotlin.dependingSourceSetNames("commonMain")
        )

        assertEquals(
            stringSetOf("jvmMain", "iosMain"), kotlin.dependingSourceSetNames("frontendMain")
        )

        assertEquals(
            stringSetOf("macosX64Main", "iosMain"), kotlin.dependingSourceSetNames("appleMain")
        )

        assertEquals(
            stringSetOf("iosArm64Main", "iosX64Main"), kotlin.dependingSourceSetNames("iosMain")
        )
    }

    companion object {
        private fun KotlinMultiplatformExtension.enableAllKotlinTargets(
            excludedTargets: Set<String> = setOf(
                // Disable Android because it requires that all projects have AGP,
                // because Android is not relevant for the KMP default hierarchy,
                // and adding AGP would be slow and complicates the tests.
                "androidTarget",
            ),
        ) {
            // Find all KotlinMultiplatformExtension functions
            // that have no args, are not deprecated, and return a KotlinTarget.
            // We assume these are KMP targets (but this assumption could change...)
            KotlinMultiplatformExtension::class.members
                .filterIsInstance<KFunction<*>>()
                .filter { it.annotations.none { annotation -> annotation is Deprecated } }
                .filter { it.parameters.size == 1 }
                .filter { it.parameters.single().type.isSubtypeOf(KotlinMultiplatformExtension::class.starProjectedType) }
                .filter { it.returnType.isSubtypeOf(KotlinTarget::class.starProjectedType) }
                .filter { it.name !in excludedTargets }
                .forEach {
                    it.call(this)
                }
        }
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


/**
 * Create a tree of the source set hierarchy.
 *
 * @param[rootSourceSetName] Name of the root source set. Good choices are `commonMain` or `commonTest`.
 * @param[node] The type of node we're rendering. The first node is `null`. Do not configure manually, this function is called recursively.
 */
private fun renderSourceSetsAsTree(
    kotlin: KotlinMultiplatformExtension,
    rootSourceSetName: String,
    margin: String = "",
    node: TreeNode? = null,
): String {
    val childSourceSets = kotlin.dependingSourceSetNames(rootSourceSetName)
        .sorted()

    val nextMargin = if (node == TreeNode.Intermediate) "$margin│   " else "$margin    "

    val currentMargin = margin + node?.prefix.orEmpty()

    return buildString {
        appendLine("${currentMargin}${rootSourceSetName}")
        childSourceSets.forEach { entry ->
            val isLastEntry = entry == childSourceSets.last()
            val nextNode = if (isLastEntry) TreeNode.Last else TreeNode.Intermediate
            appendLine(
                renderSourceSetsAsTree(
                    kotlin = kotlin,
                    rootSourceSetName = entry,
                    margin = nextMargin,
                    node = nextNode,
                )
            )
        }
    }.trimEnd()
}

private enum class TreeNode(val prefix: String) {
    Intermediate("├── "),
    Last("└── "),
}

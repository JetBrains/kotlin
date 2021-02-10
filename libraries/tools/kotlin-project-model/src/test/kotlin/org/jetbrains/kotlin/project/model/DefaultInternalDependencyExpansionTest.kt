/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultInternalDependencyExpansionTest {
    /** Create a module without internal depends edges, those need to be added manually afterwards */
    fun createTemplateModule(vararg purposes: String) = module("foo").apply {
        listOf(*purposes).forEach { purpose ->
            val common = fragment("common", purpose)

            val (jvm, js, linux) = listOf("jvm", "js", "linux").map { platform ->
                variant(platform, purpose).apply {
                    variantAttributes[KotlinPlatformTypeAttribute] = when (platform) {
                        "jvm" -> KotlinPlatformTypeAttribute.JVM
                        "js" -> KotlinPlatformTypeAttribute.JS
                        else -> {
                            variantAttributes[KotlinNativeTargetAttribute] = platform
                            KotlinPlatformTypeAttribute.NATIVE
                        }
                    }
                }
            }

            val jvmAndJs = fragment("jvmAndJs", purpose).apply {
                refines(common)
                refinedBy(jvm)
                refinedBy(js)
            }
            val jsAndLinux = fragment("jsAndLinux", purpose).apply {
                refines(common)
                refinedBy(js)
                refinedBy(linux)
            }
        }
    }

    val expansion = DefaultInternalDependencyExpansion(AssociateVariants())

    @Test
    fun testSimpleCommonTestToCommonMainDependency() {
        val module = createTemplateModule("main", "test").apply {
            // Manually associate the variants:
            listOf("jvm", "js", "linux").forEach {
                variant(it, "test").depends(variant(it, "main"))
            }
            // Add just a single dependency from commonTest to commonMain
            fragment("commonTest").depends(fragment("commonMain"))
        }
        val expectedFragments = mapOf(
            "commonTest" to setOf("commonMain"),
            "jvmAndJsTest" to setOf("jvmAndJsMain", "commonMain"),
            "jsAndLinuxTest" to setOf("jsAndLinuxMain", "commonMain"),
            "jvmTest" to setOf("jvmMain", "jvmAndJsMain", "commonMain"),
            "jsTest" to setOf("jsMain", "jsAndLinuxMain", "jvmAndJsMain", "commonMain"),
            "linuxTest" to setOf("linuxMain", "jsAndLinuxMain", "commonMain")
        )
        module.fragments.forEach { fragment ->
            val result =
                expansion.expandInternalFragmentDependencies(fragment).visibleFragments().map { it.fragmentName }.toSet()
            val expected = expectedFragments[fragment.fragmentName].orEmpty()
            assertEquals(expected, result)
        }
    }

    @Test
    fun testDeclaredDependenciesFromExpansion() {
        val module = createTemplateModule("main", "test", "integration").apply {
            /** Manually associate the variants: */
            listOf("jvm", "js", "linux").forEach {
                variant(it, "test").depends(variant(it, "main"))
                variant(it, "integration").depends(variant(it, "test"))
                variant(it, "integration").depends(variant(it, "main"))
            }

            /**
             * Add a dependency from `commonIntegration` to `commonTest` and from (a more specific fragment)
             * `jvmAndJsTest` to `jvmAndJsMain`.
             */
            fragment("commonIntegration").depends(fragment("commonTest"))
            fragment("jvmAndJsTest").depends(fragment("jvmAndJsMain"))

            /**
             * Expect that the fragment `jvmAndJsIntegration` receives the dependency on `jvmAndJsMain` through
             * the declared dependency added to the fragment `jvmAndJsTest`, which is only visible in expansion.
             */
            val result = expansion.expandInternalFragmentDependencies(fragment("jvmAndJsIntegration"))
            val expected = setOf("commonMain", "jvmAndJsMain", "commonTest", "jvmAndJsTest")
            assertEquals(expected, result.visibleFragments().map { it.fragmentName }.toSet())
            assertTrue {
                result.entries.any {
                    it.dependingFragment == fragment("jvmAndJsTest") &&
                            it.dependencyFragment == fragment("jvmAndJsMain") &&
                            it.outcome.visibleFragments().map { it.fragmentName }.toSet() == setOf("commonMain", "jvmAndJsMain")
                }
            }
        }
    }
}
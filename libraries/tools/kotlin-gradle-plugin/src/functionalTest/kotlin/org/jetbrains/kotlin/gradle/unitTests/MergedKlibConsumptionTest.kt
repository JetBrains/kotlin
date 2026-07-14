/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.tasks.bundling.Zip
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.publishing.mergedKlibPlatformPath
import org.jetbrains.kotlin.gradle.plugin.mpp.publishing.karStateAttribute
import org.jetbrains.kotlin.gradle.plugin.mpp.publishing.karStateProcessed
import org.jetbrains.kotlin.gradle.util.buildKMPWithAllBackends
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class MergedKlibConsumptionTest {

    @Test
    fun `merged klib platform paths - are derived from attributes shared between producer and consumer`() {
        val project = buildKMPWithAllBackends()
        project.evaluate()

        fun target(name: String): KotlinTarget = project.multiplatformExtension.targets.getByName(name)

        assertEquals("platform/native/linux_x64", target("linuxX64").mergedKlibPlatformPath)
        assertEquals("platform/native/ios_arm64", target("iosArm64").mergedKlibPlatformPath)
        assertEquals("platform/js", target("js").mergedKlibPlatformPath)
        assertEquals("platform/wasm/js", target("wasmJs").mergedKlibPlatformPath)
        assertEquals("platform/wasm/wasi", target("wasmWasi").mergedKlibPlatformPath)
        assertNull(target("jvm").mergedKlibPlatformPath)
        assertNull(target("metadata").mergedKlibPlatformPath)
    }

    @Test
    fun `pack merged klib task - is registered`() {
        val project = buildKMPWithAllBackends()
        project.evaluate()
        assertIs<Zip>(project.tasks.getByName("packMergedKlibTask"))
    }

    @Test
    fun `klib consuming configurations - request the unpacked merged klib state`() {
        val project = buildKMPWithAllBackends()
        project.evaluate()

        fun compileDependencyConfigurationAttribute(targetName: String): String? {
            val compilation = project.multiplatformExtension.targets.getByName(targetName)
                .compilations.getByName("main")
            return project.configurations.getByName(compilation.compileDependencyConfigurationName)
                .attributes.getAttribute(karStateAttribute)
        }

        assertEquals(karStateProcessed, compileDependencyConfigurationAttribute("linuxX64"))
        assertEquals(karStateProcessed, compileDependencyConfigurationAttribute("iosArm64"))
        assertEquals(karStateProcessed, compileDependencyConfigurationAttribute("js"))
        assertEquals(karStateProcessed, compileDependencyConfigurationAttribute("wasmJs"))
        assertEquals(karStateProcessed, compileDependencyConfigurationAttribute("wasmWasi"))

        /* Non klib compilations must not request the merged klib state */
        assertNull(compileDependencyConfigurationAttribute("jvm"))
        assertNull(compileDependencyConfigurationAttribute("metadata"))
    }

    @Test
    fun `host specific metadata configurations - request the unpacked merged klib state`() {
        val project = buildKMPWithAllBackends()
        project.evaluate()

        /* Used by the metadata transformation; may fall back to the (merged) platform api variant */
        assertEquals(
            karStateProcessed,
            project.configurations.getByName("linuxX64CompilationDependenciesMetadata")
                .attributes.getAttribute(karStateAttribute)
        )
        assertEquals(
            karStateProcessed,
            project.configurations.getByName("iosArm64CompilationDependenciesMetadata")
                .attributes.getAttribute(karStateAttribute)
        )
    }
}

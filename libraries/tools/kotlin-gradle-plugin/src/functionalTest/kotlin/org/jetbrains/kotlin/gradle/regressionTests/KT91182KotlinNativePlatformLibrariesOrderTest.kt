/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.regressionTests

import org.jetbrains.kotlin.commonizer.KonanDistribution
import org.jetbrains.kotlin.commonizer.platformLibsDir
import org.jetbrains.kotlin.gradle.targets.native.internal.getOriginalPlatformLibrariesForTargetWithKonanDistribution
import org.jetbrains.kotlin.gradle.targets.native.internal.listLibraryFiles
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.kotlinBuildDeps
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.mavenCentralCacheRedirector
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.internal.properties.nativeProperties
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class KT91182KotlinNativePlatformLibrariesOrderTest {

    @TempDir
    lateinit var konanHome: File

    // KT-91182
    @Test
    fun `platform libraries order does not depend on directory listing order`() {
        val names = listOf("builtin", "iconv", "linux", "posix", "zlib").map { "org.jetbrains.kotlin.native.platform.$it" }
        val platformLibsDir = KonanDistribution(konanHome).platformLibsDir.resolve(KonanTarget.LINUX_X64.name)
        names.reversed().forEach { platformLibsDir.resolve(it).mkdirs() }

        val project = buildProject()
        val libraries = project.objects.getOriginalPlatformLibrariesForTargetWithKonanDistribution(
            project.provider { KonanDistribution(konanHome) },
            KonanTarget.LINUX_X64,
        )

        assertEquals(names, libraries.files.map { it.name })
    }

    // KT-91182
    @Test
    fun `leaf native compile task has stdlib but no platform libraries in file inputs and fingerprints the version`() {
        val project = buildProjectWithMPP()
        project.repositories.kotlinBuildDeps()
        project.repositories.mavenCentralCacheRedirector()
        project.multiplatformExtension.linuxX64()
        project.evaluate()

        val compileTask = project.tasks.getByName("compileKotlinLinuxX64") as KotlinNativeCompile
        val inputFileNames = compileTask.libraries.files.map { it.name }
        assertEquals(listOf("stdlib"), inputFileNames)
        assertEquals(
            project.nativeProperties.kotlinNativeVersion.get(),
            compileTask.kotlinNativeVersion,
        )
    }

    // KT-91182
    @Test
    fun `listLibraryFiles is sorted by name and skips non-library files`() {
        val names = listOf("a", "b.klib", "c", "d.klib")
        names.reversed().forEach { name ->
            if (name.endsWith(".klib")) konanHome.resolve(name).writeText("") else konanHome.resolve(name).mkdirs()
        }
        konanHome.resolve("readme.txt").writeText("")

        assertEquals(names, konanHome.listLibraryFiles().map { it.name })
    }
}

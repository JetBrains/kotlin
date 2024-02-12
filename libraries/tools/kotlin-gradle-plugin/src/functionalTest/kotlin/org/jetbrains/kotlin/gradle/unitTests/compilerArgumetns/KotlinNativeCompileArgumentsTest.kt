/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests.compilerArgumetns

import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.mavenCentralCacheRedirector
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.CreateCompilerArgumentsContext
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer.ArgumentType.PluginClasspath
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer.ArgumentType.Primitive
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer.CreateCompilerArgumentsContext.Companion.default
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer.CreateCompilerArgumentsContext.Companion.lenient
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.main
import java.io.File
import kotlin.test.*

class KotlinNativeCompileArgumentsTest {
    @Test
    fun `test - simple project - old buildCompilerArgs and new CompilerArgumentsProducer - return same arguments`() {
        val project = buildProjectWithMPP()
        project.repositories.mavenLocal()

        val kotlin = project.multiplatformExtension

        kotlin.linuxArm64()
        val linuxX64Target = kotlin.linuxX64()

        project.evaluate()

        /* Check linuxX64 main compilation as 'native platform compilation' */
        run {
            val linuxX64MainCompilation = linuxX64Target.compilations.main
            val linuxX64MainCompileTask = linuxX64MainCompilation.compileTaskProvider.get()
            `assert setupCompilerArgs and createCompilerArguments are equal`(linuxX64MainCompileTask)
        }

        /* Check commonMain compilation as 'shared native compilation' */
        run {
            val commonMainCompilation = kotlin.metadata().compilations.getByName("commonMain")
            val commonMainCompileTask = commonMainCompilation.compileTaskProvider.get() as KotlinNativeCompile
            `assert setupCompilerArgs and createCompilerArguments are equal`(commonMainCompileTask)
        }
    }


    private fun `assert setupCompilerArgs and createCompilerArguments are equal`(compile: KotlinNativeCompile) {
        val argumentsFromCompilerArgumentsProducer = compile.createCompilerArguments(
            CreateCompilerArgumentsContext(
                includeArgumentTypes = setOf(Primitive, PluginClasspath),
                isLenient = true
            )
        )
        val argumentsFromBuildCompilerArgs = K2NativeCompilerArguments().apply {
            @Suppress("DEPRECATION_ERROR")
            compile.setupCompilerArgs(this, false, true)
        }

        assertEquals(
            ArgumentUtils.convertArgumentsToStringList(argumentsFromBuildCompilerArgs),
            ArgumentUtils.convertArgumentsToStringList(argumentsFromCompilerArgumentsProducer)
        )
    }

    @Test
    fun `test - simple project - failing dependency - lenient`() {
        val project = buildProjectWithMPP()
        val kotlin = project.multiplatformExtension
        val linuxX64Target = kotlin.linuxX64()
        kotlin.sourceSets.getByName("commonMain").dependencies { implementation("not-a:dependency:1.0.0") }
        project.evaluate()

        val commonMainCompileTask = linuxX64Target.compilations.main.compileTaskProvider.get()
        assertNull(commonMainCompileTask.createCompilerArguments(lenient).libraries)
        assertFails { commonMainCompileTask.createCompilerArguments(default) }
    }

    @Test
    fun `test - opt in`() {
        val project = buildProjectWithMPP()
        val kotlin = project.multiplatformExtension
        val linuxX64Target = kotlin.linuxX64()
        linuxX64Target.compilations.all {
            @Suppress("DEPRECATION")
            it.compilerOptions.options.apply {
                optIn.add("my.OptIn")
                optIn.add("my.other.OptIn")
            }
        }

        project.evaluate()

        val arguments = linuxX64Target.compilations.main.compileTaskProvider.get().createCompilerArguments(lenient)
        assertEquals(
            listOf("my.OptIn", "my.other.OptIn"), arguments.optIn?.toList()
        )
    }

    @Test
    fun `test - k2 - shared native compilation - sources`() {
        val project = buildProjectWithMPP()
        val kotlin = project.multiplatformExtension
        kotlin.linuxX64()
        kotlin.linuxArm64()

        /* Enable K2 if necessary */
        if (KotlinVersion.DEFAULT < KotlinVersion.KOTLIN_2_0) {
            kotlin.sourceSets.all {
                it.languageSettings.languageVersion = "2.0"
            }
        }

        val commonMainSourceFile = project.file("src/commonMain/kotlin/CommonMain.kt")
        commonMainSourceFile.parentFile.mkdirs()
        commonMainSourceFile.writeText("object CommonMain")

        project.evaluate()

        val sharedNativeCompilation = kotlin.metadata().compilations.getByName("commonMain")
        val sharedNativeCompileTask = sharedNativeCompilation.compileTaskProvider.get() as KotlinNativeCompile
        val arguments = sharedNativeCompileTask.createCompilerArguments(lenient)

        assertNull(
            arguments.fragments?.toList(),
            "Expected 'fragments' to *not* be set: Metadata compilations shall use -Xcommon-sources and provide klib dependencies"
        )

        assertNull(
            arguments.fragmentSources?.toList(),
            "Expected 'fragmentSources' to *not* be set: Metadata compilations shall use -Xcommon-sources and provide klib dependencies"
        )

        assertNull(
            arguments.fragmentRefines?.toList(),
            "Expected 'fragmentRefines' to *not* be set: Metadata compilations shall use -Xcommon-sources and provide klib dependencies"
        )

        assertEquals(
            listOf(commonMainSourceFile), arguments.commonSources?.toList().orEmpty().map(::File)
        )

        assertTrue(
            commonMainSourceFile.absolutePath in arguments.freeArgs,
            "Expected commonMain source file to be present in 'freeArgs'"
        )
    }

    @Test
    fun `test - k2 - platform native compilation - sources`() {
        val project = buildProjectWithMPP()
        val kotlin = project.multiplatformExtension
        kotlin.linuxX64()

        /* Enable K2 if necessary */
        if (KotlinVersion.DEFAULT < KotlinVersion.KOTLIN_2_0) {
            kotlin.sourceSets.all {
                it.languageSettings.languageVersion = "2.0"
            }
        }

        val linuxX64SourceFile = project.file("src/linuxX64Main/kotlin/CommonMain.kt")
        linuxX64SourceFile.parentFile.mkdirs()
        linuxX64SourceFile.writeText("object Linux")

        project.multiplatformExtension.applyHierarchyTemplate {
            common { withLinux() }
        }

        project.evaluate()

        val nativeCompilation = kotlin.linuxX64().compilations.main
        val sharedNativeCompileTask = nativeCompilation.compileTaskProvider.get() as KotlinNativeCompile
        val arguments = sharedNativeCompileTask.createCompilerArguments(lenient)

        assertNull(
            arguments.commonSources?.toList(),
            "Expected 'commonSources' to not be set: Native Platform compilations shall use -Xfragment{x} arguments"
        )

        assertEquals(
            setOf("commonMain", "linuxX64Main"),
            arguments.fragments?.toSet(),
            "Expected 'fragments' to *not* be set: Metadata compilations shall use -Xcommon-sources and provide klib dependencies"
        )

        assertEquals(
            listOf("linuxX64Main:${linuxX64SourceFile.absolutePath}"),
            arguments.fragmentSources?.toList(),
        )

        assertEquals(
            listOf("linuxX64Main:commonMain"),
            arguments.fragmentRefines?.toList(),
        )


        assertTrue(
            linuxX64SourceFile.absolutePath in arguments.freeArgs,
            "Expected linuxX64 source file to be present in 'freeArgs'"
        )
    }

    @Test
    fun `native compilation dependency files should contain native platform dependencies and stdlib`() {
        val project = buildProjectWithMPP()
        project.repositories.mavenLocal()
        project.repositories.mavenCentralCacheRedirector()
        val kotlin = project.multiplatformExtension

        kotlin.linuxX64()

        project.evaluate()
        val nativeCompilation = kotlin.linuxX64().compilations.main

        val expectedPlatformDependencies = listOf("iconv", "posix", "zlib", "linux", "builtin").map {
            "org.jetbrains.kotlin.native.platform.$it"
        }.toSet()

        val expectedDependencies = expectedPlatformDependencies + listOf("stdlib")

        val actualDependencies = nativeCompilation.compileDependencyFiles
            .map { it.name }
            .toSet()

        assertEquals(expectedDependencies, actualDependencies)
    }

    @Test
    fun `native compile tasks SHOULD NOT have native platform dependencies but SHOULD have stdlib`() {
        val project = buildProjectWithMPP()
        project.repositories.mavenLocal()
        project.repositories.mavenCentralCacheRedirector()
        val kotlin = project.multiplatformExtension

        kotlin.linuxX64().binaries.executable()
        kotlin.linuxArm64().binaries.executable()

        project.evaluate()

        val compileLinuxX64 = project.tasks.getByName("compileKotlinLinuxX64") as KotlinNativeCompile
        val compileLinuxArm64 = project.tasks.getByName("compileKotlinLinuxArm64") as KotlinNativeCompile
        val compileLinuxMainMetadata = project.tasks.getByName("compileLinuxMainKotlinMetadata") as KotlinNativeCompile

        val linkLinuxX64 = project.tasks.getByName("linkDebugExecutableLinuxX64") as KotlinNativeLink
        val linkLinuxArm64 = project.tasks.getByName("linkDebugExecutableLinuxArm64") as KotlinNativeLink

        fun FileCollection?.assertIsPlatformDependencies(target: String) {
            if (this == null) fail("Expected to have platform dependencies of target $target but got null")
            for (file in this) {
                if (!file.path.contains("platform${File.separator}$target")) fail("File $file is expected to be a platform dependency of $target")
            }
        }
        compileLinuxX64.excludeOriginalPlatformLibraries.assertIsPlatformDependencies("linux_x64")
        compileLinuxArm64.excludeOriginalPlatformLibraries.assertIsPlatformDependencies("linux_arm64")
        if (compileLinuxMainMetadata.excludeOriginalPlatformLibraries != null) fail(
            "Native metadata compilation should not exclude platform libraries because they are coming from commonizer. " +
                    "And is not included by default by Kotlin/Native compiler like default platform libraries."
        )
        linkLinuxX64.excludeOriginalPlatformLibraries.assertIsPlatformDependencies("linux_x64")
        linkLinuxArm64.excludeOriginalPlatformLibraries.assertIsPlatformDependencies("linux_arm64")

        fun Array<String>?.assertFilePathsDontContain(pathSubString: String) {
            if (this == null) return
            val badFiles = filter { it.contains(pathSubString) }
            if (badFiles.isEmpty()) return
            fail("Following files contain unexpected '$pathSubString' substring in their paths: \n${badFiles.joinToString("\n")}")
        }

        compileLinuxX64.createCompilerArguments(default).libraries.assertFilePathsDontContain("linux_x64")
        compileLinuxArm64.createCompilerArguments(default).libraries.assertFilePathsDontContain("linux_arm64")
        linkLinuxX64.createCompilerArguments(default).libraries.assertFilePathsDontContain("linux_x64")
        linkLinuxArm64.createCompilerArguments(default).libraries.assertFilePathsDontContain("linux_arm64")
    }
}
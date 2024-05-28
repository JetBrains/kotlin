/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.gradle.plugin.ProjectLocalConfigurations
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.testbase.MPPNativeTargets
import org.jetbrains.kotlin.gradle.testbase.assertHasDiagnostic
import org.jetbrains.kotlin.gradle.testbase.assertNoDiagnostic
import org.jetbrains.kotlin.gradle.util.isWindows
import org.jetbrains.kotlin.gradle.util.modify
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.Assert
import org.junit.Test
import java.util.*
import java.util.zip.ZipFile
import kotlin.test.*

open class NewMultiplatformIT : BaseGradleIT() {
    private val gradleVersion = GradleVersionRequired.FOR_MPP_SUPPORT

    override val defaultGradleVersion: GradleVersionRequired
        get() = gradleVersion

    private fun Project.targetClassesDir(targetName: String, sourceSetName: String = "main") =
        classesDir(sourceSet = "$targetName/$sourceSetName")

    private data class HmppFlags(
        val hmppSupport: Boolean,
        val name: String,
    ) {
        override fun toString() = name
    }

    private val noHMPP = HmppFlags(
        name = "No HMPP",
        hmppSupport = false
    )

    private val hmppWoCompatibilityMetadataArtifact = HmppFlags(
        name = "HMPP without Compatibility Metadata Artifact",
        hmppSupport = true
    )

    private val HmppFlags.buildOptions
        get() = defaultBuildOptions().copy(
            hierarchicalMPPStructureSupport = hmppSupport
        )

    @Test
    fun testLibAndApp() = doTestLibAndApp(
        "sample-lib",
        "sample-app",
        hmppWoCompatibilityMetadataArtifact
    )

    @Test
    fun testLibAndAppWithoutHMPP() = doTestLibAndApp(
        "sample-lib",
        "sample-app",
        noHMPP
    )

    @Test
    fun testLibAndAppWithGradleKotlinDsl() = doTestLibAndApp(
        "sample-lib-gradle-kotlin-dsl",
        "sample-app-gradle-kotlin-dsl",
        hmppWoCompatibilityMetadataArtifact
    )

    private fun doTestLibAndApp(
        libProjectName: String,
        appProjectName: String,
        hmppFlags: HmppFlags,
    ) {
        val libProject = transformNativeTestProjectWithPluginDsl(libProjectName, directoryPrefix = "new-mpp-lib-and-app")
        val appProject = transformNativeTestProjectWithPluginDsl(appProjectName, directoryPrefix = "new-mpp-lib-and-app")

        val buildOptions = hmppFlags.buildOptions
        val compileTasksNames =
            listOf("Jvm6", "NodeJs", "Linux64").map { ":compileKotlin$it" }

        with(libProject) {
            build(
                "publish",
                options = buildOptions
            ) {
                assertSuccessful()
                assertTasksExecuted(*compileTasksNames.toTypedArray(), ":jvm6Jar", ":nodeJsJar", ":compileCommonMainKotlinMetadata")

                val groupDir = projectDir.resolve("repo/com/example")
                val jvmJarName = "sample-lib-jvm6/1.0/sample-lib-jvm6-1.0.jar"
                val jsExtension = "klib"
                val jsKlibName = "sample-lib-nodejs/1.0/sample-lib-nodejs-1.0.$jsExtension"
                val metadataJarName = "sample-lib/1.0/sample-lib-1.0.jar"
                val nativeKlibName = "sample-lib-linux64/1.0/sample-lib-linux64-1.0.klib"

                listOf(jvmJarName, jsKlibName, metadataJarName, "sample-lib/1.0/sample-lib-1.0.module").forEach {
                    Assert.assertTrue("$it should exist", groupDir.resolve(it).exists())
                }

                val gradleMetadata = groupDir.resolve("sample-lib/1.0/sample-lib-1.0.module").readText()
                assertFalse(gradleMetadata.contains(ProjectLocalConfigurations.ATTRIBUTE.name))

                listOf(jvmJarName, jsKlibName, nativeKlibName).forEach {
                    val pom = groupDir.resolve(it.replaceAfterLast('.', "pom"))
                    Assert.assertTrue(
                        "$pom should contain a name section.",
                        pom.readText().contains("<name>Sample MPP library</name>")
                    )
                    Assert.assertFalse(
                        "$pom should not contain standard K/N libraries as dependencies.",
                        pom.readText().contains("<groupId>Kotlin/Native</groupId>")
                    )
                }

                val jvmJarEntries = ZipFile(groupDir.resolve(jvmJarName)).entries().asSequence().map { it.name }.toSet()
                Assert.assertTrue("com/example/lib/CommonKt.class" in jvmJarEntries)
                Assert.assertTrue("com/example/lib/MainKt.class" in jvmJarEntries)

                Assert.assertTrue(groupDir.resolve(jsKlibName).exists())

                Assert.assertTrue(groupDir.resolve(nativeKlibName).exists())
            }
        }

        val libLocalRepoUri = libProject.projectDir.resolve("repo").toURI()

        with(appProject) {
            setupWorkingDir(false)

            // we use `maven { setUrl(...) }` because this syntax actually works both for Groovy and Kotlin DSLs in Gradle
            gradleBuildScript().appendText("\nrepositories { maven { setUrl(\"$libLocalRepoUri\") } }")

            fun CompiledProject.checkAppBuild() {
                assertSuccessful()
                assertTasksExecuted(*compileTasksNames.toTypedArray())

                projectDir.resolve(targetClassesDir("jvm6")).run {
                    Assert.assertTrue(resolve("com/example/app/AKt.class").exists())
                    Assert.assertTrue(resolve("com/example/app/UseBothIdsKt.class").exists())
                }

                projectDir.resolve(targetClassesDir("jvm8")).run {
                    Assert.assertTrue(resolve("com/example/app/AKt.class").exists())
                    Assert.assertTrue(resolve("com/example/app/UseBothIdsKt.class").exists())
                    Assert.assertTrue(resolve("com/example/app/Jdk8ApiUsageKt.class").exists())
                }

                val nativeExeName = if (isWindows) "main.exe" else "main.kexe"
                assertFileExists("build/bin/linux64/mainDebugExecutable/$nativeExeName")

                // Check that linker options were correctly passed to the K/N compiler.
                withNativeCommandLineArguments(":linkMainDebugExecutableLinux64") { arguments ->
                    val parsedArguments = parseCommandLineArguments<K2NativeCompilerArguments>(arguments)
                    assertEquals(listOf("-L."), parsedArguments.singleLinkerArguments?.toList())
                    assertTrue(arguments.containsSequentially("-linker-option", "-L."))
                }
            }

            build(
                "assemble",
                "resolveRuntimeDependencies",
                options = buildOptions
            ) {
                checkAppBuild()
                assertTasksExecuted(":resolveRuntimeDependencies") // KT-26301
            }

            // Now run again with a project dependency instead of a module one:
            libProject.projectDir.copyRecursively(projectDir.resolve(libProject.projectDir.name))
            gradleSettingsScript().appendText("\ninclude(\"${libProject.projectDir.name}\")")
            gradleBuildScript().modify { it.replace("\"com.example:sample-lib:1.0\"", "project(\":${libProject.projectDir.name}\")") }

            gradleBuildScript(libProjectName).takeIf { it.extension == "kts" }?.modify {
                it.replace(Regex("""\.version\(.*\)"""), "")
            }

            build(
                "clean",
                "assemble",
                "--rerun-tasks",
                options = buildOptions
            ) {
                checkAppBuild()
            }
        }
    }

    @Test
    fun testMavenPublishAppliedBeforeMultiplatformPlugin() =
        with(transformNativeTestProject("sample-lib", directoryPrefix = "new-mpp-lib-and-app")) {
            gradleBuildScript().modify { "apply plugin: 'maven-publish'\n$it" }

            build {
                assertSuccessful()
            }
        }

    private val targetName = when (HostManager.host) {
        KonanTarget.LINUX_X64 -> "linux64"
        KonanTarget.MACOS_X64 -> "macos64"
        KonanTarget.MACOS_ARM64 -> "macosArm64"
        KonanTarget.MINGW_X64 -> "mingw64"
        else -> fail("Unsupported host")
    }

    @Test
    fun testConsumeMppLibraryFromNonKotlinProject() {
        val libRepo = with(transformNativeTestProject("sample-lib", gradleVersion, "new-mpp-lib-and-app")) {
            build("publish") { assertSuccessful() }
            projectDir.resolve("repo")
        }

        with(transformNativeTestProject("sample-app-without-kotlin", gradleVersion, "new-mpp-lib-and-app")) {
            setupWorkingDir()
            gradleBuildScript().appendText("\nrepositories { maven { url '${libRepo.toURI()}' } }")

            build("assemble") {
                assertSuccessful()
                assertTasksExecuted(":compileJava")
                assertFileExists("build/classes/java/main/A.class")
            }
        }
    }

    @Test
    fun testMppBuildWithCompilerPlugins() = with(transformNativeTestProject("sample-lib", gradleVersion, "new-mpp-lib-and-app")) {
        val printOptionsTaskName = "printCompilerPluginOptions"
        val argsMarker = "=args=>"
        val classpathMarker = "=cp=>"
        val compilerPluginArgsRegex = "(\\w+)${Regex.escape(argsMarker)}(.*)".toRegex()
        val compilerPluginClasspathRegex = "(\\w+)${Regex.escape(classpathMarker)}(.*)".toRegex()

        gradleBuildScript().appendText(
            "\n" + """
            buildscript {
                dependencies {
                    classpath "org.jetbrains.kotlin:kotlin-allopen:${'$'}kotlin_version"
                    classpath "org.jetbrains.kotlin:kotlin-noarg:${'$'}kotlin_version"
                }
            }
            apply plugin: 'kotlin-allopen'
            apply plugin: 'kotlin-noarg'

            allOpen { annotation 'com.example.Annotation' }
            noArg { annotation 'com.example.Annotation' }

            task $printOptionsTaskName {
                // if the tasks are not configured during evaluation phase, configuring them during execution
                // leads to new dependencies unsuccessfully added to the resolved compilerPluginsClasspath configuration
                kotlin.targets.all { compilations.all { /*force to configure the*/ compileKotlinTask } }
                doFirst {
                    kotlin.sourceSets.each { sourceSet ->
                        def args = sourceSet.languageSettings.compilerPluginArguments
                        def cp = sourceSet.languageSettings.compilerPluginClasspath.files
                        println sourceSet.name + '$argsMarker' + args
                        println sourceSet.name + '$classpathMarker' + cp
                    }
                }
            }
            """.trimIndent()
        )

        projectDir.resolve("src/commonMain/kotlin/Annotation.kt").writeText(
            """
            package com.example
            annotation class Annotation
            """.trimIndent()
        )
        projectDir.resolve("src/commonMain/kotlin/Annotated.kt").writeText(
            """
            package com.example
            @Annotation
            open class Annotated(var y: Int) { var x = 2 }
            """.trimIndent()
        )
        // TODO once Kotlin/Native properly supports compiler plugins, move this class to the common sources
        listOf("jvm6", "nodeJs").forEach {
            projectDir.resolve("src/${it}Main/kotlin/Override.kt").writeText(
                """
                package com.example
                @Annotation
                class Override : Annotated(0) {
                    override var x = 3
                }
                """.trimIndent()
            )
        }

        // Do not use embeddable compiler in Kotlin/Native, otherwise it would effectively enable allopen & noarg plugins for Native, and
        // we'd be testing that the latest versions of allopen/noarg work with the fixed version of Kotlin/Native (defined in the root
        // build.gradle.kts), which is generally not guaranteed.
        build("assemble", "-Pkotlin.native.useEmbeddableCompilerJar=false", printOptionsTaskName) {
            assertSuccessful()
            assertTasksExecuted(*listOf("Jvm6", "NodeJs", "Linux64").map { ":compileKotlin$it" }.toTypedArray())
            assertFileExists("build/classes/kotlin/jvm6/main/com/example/Annotated.class")
            assertFileExists("build/classes/kotlin/jvm6/main/com/example/Override.class")

            val (compilerPluginArgsBySourceSet, compilerPluginClasspathBySourceSet) =
                listOf(compilerPluginArgsRegex, compilerPluginClasspathRegex)
                    .map { marker ->
                        marker.findAll(output).associate { it.groupValues[1] to it.groupValues[2] }
                    }

            // TODO once Kotlin/Native properly supports compiler plugins, expand this to all source sets:
            listOf("commonMain", "commonTest", "jvm6Main", "jvm6Test", "nodeJsMain", "nodeJsTest").forEach {
                val expectedArgs = "[plugin:org.jetbrains.kotlin.allopen:annotation=com.example.Annotation, " +
                        "plugin:org.jetbrains.kotlin.noarg:annotation=com.example.Annotation]"

                assertEquals(expectedArgs, compilerPluginArgsBySourceSet[it], "Expected $expectedArgs as plugin args for $it")
                assertTrue { compilerPluginClasspathBySourceSet[it]!!.contains("kotlin-allopen") }
                assertTrue { compilerPluginClasspathBySourceSet[it]!!.contains("kotlin-noarg") }
            }
        }
    }

    companion object {
        fun List<String>.containsSequentially(vararg elements: String): Boolean {
            check(elements.isNotEmpty())
            return Collections.indexOfSubList(this, elements.toList()) != -1
        }
    }
}

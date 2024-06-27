/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.mpp

import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.ProjectLocalConfigurations
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.isWindows
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.test.TestMetadata
import java.util.zip.ZipFile
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.test.assertContains

@MppGradlePluginTests
class MppDslAppAndLibIT : KGPBaseTest() {

    @GradleTest
    @TestMetadata(value = "new-mpp-lib-and-app")
    fun testLibAndApp(gradleVersion: GradleVersion) {
        doTestLibAndApp(
            libProjectPath = "new-mpp-lib-and-app/sample-lib",
            appProjectPath = "new-mpp-lib-and-app/sample-app",
            gradleVersion = gradleVersion,
            hmppSupport = true,
        )
    }

    @GradleTest
    @TestMetadata(value = "new-mpp-lib-and-app")
    fun testLibAndAppWithoutHMPP(gradleVersion: GradleVersion) = doTestLibAndApp(
        libProjectPath = "new-mpp-lib-and-app/sample-lib",
        appProjectPath = "new-mpp-lib-and-app/sample-app",
        gradleVersion = gradleVersion,
        hmppSupport = false,
    )

    @GradleTest
    @TestMetadata(value = "new-mpp-lib-and-app")
    fun testLibAndAppWithGradleKotlinDsl(gradleVersion: GradleVersion) {
        doTestLibAndApp(
            libProjectPath = "new-mpp-lib-and-app/sample-lib-gradle-kotlin-dsl",
            appProjectPath = "new-mpp-lib-and-app/sample-app-gradle-kotlin-dsl",
            gradleVersion = gradleVersion,
            hmppSupport = true,
        )
    }

    private fun doTestLibAndApp(
        libProjectPath: String,
        appProjectPath: String,
        gradleVersion: GradleVersion,
        hmppSupport: Boolean,
    ) {
        val additionalBuildArgs = buildList {
            if (hmppSupport) {
                add("-P" + "kotlin.mpp.hierarchicalStructureSupport")
                add("-P" + "kotlin.internal.suppressGradlePluginErrors=PreHMPPFlagsError")
            }
        }

        val localRepoDir = defaultLocalRepo(gradleVersion)

        val compileTasksNames = listOf(
            ":compileKotlinJvm6",
            ":compileKotlinNodeJs",
            ":compileKotlinLinux64",
        )

        val libProject = project(
            projectName = libProjectPath,
            gradleVersion = gradleVersion,
            localRepoDir = localRepoDir,
        ) {
            build(
                buildArguments = buildList {
                    add("clean")
                    add("publish")
                    addAll(additionalBuildArgs)
                }.toTypedArray(),
            ) {
                assertTasksExecuted(compileTasksNames)
                assertTasksExecuted(
                    ":jvm6Jar",
                    ":nodeJsJar",
                    ":compileCommonMainKotlinMetadata",
                )

                val groupDir = localRepoDir.resolve("com/example")
                assertDirectoryExists(groupDir)

                val jvmJar = groupDir.resolve("sample-lib-jvm6/1.0/sample-lib-jvm6-1.0.jar")
                val jsKlib = groupDir.resolve("sample-lib-nodejs/1.0/sample-lib-nodejs-1.0.klib")
                val metadataJar = groupDir.resolve("sample-lib/1.0/sample-lib-1.0.jar")
                val nativeKlib = groupDir.resolve("sample-lib-linux64/1.0/sample-lib-linux64-1.0.klib")
                val gradleModule = groupDir.resolve("sample-lib/1.0/sample-lib-1.0.module")

                assertFileExists(jvmJar)
                assertFileExists(jsKlib)
                assertFileExists(metadataJar)
                assertFileExists(nativeKlib)
                assertFileExists(gradleModule)

                val gradleMetadata = groupDir.resolve("sample-lib/1.0/sample-lib-1.0.module")
                assertFileDoesNotContain(gradleMetadata, ProjectLocalConfigurations.ATTRIBUTE.name)

                listOf(jvmJar, jsKlib, nativeKlib).forEach { file ->
                    val pom = file.resolveSibling(file.nameWithoutExtension + ".pom")
                    assertFileContains(
                        pom,
                        "<name>Sample MPP library</name>",
                    )
                    assertFileDoesNotContain(
                        pom,
                        "<groupId>Kotlin/Native</groupId>",
                        message = "$pom should not contain standard K/N libraries as dependencies."
                    )
                }

                val jvmJarEntries = ZipFile(jvmJar.toFile()).use { zip ->
                    zip.entries().asSequence().map { it.name }.toSet()
                }
                assertContains(jvmJarEntries, "com/example/lib/CommonKt.class")
                assertContains(jvmJarEntries, "com/example/lib/MainKt.class")
            }
        }

        project(
            projectName = appProjectPath,
            gradleVersion = gradleVersion,
            localRepoDir = localRepoDir,
        ) {
            val buildGradle = listOf(buildGradle, buildGradleKts).first { it.exists() }
            buildGradle.replaceText(
                "kotlinCompileLogLevel = LogLevel.LIFECYCLE",
                "kotlinCompileLogLevel = LogLevel.DEBUG",
            )
            buildGradle.replaceText(
                "kotlinCompileCacheBuster = 0",
                "kotlinCompileCacheBuster = System.currentTimeMillis()",
            )

            fun BuildResult.checkAppBuild() {
                assertTasksExecuted(compileTasksNames)
                assertTasksExecuted(":linkMainDebugExecutableLinux64")

                projectPath.resolve(targetClassesDir("jvm6")).run {
                    assertFileExists(resolve("com/example/app/AKt.class"))
                    assertFileExists(resolve("com/example/app/UseBothIdsKt.class"))
                }

                projectPath.resolve(targetClassesDir("jvm8")).run {
                    assertFileExists(resolve("com/example/app/AKt.class"))
                    assertFileExists(resolve("com/example/app/UseBothIdsKt.class"))
                    assertFileExists(resolve("com/example/app/Jdk8ApiUsageKt.class"))
                }

                assertFileInProjectExists("build/bin/linux64/mainDebugExecutable/main.kexe")

                // Check that linker options were correctly passed to the K/N compiler.
                extractNativeTasksCommandLineArgumentsFromOutput(":linkMainDebugExecutableLinux64") {
                    assertCommandLineArgumentsContainSequentially("-linker-option", "-L.")
                }
            }

            build(
                buildArguments = buildList {
                    add("assemble")
                    add("resolveRuntimeDependencies")
                    addAll(additionalBuildArgs)
                    add("-P" + "kotlinCompileCacheBuster=${System.currentTimeMillis()}")
                    add("-P" + "kotlinCompileLogLevel=DEBUG")
                }.toTypedArray(),
            ) {
                checkAppBuild()
                assertTasksExecuted(":resolveRuntimeDependencies") // KT-26301
            }

            // Now run again with a project dependency instead of a module one:
            includeOtherProjectAsSubmodule(
                otherProjectName = libProjectPath,
                newSubmoduleName = libProject.projectPath.name,
                isKts = settingsGradleKts.exists(),
            )

            // Delete the lib local repo, to ensure that Gradle uses the subproject
            localRepoDir.deleteRecursively()

            buildGradle.replaceText(
                "\"com.example:sample-lib:1.0\"",
                "project(\":${libProject.projectPath.name}\")",
            )

            build("clean")

            build(
                buildArguments = buildList {
                    add("assemble")
                    add("-P" + "kotlinCompileCacheBuster=${System.currentTimeMillis()}")
                    add("-P" + "kotlinCompileLogLevel=DEBUG")
                }.toTypedArray(),
            ) {
                assertTasksExecuted(":${libProject.projectPath.name}:assemble")
                checkAppBuild()
            }
        }
    }

    companion object {
        private fun targetClassesDir(targetName: String, sourceSetName: String = "main") =
            classesDir(sourceSet = "$targetName/$sourceSetName")

        private fun classesDir(subproject: String? = null, sourceSet: String = "main", language: String = "kotlin"): String =
            (subproject?.plus("/") ?: "") + "build/classes/$language/$sourceSet/"
    }
}

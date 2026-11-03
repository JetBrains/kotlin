/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.internal.tasks.DefaultTaskContainer
import org.gradle.api.tasks.TaskInstantiationException
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.mavenCentralCacheRedirector
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.kotlinBuildDeps
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.util.assertThrows
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class KotlinNativeLinkTest {

    @Test
    fun `apiFiles - contain api dependencies - when KotlinNativeLink task is configured before compilation's api dependencies`() {
        val parent = buildProject()
        val apiInCommon = buildProjectWithMPP(
            projectBuilder = { withParent(parent).withName("apiInCommon") }
        ) { kotlin { linuxArm64() } }.evaluate()

        val apiInLinux = buildProjectWithMPP(
            projectBuilder = { withParent(parent).withName("apiInLinux") }
        ) { kotlin { linuxArm64() } }.evaluate()

        buildProjectWithMPP(
            projectBuilder = { withParent(parent).withName("apiInIos") }
        ) { kotlin { linuxArm64() } }.evaluate()

        val exportingProject = buildProjectWithMPP(
            projectBuilder = { withParent(parent).withName("exportingProject") }
        ) {
            repositories.kotlinBuildDeps()
            repositories.mavenCentralCacheRedirector()
            kotlin {
                iosArm64()
                linuxArm64 {
                    binaries.staticLib {
                        export(project(":apiInCommon"))
                        export(project(":apiInLinux"))
                    }
                }

                sourceSets.commonMain.dependencies { api(project(":apiInCommon")) }
                sourceSets.linuxMain.dependencies { api(project(":apiInLinux")) }
                sourceSets.iosMain.dependencies { api(project(":apiInIos")) }
            }
        }

        // 1. Configure KotlinNativeLink's apiFiles before compilation's apiConfiguration is wired. Using apiFilesConfiguration directly here because apiFiles is filtered by File.exists check
        val apiFiles = (exportingProject.tasks.getByName("linkReleaseStaticLinuxArm64") as KotlinNativeLink).apiFiles

        // 2. Set up the compilations
        exportingProject.evaluate()

        assertEquals(
            hashSetOf(
                apiInCommon.layout.buildDirectory.file("classes/kotlin/linuxArm64/main/klib/apiInCommon").get().asFile,
                apiInLinux.layout.buildDirectory.file("classes/kotlin/linuxArm64/main/klib/apiInLinux").get().asFile,
            ),
            apiFiles.files,
        )
    }

    @Test
    fun `KT-72112 - eager KotlinNativeLink task instantiation - doesn't fail configuration`() {
        buildProjectWithMPP {
            // Force all tasks to instantiate eagerly
            tasks.all { }
            kotlin {
                iosSimulatorArm64().binaries.framework { }
            }
        }.evaluate()
    }

    @Test
    fun `KT-89241 - link task declares produced files as outputs - not the whole destination directory`() {
        val project = buildProjectWithMPP {
            kotlin {
                linuxX64 {
                    binaries.executable()
                    binaries.staticLib()
                    binaries.sharedLib()
                }
                mingwX64 { binaries.sharedLib() }
                iosSimulatorArm64 {
                    binaries.executable()
                    binaries.sharedLib()
                    binaries.staticLib()
                    binaries.framework()
                    binaries.framework("static") { isStatic = true }
                }
            }
        }.evaluate()

        fun outputsOf(taskName: String): Set<File> = (project.tasks.getByName(taskName) as KotlinNativeLink).outputs.files.files
        fun bin(target: String, binary: String, vararg names: String): Set<File> {
            val dir = project.layout.buildDirectory.dir("bin/$target/$binary").get().asFile
            return names.map { dir.resolve(it) }.toSet()
        }

        assertEquals(bin("linuxX64", "debugExecutable", "test.kexe"), outputsOf("linkDebugExecutableLinuxX64"))
        assertEquals(bin("linuxX64", "debugTest", "test.kexe"), outputsOf("linkDebugTestLinuxX64"))
        assertEquals(bin("linuxX64", "debugStatic", "libtest.a", "libtest_api.h"), outputsOf("linkDebugStaticLinuxX64"))
        assertEquals(bin("linuxX64", "debugShared", "libtest.so", "libtest_api.h"), outputsOf("linkDebugSharedLinuxX64"))
        assertEquals(bin("mingwX64", "debugShared", "test.dll", "test_api.h", "test.def"), outputsOf("linkDebugSharedMingwX64"))

        // Apple binaries get a dSYM, release ones too because light debug info is on by default.
        // Static archives don't.
        assertEquals(
            bin("iosSimulatorArm64", "debugExecutable", "test.kexe", "test.kexe.dSYM"),
            outputsOf("linkDebugExecutableIosSimulatorArm64")
        )
        assertEquals(
            bin("iosSimulatorArm64", "releaseExecutable", "test.kexe", "test.kexe.dSYM"),
            outputsOf("linkReleaseExecutableIosSimulatorArm64")
        )
        assertEquals(
            bin("iosSimulatorArm64", "debugShared", "libtest.dylib", "libtest_api.h", "libtest.dylib.dSYM"),
            outputsOf("linkDebugSharedIosSimulatorArm64")
        )
        assertEquals(
            bin("iosSimulatorArm64", "debugStatic", "libtest.a", "libtest_api.h"),
            outputsOf("linkDebugStaticIosSimulatorArm64")
        )
        assertEquals(
            bin("iosSimulatorArm64", "debugFramework", "test.framework", "test.framework.dSYM"),
            outputsOf("linkDebugFrameworkIosSimulatorArm64")
        )
        assertEquals(
            bin("iosSimulatorArm64", "releaseFramework", "test.framework", "test.framework.dSYM"),
            outputsOf("linkReleaseFrameworkIosSimulatorArm64")
        )
        assertEquals(
            bin("iosSimulatorArm64", "staticDebugFramework", "static.framework"),
            outputsOf("linkStaticDebugFrameworkIosSimulatorArm64")
        )
    }
}

/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName", "DuplicatedCode")

package org.jetbrains.kotlin.gradle.dependencyResolutionTests.tcs

import org.gradle.api.Project
import org.jetbrains.kotlin.compilerRunner.konanVersion
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.mavenCentralCacheRedirector
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinResolvedBinaryDependency
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.assertMatches
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.binaryCoordinates
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.ide.kotlinIdeMultiplatformImport
import org.jetbrains.kotlin.gradle.util.applyMultiplatformPlugin
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.enableDefaultStdlibDependency
import org.jetbrains.kotlin.gradle.util.enableDependencyVerification
import org.jetbrains.kotlin.gradle.utils.androidExtension
import org.junit.Test

class IdeStdlibResolutionTest {

    @Test
    fun `test single jvm target`() {
        val project = createProjectWithDefaultStdlibEnabled()

        val kotlin = project.multiplatformExtension
        kotlin.jvm()

        project.evaluate()

        project.assertStdlibDependencies(kotlin.sourceSets.getByName("commonMain"), jvmStdlibDependencies(kotlin))
        project.assertStdlibDependencies(kotlin.sourceSets.getByName("commonTest"), jvmStdlibDependencies(kotlin))
        project.assertStdlibDependencies(kotlin.sourceSets.getByName("jvmMain"), jvmStdlibDependencies(kotlin))
        project.assertStdlibDependencies(kotlin.sourceSets.getByName("jvmTest"), jvmStdlibDependencies(kotlin))
    }

    @Test
    fun `test single native target`() {
        val project = createProjectWithDefaultStdlibEnabled()

        val kotlin = project.multiplatformExtension
        kotlin.linuxX64("linux")

        project.evaluate()

        project.assertStdlibDependencies(kotlin.sourceSets.getByName("commonMain"), nativeStdlibDependency(kotlin))
        project.assertStdlibDependencies(kotlin.sourceSets.getByName("commonTest"), nativeStdlibDependency(kotlin))
        project.assertStdlibDependencies(kotlin.sourceSets.getByName("linuxMain"), nativeStdlibDependency(kotlin))
        project.assertStdlibDependencies(kotlin.sourceSets.getByName("linuxTest"), nativeStdlibDependency(kotlin))
    }

    @Test
    fun `test single js target`() {
        val project = createProjectWithDefaultStdlibEnabled()

        val kotlin = project.multiplatformExtension
        kotlin.js(KotlinJsCompilerType.IR)

        project.evaluate()

        project.assertStdlibDependencies(kotlin.sourceSets.getByName("commonMain"), jsStdlibDependency(kotlin))
        project.assertStdlibDependencies(kotlin.sourceSets.getByName("commonTest"), jsStdlibDependency(kotlin))
        project.assertStdlibDependencies(kotlin.sourceSets.getByName("jsMain"), jsStdlibDependency(kotlin))
        project.assertStdlibDependencies(kotlin.sourceSets.getByName("jsTest"), jsStdlibDependency(kotlin))
    }

    @Test
    fun `test jvm+native shared simple project`() {
        val project = createProjectWithDefaultStdlibEnabled()

        val kotlin = project.multiplatformExtension

        kotlin.jvm()
        kotlin.linuxX64("linux")

        project.evaluate()

        project.assertStdlibDependencies(kotlin.sourceSets.getByName("commonMain"), commonStdlibDependency(kotlin))
        project.assertStdlibDependencies(kotlin.sourceSets.getByName("commonTest"), commonStdlibDependency(kotlin))
        project.assertStdlibDependencies(kotlin.sourceSets.getByName("jvmMain"), jvmStdlibDependencies(kotlin))
        project.assertStdlibDependencies(kotlin.sourceSets.getByName("jvmTest"), jvmStdlibDependencies(kotlin))
        project.assertStdlibDependencies(kotlin.sourceSets.getByName("linuxMain"), nativeStdlibDependency(kotlin))
        project.assertStdlibDependencies(kotlin.sourceSets.getByName("linuxTest"), nativeStdlibDependency(kotlin))
    }

    @Test
    fun `test bamboo jvm`() {
        val project = createProjectWithDefaultStdlibEnabled()

        val kotlin = project.multiplatformExtension
        kotlin.jvm()
        kotlin.linuxX64("linux")

        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val commonTest = kotlin.sourceSets.getByName("commonTest")
        val jvmMain = kotlin.sourceSets.getByName("jvmMain")
        val jvmTest = kotlin.sourceSets.getByName("jvmTest")
        val jvmIntermediateMain = kotlin.sourceSets.create("jvmIntermediateMain") {
            it.dependsOn(commonMain)
            jvmMain.dependsOn(it)
        }
        val jvmIntermediateTest = kotlin.sourceSets.create("jvmIntermediateTest") {
            it.dependsOn(commonTest)
            jvmTest.dependsOn(it)
        }

        project.evaluate()

        project.assertStdlibDependencies(commonMain, commonStdlibDependency(kotlin))
        project.assertStdlibDependencies(commonTest, commonStdlibDependency(kotlin))
        project.assertStdlibDependencies(jvmIntermediateMain, jvmStdlibDependencies(kotlin))
        project.assertStdlibDependencies(jvmIntermediateTest, jvmStdlibDependencies(kotlin))
    }

    @Test
    fun `test bamboo linux`() {
        val project = createProjectWithDefaultStdlibEnabled()

        val kotlin = project.multiplatformExtension
        kotlin.jvm()
        kotlin.linuxX64("linux")

        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val commonTest = kotlin.sourceSets.getByName("commonTest")
        val linuxMain = kotlin.sourceSets.getByName("linuxMain")
        val linuxTest = kotlin.sourceSets.getByName("linuxTest")
        val linuxIntermediateMain = kotlin.sourceSets.create("linuxIntermediateMain") {
            it.dependsOn(commonMain)
            linuxMain.dependsOn(it)
        }
        val linuxIntermediateTest = kotlin.sourceSets.create("linuxIntermediateTest") {
            it.dependsOn(commonTest)
            linuxTest.dependsOn(it)
        }

        project.evaluate()

        project.assertStdlibDependencies(linuxIntermediateMain, nativeStdlibDependency(kotlin))
        project.assertStdlibDependencies(linuxIntermediateTest, nativeStdlibDependency(kotlin))
    }

    @Test
    fun `test nativeShared`() {
        val project = createProjectWithDefaultStdlibEnabled()

        val kotlin = project.multiplatformExtension
        kotlin.jvm()
        kotlin.linuxX64("x64")
        kotlin.linuxArm64("arm64")

        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val commonTest = kotlin.sourceSets.getByName("commonTest")
        val x64Main = kotlin.sourceSets.getByName("x64Main")
        val x64Test = kotlin.sourceSets.getByName("x64Test")
        val arm64Main = kotlin.sourceSets.getByName("arm64Main")
        val arm64Test = kotlin.sourceSets.getByName("arm64Test")
        val linuxSharedMain = kotlin.sourceSets.create("linuxSharedMain") {
            it.dependsOn(commonMain)
            x64Main.dependsOn(it)
            arm64Main.dependsOn(it)
        }
        val linuxSharedTest = kotlin.sourceSets.create("linuxSharedTest") {
            it.dependsOn(commonTest)
            x64Test.dependsOn(it)
            arm64Test.dependsOn(it)
        }

        project.evaluate()

        project.assertStdlibDependencies(
            linuxSharedMain, listOf(
                nativeStdlibDependency(kotlin),

                /* See: KT-56278: We still need stdlib-common for shared native source sets */
                commonStdlibDependency(kotlin)
            )
        )
        project.assertStdlibDependencies(
            linuxSharedTest, listOf(
                nativeStdlibDependency(kotlin),

                /* See: KT-56278: We still need stdlib-common for shared native source sets */
                commonStdlibDependency(kotlin)
            )
        )
    }

    @Test
    fun `test jvm + android`() {
        val project = createProjectWithAndroidAndDefaultStdlibEnabled()

        val kotlin = project.multiplatformExtension
        kotlin.androidTarget()
        kotlin.jvm()

        project.evaluate()

        // TODO think about jvm + android stdlib
        project.assertStdlibDependencies(kotlin.sourceSets.getByName("commonMain"), emptyList<Any>())
        project.assertStdlibDependencies(kotlin.sourceSets.getByName("commonTest"), emptyList<Any>())
    }

    private fun Project.assertStdlibDependencies(sourceSet: KotlinSourceSet, dependencies: Any) {
        project.kotlinIdeMultiplatformImport.resolveDependencies(sourceSet)
            .filterIsInstance<IdeaKotlinResolvedBinaryDependency>()
            .filter { binaryDependency -> "stdlib" in binaryDependency.coordinates?.module.orEmpty() }
            .assertMatches(dependencies)
    }

    private fun createProjectWithDefaultStdlibEnabled() = buildProject {
        enableDependencyVerification(false)
        enableDefaultStdlibDependency(true)
        applyMultiplatformPlugin()
        repositories.mavenLocal()
        repositories.mavenCentralCacheRedirector()
    }

    private fun createProjectWithAndroidAndDefaultStdlibEnabled() = buildProject {
        enableDefaultStdlibDependency(false)
        enableDependencyVerification(false)
        applyMultiplatformPlugin()
        plugins.apply("com.android.library")
        androidExtension.compileSdkVersion(33)
        repositories.mavenLocal()
        repositories.mavenCentralCacheRedirector()
        repositories.google()
    }

    private fun commonStdlibDependency(kotlin: KotlinMultiplatformExtension) =
        binaryCoordinates("org.jetbrains.kotlin:kotlin-stdlib-common:${kotlin.coreLibrariesVersion}")

    private fun jvmStdlibDependencies(kotlin: KotlinMultiplatformExtension) = listOf(
        binaryCoordinates("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlin.coreLibrariesVersion}"),
        binaryCoordinates("org.jetbrains.kotlin:kotlin-stdlib-jdk7:${kotlin.coreLibrariesVersion}"),
        binaryCoordinates("org.jetbrains.kotlin:kotlin-stdlib:${kotlin.coreLibrariesVersion}"),
    )

    private fun jsStdlibDependency(kotlin: KotlinMultiplatformExtension) =
        binaryCoordinates("org.jetbrains.kotlin:kotlin-stdlib-js:${kotlin.coreLibrariesVersion}")

    private fun nativeStdlibDependency(kotlin: KotlinMultiplatformExtension) =
        binaryCoordinates("org.jetbrains.kotlin.native:stdlib:${kotlin.project.konanVersion}")
}

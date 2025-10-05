/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.sourceSets

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.kotlinJvmExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultKotlinSourceSetTest {


    @Test
    fun defaultJvmMainSources() {
        val jvmProject = buildProjectWithJvm { }

        val mainSourceSet = jvmProject.mainKotlinSourceSet
        assertEquals(
            jvmProject.expectedDefaultMainJvmSources,
            mainSourceSet.kotlin.sourceDirectories.files,
            "Default JVM sources are not expected",
        )
        assertTrue(
            mainSourceSet.generatedKotlin.sourceDirectories.files.isEmpty(),
            "Default generated sources are not empty",
        )
        assertEquals(
            jvmProject.expectedDefaultMainJvmSources,
            mainSourceSet.allKotlin.sourceDirectories.files,
            "Default all JVM sources are not expected",
        )
    }

    @Test
    fun defaultJvmTestSources() {
        val jvmProject = buildProjectWithJvm { }


        val testSourceSet = jvmProject.testKotlinSourceSet
        assertEquals(
            jvmProject.expectedDefaultTestJvmSources,
            testSourceSet.kotlin.sourceDirectories.files,
            "Default JVM sources are not expected",
        )
        assertTrue(
            testSourceSet.generatedKotlin.sourceDirectories.files.isEmpty(),
            "Default generated sources are not empty",
        )
        assertEquals(
            jvmProject.expectedDefaultTestJvmSources,
            testSourceSet.allKotlin.sourceDirectories.files,
            "Default all JVM sources are not expected",
        )
    }

    @Test
    fun addedJvmSourcesToKotlinAppearInAllKotlin() {
        val jvmProject = buildProjectWithJvm {}
        val additionalSourceSet = jvmProject.layout.projectDirectory.dir("src/another/kotlin")
        val mainSourceSet = jvmProject.mainKotlinSourceSet
        mainSourceSet.kotlin.srcDir(additionalSourceSet)

        assertEquals(
            jvmProject.expectedDefaultMainJvmSources + additionalSourceSet.asFile,
            mainSourceSet.kotlin.sourceDirectories.files,
            "JVM sources are not expected",
        )
        assertTrue(
            mainSourceSet.generatedKotlin.sourceDirectories.files.isEmpty(),
            "Generated sources are not empty",
        )
        assertEquals(
            jvmProject.expectedDefaultMainJvmSources + additionalSourceSet.asFile,
            mainSourceSet.allKotlin.sourceDirectories.files,
            "All JVM sources are not expected",
        )
    }

    @Test
    fun addedGeneratedSourcesAppearInAllKotlin() {
        val jvmProject = buildProjectWithJvm {}
        val generatedSourceSet = jvmProject.layout.projectDirectory.dir("src/gen/kotlin")
        val mainSourceSet = jvmProject.mainKotlinSourceSet
        mainSourceSet.generatedKotlin.srcDir(generatedSourceSet)

        assertEquals(
            jvmProject.expectedDefaultMainJvmSources,
            mainSourceSet.kotlin.sourceDirectories.files,
            "JVM sources are not expected",
        )
        assertEquals(
            setOf(generatedSourceSet.asFile),
            mainSourceSet.generatedKotlin.sourceDirectories.files,
            "Generated sources are not expected",
        )
        assertEquals(
            jvmProject.expectedDefaultMainJvmSources + generatedSourceSet.asFile,
            mainSourceSet.allKotlin.sourceDirectories.files,
            "All JVM sources are not expected",
        )
    }

    @Test
    fun defaultKmpCommonMainSources() {
        val kmpProject = buildProjectWithMPP {
            with(multiplatformExtension) {
                jvm()
                linuxX64()
            }
        }

        val commonMainSourceSet = kmpProject.commonMainKotlinSourceSet
        assertEquals(
            kmpProject.expectedDefaultCommonMainSources,
            commonMainSourceSet.kotlin.sourceDirectories.files,
            "Default common-main sources are not expected",
        )
        assertTrue(
            commonMainSourceSet.generatedKotlin.sourceDirectories.files.isEmpty(),
            "Default common-main generated sources are not empty",
        )
        assertEquals(
            kmpProject.expectedDefaultCommonMainSources,
            commonMainSourceSet.allKotlin.sourceDirectories.files,
            "Default all common-main sources are not expected",
        )
    }

    @Test
    fun defaultKmpCommonTestSources() {
        val kmpProject = buildProjectWithMPP {
            with(multiplatformExtension) {
                jvm()
                linuxX64()
            }
        }

        val commonTestSourceSet = kmpProject.multiplatformExtension.sourceSets.getDefaultByName("commonTest")
        val expectedDefaultCommonTestSources = setOf(
            kmpProject.layout.projectDirectory.dir("src/commonTest/kotlin").asFile,
        )
        assertEquals(
            expectedDefaultCommonTestSources,
            commonTestSourceSet.kotlin.sourceDirectories.files,
            "Default common-test sources are not expected",
        )
        assertTrue(
            commonTestSourceSet.generatedKotlin.sourceDirectories.files.isEmpty(),
            "Default common-test generated sources are not empty",
        )
        assertEquals(
            expectedDefaultCommonTestSources,
            commonTestSourceSet.allKotlin.sourceDirectories.files,
            "Default all common-test sources are not expected",
        )
    }

    @Test
    fun defaultKmpJvmMainSources() {
        val kmpProject = buildProjectWithMPP {
            with(multiplatformExtension) {
                jvm()
                linuxX64()
            }
        }

        val jvmMainSourceSet = kmpProject.jvmMainKotlinSourceSet
        assertEquals(
            kmpProject.expectedDefaultJvmMainSources,
            jvmMainSourceSet.kotlin.sourceDirectories.files,
            "Default jvm-main sources are not expected",
        )
        assertTrue(
            jvmMainSourceSet.generatedKotlin.sourceDirectories.files.isEmpty(),
            "Default jvm-main generated sources are not empty",
        )
        assertEquals(
            kmpProject.expectedDefaultJvmMainSources,
            jvmMainSourceSet.allKotlin.sourceDirectories.files,
            "Default all jvm-main sources are not expected",
        )
    }

    @Test
    fun defaultKmpJvmTestSources() {
        val kmpProject = buildProjectWithMPP {
            with(multiplatformExtension) {
                jvm()
                linuxX64()
            }
        }

        val jvmTestSourceSet = kmpProject.multiplatformExtension.sourceSets.getDefaultByName("jvmTest")
        val expectedDefaultJvmTestSources = setOf(
            kmpProject.layout.projectDirectory.dir("src/jvmTest/kotlin").asFile,
            kmpProject.layout.projectDirectory.dir("src/jvmTest/java").asFile,
        )
        assertEquals(
            expectedDefaultJvmTestSources,
            jvmTestSourceSet.kotlin.sourceDirectories.files,
            "Default jvm-test sources are not expected",
        )
        assertTrue(
            jvmTestSourceSet.generatedKotlin.sourceDirectories.files.isEmpty(),
            "Default jvm-test generated sources are not empty",
        )
        assertEquals(
            expectedDefaultJvmTestSources,
            jvmTestSourceSet.allKotlin.sourceDirectories.files,
            "Default all jvm-test sources are not expected",
        )
    }

    @Test
    fun defaultKmpLinuxX64MainSources() {
        val kmpProject = buildProjectWithMPP {
            with(multiplatformExtension) {
                jvm()
                linuxX64()
            }
        }

        val linuxX64MainSourceSet = kmpProject.linuxX64MainKotlinSourceSet
        assertEquals(
            kmpProject.expectedDefaultLinuxX64MainSources,
            linuxX64MainSourceSet.kotlin.sourceDirectories.files,
            "Default linuxx64-main sources are not expected",
        )
        assertTrue(
            linuxX64MainSourceSet.generatedKotlin.sourceDirectories.files.isEmpty(),
            "Default linuxx64-main generated sources are not empty",
        )
        assertEquals(
            kmpProject.expectedDefaultLinuxX64MainSources,
            linuxX64MainSourceSet.allKotlin.sourceDirectories.files,
            "Default all linuxx64-main sources are not expected",
        )
    }

    @Test
    fun defaultKmpLinuxX64TestSources() {
        val kmpProject = buildProjectWithMPP {
            with(multiplatformExtension) {
                jvm()
                linuxX64()
            }
        }

        val linuxX64TestSourceSet = kmpProject.multiplatformExtension.sourceSets.getByName("linuxX64Test") as DefaultKotlinSourceSet
        val expectedDefaultLinuxX64TestSources = setOf(
            kmpProject.layout.projectDirectory.dir("src/linuxX64Test/kotlin").asFile,
        )
        assertEquals(
            expectedDefaultLinuxX64TestSources,
            linuxX64TestSourceSet.kotlin.sourceDirectories.files,
            "Default linuxx64-test sources are not expected",
        )
        assertTrue(
            linuxX64TestSourceSet.generatedKotlin.sourceDirectories.files.isEmpty(),
            "Default linuxx64-test generated sources are not empty",
        )
        assertEquals(
            expectedDefaultLinuxX64TestSources,
            linuxX64TestSourceSet.allKotlin.sourceDirectories.files,
            "Default all linuxx64-test sources are not expected",
        )
    }

    @Test
    fun addedKmpCommonSourcesAppearInAllKotlin() {
        val kmpProject = buildProjectWithMPP {
            with(multiplatformExtension) {
                jvm()
                linuxX64()
            }
        }

        val additionalSources = kmpProject.layout.projectDirectory.dir("src/other/kotlin")
        val commonMainSourceSet = kmpProject.commonMainKotlinSourceSet
        commonMainSourceSet.kotlin.srcDir(additionalSources)
        assertEquals(
            kmpProject.expectedDefaultCommonMainSources + additionalSources.asFile,
            commonMainSourceSet.kotlin.sourceDirectories.files,
            "common-main sources are not expected",
        )
        assertTrue(
            commonMainSourceSet.generatedKotlin.sourceDirectories.files.isEmpty(),
            "common-main generated sources are not empty",
        )
        assertEquals(
            kmpProject.expectedDefaultCommonMainSources + additionalSources.asFile,
            commonMainSourceSet.allKotlin.sourceDirectories.files,
            "all common-main sources are not expected",
        )
        assertEquals(
            kmpProject.expectedDefaultJvmMainSources,
            kmpProject.jvmMainKotlinSourceSet.allKotlin.sourceDirectories.files,
            "All jvm-main sources are not expected"
        )
        assertEquals(
            kmpProject.expectedDefaultLinuxX64MainSources,
            kmpProject.linuxX64MainKotlinSourceSet.allKotlin.sourceDirectories.files,
            "All linuxx64-main sources are not expected"
        )
    }

    @Test
    fun addedKmpGeneratedSourcesAppearInAllCommon() {
        val kmpProject = buildProjectWithMPP {
            with(multiplatformExtension) {
                jvm()
                linuxX64()
            }
        }

        val generatedSources = kmpProject.layout.projectDirectory.dir("src/genCommon/kotlin")
        val commonMainSourceSet = kmpProject.commonMainKotlinSourceSet
        commonMainSourceSet.generatedKotlin.srcDir(generatedSources)
        assertEquals(
            kmpProject.expectedDefaultCommonMainSources,
            commonMainSourceSet.kotlin.sourceDirectories.files,
            "common-main sources are not expected",
        )
        assertEquals(
            setOf(generatedSources.asFile),
            commonMainSourceSet.generatedKotlin.sourceDirectories.files,
            "common-main generated sources are not expected",
        )
        assertEquals(
            kmpProject.expectedDefaultCommonMainSources + generatedSources.asFile,
            commonMainSourceSet.allKotlin.sourceDirectories.files,
            "all common-main sources are not expected",
        )
        assertEquals(
            kmpProject.expectedDefaultJvmMainSources,
            kmpProject.jvmMainKotlinSourceSet.allKotlin.sourceDirectories.files,
            "All jvm-main sources are not expected"
        )
        assertEquals(
            kmpProject.expectedDefaultLinuxX64MainSources,
            kmpProject.linuxX64MainKotlinSourceSet.allKotlin.sourceDirectories.files,
            "All linuxx64-main sources are not expected"
        )
    }

    @Test
    fun sourceSetFilterIsReflectedInAllKotlin() {
        val jvmProject = buildProjectWithJvm {
            mainKotlinSourceSet.kotlin {
                srcDir(layout.projectDirectory.dir("additionalSrc"))
                exclude("**/main.kt")
            }
        }

        jvmProject.layout.projectDirectory.dir("src/main/kotlin").file("main.kt").asFile
            .apply { parentFile.mkdirs() }
            .writeText("fun main() = println(\"main\")")
        val expectedFile = jvmProject.layout.projectDirectory.dir("additionalSrc").file("additional.kt")
            .asFile
            .apply { parentFile.mkdirs() }
        expectedFile.writeText("fun additional() = println(\"additional\")")

        val mainSourceSet = jvmProject.mainKotlinSourceSet

        assertEquals(
            setOf(expectedFile),
            mainSourceSet.kotlin.asFileTree.files,
            "Default JVM sources are not expected",
        )
        assertTrue(
            mainSourceSet.generatedKotlin.asFileTree.files.isEmpty(),
            "Default generated sources are not empty",
        )
        assertEquals(
            setOf(expectedFile),
            mainSourceSet.allKotlinSources.asFileTree.files,
            "Default all JVM sources are not expected",
        )
    }

    private fun NamedDomainObjectContainer<KotlinSourceSet>.getDefaultByName(name: String) = getByName(name) as DefaultKotlinSourceSet
    private val Project.mainKotlinSourceSet: DefaultKotlinSourceSet get() = kotlinJvmExtension.sourceSets.getDefaultByName("main")
    private val Project.testKotlinSourceSet get() = kotlinJvmExtension.sourceSets.getDefaultByName("test")
    private val Project.commonMainKotlinSourceSet get() = multiplatformExtension.sourceSets.getDefaultByName("commonMain")
    private val Project.jvmMainKotlinSourceSet get() = multiplatformExtension.sourceSets.getDefaultByName("jvmMain")
    private val Project.linuxX64MainKotlinSourceSet get() = multiplatformExtension.sourceSets.getDefaultByName("linuxX64Main")
    private val Project.expectedDefaultMainJvmSources
        get() = setOf(
            layout.projectDirectory.dir("src/main/kotlin").asFile,
            layout.projectDirectory.dir("src/main/java").asFile,
        )
    private val Project.expectedDefaultTestJvmSources
        get() = setOf(
            layout.projectDirectory.dir("src/test/kotlin").asFile,
            layout.projectDirectory.dir("src/test/java").asFile,
        )
    private val Project.expectedDefaultCommonMainSources
        get() = setOf(
            layout.projectDirectory.dir("src/commonMain/kotlin").asFile,
        )
    private val Project.expectedDefaultJvmMainSources
        get() = setOf(
            layout.projectDirectory.dir("src/jvmMain/kotlin").asFile,
            layout.projectDirectory.dir("src/jvmMain/java").asFile,
        )
    private val Project.expectedDefaultLinuxX64MainSources
        get() = setOf(
            layout.projectDirectory.dir("src/linuxX64Main/kotlin").asFile,
        )
}
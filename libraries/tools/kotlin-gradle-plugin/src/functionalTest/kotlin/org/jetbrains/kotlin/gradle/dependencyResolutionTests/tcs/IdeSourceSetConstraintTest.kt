/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName", "DuplicatedCode")

package org.jetbrains.kotlin.gradle.dependencyResolutionTests.tcs

import org.jetbrains.kotlin.gradle.dependencyResolutionTests.mavenCentralCacheRedirector
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.ide.IdeMultiplatformImport
import org.jetbrains.kotlin.gradle.util.applyMultiplatformPlugin
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.enableDependencyVerification
import org.jetbrains.kotlin.gradle.utils.androidExtension
import org.junit.Test
import java.util.*

class IdeSourceSetConstraintTest {
    @Test
    fun `test single target JVM project`() {
        val project = buildMppProject()
        val kotlin = project.multiplatformExtension
        kotlin.jvm()

        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val commonTest = kotlin.sourceSets.getByName("commonTest")
        val jvmMain = kotlin.sourceSets.getByName("jvmMain")
        val jvmTest = kotlin.sourceSets.getByName("jvmTest")

        project.evaluate()

        for (commonSourceSet in listOf(commonMain, commonTest)) {
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.isAndroid, isMatchExpected = false)
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.isJvmAndAndroid, isMatchExpected = false)
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.isLeaf, isMatchExpected = false)
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.isNative, isMatchExpected = false)
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.isSinglePlatformType, isMatchExpected = true)
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.unconstrained, isMatchExpected = true)
        }

        for (jvmSourceSet in listOf(jvmMain, jvmTest)) {
            assertConstraint(jvmSourceSet, IdeMultiplatformImport.SourceSetConstraint.isAndroid, isMatchExpected = false)
            assertConstraint(jvmSourceSet, IdeMultiplatformImport.SourceSetConstraint.isJvmAndAndroid, isMatchExpected = false)
            assertConstraint(jvmSourceSet, IdeMultiplatformImport.SourceSetConstraint.isLeaf, isMatchExpected = true)
            assertConstraint(jvmSourceSet, IdeMultiplatformImport.SourceSetConstraint.isNative, isMatchExpected = false)
            assertConstraint(jvmSourceSet, IdeMultiplatformImport.SourceSetConstraint.isSinglePlatformType, isMatchExpected = true)
            assertConstraint(jvmSourceSet, IdeMultiplatformImport.SourceSetConstraint.unconstrained, isMatchExpected = true)
        }
    }

    @Test
    fun `test single target JS project`() {
        val project = buildMppProject()
        val kotlin = project.multiplatformExtension
        kotlin.js(KotlinJsCompilerType.IR)

        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val commonTest = kotlin.sourceSets.getByName("commonTest")
        val jsMain = kotlin.sourceSets.getByName("jsMain")
        val jsTest = kotlin.sourceSets.getByName("jsTest")

        project.evaluate()

        for (commonSourceSet in listOf(commonMain, commonTest)) {
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.isAndroid, isMatchExpected = false)
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.isJvmAndAndroid, isMatchExpected = false)
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.isLeaf, isMatchExpected = false)
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.isNative, isMatchExpected = false)
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.isSinglePlatformType, isMatchExpected = true)
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.unconstrained, isMatchExpected = true)
        }

        for (jsSourceSet in listOf(jsMain, jsTest)) {
            assertConstraint(jsSourceSet, IdeMultiplatformImport.SourceSetConstraint.isAndroid, isMatchExpected = false)
            assertConstraint(jsSourceSet, IdeMultiplatformImport.SourceSetConstraint.isJvmAndAndroid, isMatchExpected = false)
            assertConstraint(jsSourceSet, IdeMultiplatformImport.SourceSetConstraint.isLeaf, isMatchExpected = true)
            assertConstraint(jsSourceSet, IdeMultiplatformImport.SourceSetConstraint.isNative, isMatchExpected = false)
            assertConstraint(jsSourceSet, IdeMultiplatformImport.SourceSetConstraint.isSinglePlatformType, isMatchExpected = true)
            assertConstraint(jsSourceSet, IdeMultiplatformImport.SourceSetConstraint.unconstrained, isMatchExpected = true)
        }
    }

    @Test
    fun `test single target Linux project`() {
        val project = buildMppProject()
        val kotlin = project.multiplatformExtension
        kotlin.linuxX64("linux")

        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val commonTest = kotlin.sourceSets.getByName("commonTest")
        val linuxMain = kotlin.sourceSets.getByName("linuxMain")
        val linuxTest = kotlin.sourceSets.getByName("linuxTest")

        project.evaluate()

        for (commonSourceSet in listOf(commonMain, commonTest)) {
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.isAndroid, isMatchExpected = false)
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.isJvmAndAndroid, isMatchExpected = false)
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.isLeaf, isMatchExpected = false)
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.isNative, isMatchExpected = true)
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.isSinglePlatformType, isMatchExpected = true)
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.unconstrained, isMatchExpected = true)
        }

        for (linuxSourceSet in listOf(linuxMain, linuxTest)) {
            assertConstraint(linuxSourceSet, IdeMultiplatformImport.SourceSetConstraint.isAndroid, isMatchExpected = false)
            assertConstraint(linuxSourceSet, IdeMultiplatformImport.SourceSetConstraint.isJvmAndAndroid, isMatchExpected = false)
            assertConstraint(linuxSourceSet, IdeMultiplatformImport.SourceSetConstraint.isLeaf, isMatchExpected = true)
            assertConstraint(linuxSourceSet, IdeMultiplatformImport.SourceSetConstraint.isNative, isMatchExpected = true)
            assertConstraint(linuxSourceSet, IdeMultiplatformImport.SourceSetConstraint.isSinglePlatformType, isMatchExpected = true)
            assertConstraint(linuxSourceSet, IdeMultiplatformImport.SourceSetConstraint.unconstrained, isMatchExpected = true)
        }
    }

    @Test
    fun `test JVM + Android project`() {
        val project = buildMppProjectWithAndroidPlugin()
        val kotlin = project.multiplatformExtension
        kotlin.jvm()
        kotlin.androidTarget()

        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val commonTest = kotlin.sourceSets.getByName("commonTest")
        val jvmMain = kotlin.sourceSets.getByName("jvmMain")
        val jvmTest = kotlin.sourceSets.getByName("jvmTest")
        val androidMain = kotlin.sourceSets.getByName("androidMain")
        val androidUnitTest = kotlin.sourceSets.getByName("androidUnitTest")
        val androidInstrumentedTest = kotlin.sourceSets.getByName("androidInstrumentedTest")

        project.evaluate()

        for (commonSourceSet in listOf(commonMain, commonTest)) {
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.isAndroid, isMatchExpected = false)
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.isJvmAndAndroid, isMatchExpected = true)
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.isLeaf, isMatchExpected = false)
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.isNative, isMatchExpected = false)
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.isSinglePlatformType, isMatchExpected = false)
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.unconstrained, isMatchExpected = true)
        }

        for (jvmSourceSet in listOf(jvmMain, jvmTest)) {
            assertConstraint(jvmSourceSet, IdeMultiplatformImport.SourceSetConstraint.isAndroid, isMatchExpected = false)
            assertConstraint(jvmSourceSet, IdeMultiplatformImport.SourceSetConstraint.isJvmAndAndroid, isMatchExpected = false)
            assertConstraint(jvmSourceSet, IdeMultiplatformImport.SourceSetConstraint.isLeaf, isMatchExpected = true)
            assertConstraint(jvmSourceSet, IdeMultiplatformImport.SourceSetConstraint.isNative, isMatchExpected = false)
            assertConstraint(jvmSourceSet, IdeMultiplatformImport.SourceSetConstraint.isSinglePlatformType, isMatchExpected = true)
            assertConstraint(jvmSourceSet, IdeMultiplatformImport.SourceSetConstraint.unconstrained, isMatchExpected = true)
        }

        for (androidSourceSet in listOf(androidMain, androidUnitTest, androidInstrumentedTest)) {
            assertConstraint(androidSourceSet, IdeMultiplatformImport.SourceSetConstraint.isAndroid, isMatchExpected = true)
            assertConstraint(androidSourceSet, IdeMultiplatformImport.SourceSetConstraint.isJvmAndAndroid, isMatchExpected = false)
            assertConstraint(androidSourceSet, IdeMultiplatformImport.SourceSetConstraint.isLeaf, isMatchExpected = true)
            assertConstraint(androidSourceSet, IdeMultiplatformImport.SourceSetConstraint.isNative, isMatchExpected = false)
            assertConstraint(androidSourceSet, IdeMultiplatformImport.SourceSetConstraint.isSinglePlatformType, isMatchExpected = true)
            assertConstraint(androidSourceSet, IdeMultiplatformImport.SourceSetConstraint.unconstrained, isMatchExpected = true)
        }
    }

    @Test
    fun `test bamboo JVM project`() {
        val project = buildMppProject()
        val kotlin = project.multiplatformExtension
        kotlin.linuxX64("linux")
        kotlin.jvm()

        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val commonTest = kotlin.sourceSets.getByName("commonTest")
        val linuxMain = kotlin.sourceSets.getByName("linuxMain")
        val linuxTest = kotlin.sourceSets.getByName("linuxTest")
        val jvmMain = kotlin.sourceSets.getByName("jvmMain")
        val jvmTest = kotlin.sourceSets.getByName("jvmTest")
        val jvmIntermediateMain = kotlin.sourceSets.create("jvmIntermediateMain") { intermediate ->
            intermediate.dependsOn(commonMain)
            jvmMain.dependsOn(intermediate)
        }
        val jvmIntermediateTest = kotlin.sourceSets.create("jvmIntermediateTest") { intermediate ->
            intermediate.dependsOn(commonTest)
            jvmTest.dependsOn(intermediate)
        }

        project.evaluate()

        for (commonSourceSet in listOf(commonMain, commonTest)) {
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.isAndroid, isMatchExpected = false)
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.isJvmAndAndroid, isMatchExpected = false)
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.isLeaf, isMatchExpected = false)
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.isNative, isMatchExpected = false)
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.isSinglePlatformType, isMatchExpected = false)
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.unconstrained, isMatchExpected = true)
        }

        for (linuxSourceSet in listOf(linuxMain, linuxTest)) {
            assertConstraint(linuxSourceSet, IdeMultiplatformImport.SourceSetConstraint.isAndroid, isMatchExpected = false)
            assertConstraint(linuxSourceSet, IdeMultiplatformImport.SourceSetConstraint.isJvmAndAndroid, isMatchExpected = false)
            assertConstraint(linuxSourceSet, IdeMultiplatformImport.SourceSetConstraint.isLeaf, isMatchExpected = true)
            assertConstraint(linuxSourceSet, IdeMultiplatformImport.SourceSetConstraint.isNative, isMatchExpected = true)
            assertConstraint(linuxSourceSet, IdeMultiplatformImport.SourceSetConstraint.isSinglePlatformType, isMatchExpected = true)
            assertConstraint(linuxSourceSet, IdeMultiplatformImport.SourceSetConstraint.unconstrained, isMatchExpected = true)
        }

        for (jvmSourceSet in listOf(jvmMain, jvmTest)) {
            assertConstraint(jvmSourceSet, IdeMultiplatformImport.SourceSetConstraint.isAndroid, isMatchExpected = false)
            assertConstraint(jvmSourceSet, IdeMultiplatformImport.SourceSetConstraint.isJvmAndAndroid, isMatchExpected = false)
            assertConstraint(jvmSourceSet, IdeMultiplatformImport.SourceSetConstraint.isLeaf, isMatchExpected = true)
            assertConstraint(jvmSourceSet, IdeMultiplatformImport.SourceSetConstraint.isNative, isMatchExpected = false)
            assertConstraint(jvmSourceSet, IdeMultiplatformImport.SourceSetConstraint.isSinglePlatformType, isMatchExpected = true)
            assertConstraint(jvmSourceSet, IdeMultiplatformImport.SourceSetConstraint.unconstrained, isMatchExpected = true)
        }

        for (intermediateSourceSet in listOf(jvmIntermediateMain, jvmIntermediateTest)) {
            assertConstraint(intermediateSourceSet, IdeMultiplatformImport.SourceSetConstraint.isAndroid, isMatchExpected = false)
            assertConstraint(intermediateSourceSet, IdeMultiplatformImport.SourceSetConstraint.isJvmAndAndroid, isMatchExpected = false)
            assertConstraint(intermediateSourceSet, IdeMultiplatformImport.SourceSetConstraint.isLeaf, isMatchExpected = false)
            assertConstraint(intermediateSourceSet, IdeMultiplatformImport.SourceSetConstraint.isNative, isMatchExpected = false)
            assertConstraint(intermediateSourceSet, IdeMultiplatformImport.SourceSetConstraint.isSinglePlatformType, isMatchExpected = true)
            assertConstraint(intermediateSourceSet, IdeMultiplatformImport.SourceSetConstraint.unconstrained, isMatchExpected = true)
        }
    }

    @Test
    fun `test bamboo Linux project`() {
        val project = buildMppProject()
        val kotlin = project.multiplatformExtension
        kotlin.linuxX64("linux")
        kotlin.jvm()

        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val commonTest = kotlin.sourceSets.getByName("commonTest")
        val linuxMain = kotlin.sourceSets.getByName("linuxMain")
        val linuxTest = kotlin.sourceSets.getByName("linuxTest")
        val jvmMain = kotlin.sourceSets.getByName("jvmMain")
        val jvmTest = kotlin.sourceSets.getByName("jvmTest")
        val linuxIntermediateMain = kotlin.sourceSets.create("linuxIntermediateMain") { intermediate ->
            intermediate.dependsOn(commonMain)
            linuxMain.dependsOn(intermediate)
        }
        val linuxIntermediateTest = kotlin.sourceSets.create("linuxIntermediateTest") { intermediate ->
            intermediate.dependsOn(commonTest)
            linuxTest.dependsOn(intermediate)
        }

        project.evaluate()

        for (commonSourceSet in listOf(commonMain, commonTest)) {
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.isAndroid, isMatchExpected = false)
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.isJvmAndAndroid, isMatchExpected = false)
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.isLeaf, isMatchExpected = false)
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.isNative, isMatchExpected = false)
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.isSinglePlatformType, isMatchExpected = false)
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.unconstrained, isMatchExpected = true)
        }

        for (linuxSourceSet in listOf(linuxMain, linuxTest)) {
            assertConstraint(linuxSourceSet, IdeMultiplatformImport.SourceSetConstraint.isAndroid, isMatchExpected = false)
            assertConstraint(linuxSourceSet, IdeMultiplatformImport.SourceSetConstraint.isJvmAndAndroid, isMatchExpected = false)
            assertConstraint(linuxSourceSet, IdeMultiplatformImport.SourceSetConstraint.isLeaf, isMatchExpected = true)
            assertConstraint(linuxSourceSet, IdeMultiplatformImport.SourceSetConstraint.isNative, isMatchExpected = true)
            assertConstraint(linuxSourceSet, IdeMultiplatformImport.SourceSetConstraint.isSinglePlatformType, isMatchExpected = true)
            assertConstraint(linuxSourceSet, IdeMultiplatformImport.SourceSetConstraint.unconstrained, isMatchExpected = true)
        }

        for (jvmSourceSet in listOf(jvmMain, jvmTest)) {
            assertConstraint(jvmSourceSet, IdeMultiplatformImport.SourceSetConstraint.isAndroid, isMatchExpected = false)
            assertConstraint(jvmSourceSet, IdeMultiplatformImport.SourceSetConstraint.isJvmAndAndroid, isMatchExpected = false)
            assertConstraint(jvmSourceSet, IdeMultiplatformImport.SourceSetConstraint.isLeaf, isMatchExpected = true)
            assertConstraint(jvmSourceSet, IdeMultiplatformImport.SourceSetConstraint.isNative, isMatchExpected = false)
            assertConstraint(jvmSourceSet, IdeMultiplatformImport.SourceSetConstraint.isSinglePlatformType, isMatchExpected = true)
            assertConstraint(jvmSourceSet, IdeMultiplatformImport.SourceSetConstraint.unconstrained, isMatchExpected = true)
        }

        for (intermediateSourceSet in listOf(linuxIntermediateMain, linuxIntermediateTest)) {
            assertConstraint(intermediateSourceSet, IdeMultiplatformImport.SourceSetConstraint.isAndroid, isMatchExpected = false)
            assertConstraint(intermediateSourceSet, IdeMultiplatformImport.SourceSetConstraint.isJvmAndAndroid, isMatchExpected = false)
            assertConstraint(intermediateSourceSet, IdeMultiplatformImport.SourceSetConstraint.isLeaf, isMatchExpected = false)
            assertConstraint(intermediateSourceSet, IdeMultiplatformImport.SourceSetConstraint.isNative, isMatchExpected = true)
            assertConstraint(intermediateSourceSet, IdeMultiplatformImport.SourceSetConstraint.isSinglePlatformType, isMatchExpected = true)
            assertConstraint(intermediateSourceSet, IdeMultiplatformImport.SourceSetConstraint.unconstrained, isMatchExpected = true)
        }
    }

    @Test
    fun `test bamboo JS project`() {
        val project = buildMppProject()
        val kotlin = project.multiplatformExtension
        kotlin.linuxX64("linux")
        kotlin.js(KotlinJsCompilerType.IR)

        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val commonTest = kotlin.sourceSets.getByName("commonTest")
        val linuxMain = kotlin.sourceSets.getByName("linuxMain")
        val linuxTest = kotlin.sourceSets.getByName("linuxTest")
        val jsMain = kotlin.sourceSets.getByName("jsMain")
        val jsTest = kotlin.sourceSets.getByName("jsTest")
        val jsIntermediateMain = kotlin.sourceSets.create("jsIntermediateMain") { intermediate ->
            intermediate.dependsOn(commonMain)
            jsMain.dependsOn(intermediate)
        }
        val jsIntermediateTest = kotlin.sourceSets.create("jsIntermediateTest") { intermediate ->
            intermediate.dependsOn(commonTest)
            jsTest.dependsOn(intermediate)
        }

        project.evaluate()

        for (commonSourceSet in listOf(commonMain, commonTest)) {
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.isAndroid, isMatchExpected = false)
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.isJvmAndAndroid, isMatchExpected = false)
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.isLeaf, isMatchExpected = false)
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.isNative, isMatchExpected = false)
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.isSinglePlatformType, isMatchExpected = false)
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.unconstrained, isMatchExpected = true)
        }

        for (linuxSourceSet in listOf(linuxMain, linuxTest)) {
            assertConstraint(linuxSourceSet, IdeMultiplatformImport.SourceSetConstraint.isAndroid, isMatchExpected = false)
            assertConstraint(linuxSourceSet, IdeMultiplatformImport.SourceSetConstraint.isJvmAndAndroid, isMatchExpected = false)
            assertConstraint(linuxSourceSet, IdeMultiplatformImport.SourceSetConstraint.isLeaf, isMatchExpected = true)
            assertConstraint(linuxSourceSet, IdeMultiplatformImport.SourceSetConstraint.isNative, isMatchExpected = true)
            assertConstraint(linuxSourceSet, IdeMultiplatformImport.SourceSetConstraint.isSinglePlatformType, isMatchExpected = true)
            assertConstraint(linuxSourceSet, IdeMultiplatformImport.SourceSetConstraint.unconstrained, isMatchExpected = true)
        }

        for (jsSourceSet in listOf(jsMain, jsTest)) {
            assertConstraint(jsSourceSet, IdeMultiplatformImport.SourceSetConstraint.isAndroid, isMatchExpected = false)
            assertConstraint(jsSourceSet, IdeMultiplatformImport.SourceSetConstraint.isJvmAndAndroid, isMatchExpected = false)
            assertConstraint(jsSourceSet, IdeMultiplatformImport.SourceSetConstraint.isLeaf, isMatchExpected = true)
            assertConstraint(jsSourceSet, IdeMultiplatformImport.SourceSetConstraint.isNative, isMatchExpected = false)
            assertConstraint(jsSourceSet, IdeMultiplatformImport.SourceSetConstraint.isSinglePlatformType, isMatchExpected = true)
            assertConstraint(jsSourceSet, IdeMultiplatformImport.SourceSetConstraint.unconstrained, isMatchExpected = true)
        }

        for (intermediateSourceSet in listOf(jsIntermediateMain, jsIntermediateTest)) {
            assertConstraint(intermediateSourceSet, IdeMultiplatformImport.SourceSetConstraint.isAndroid, isMatchExpected = false)
            assertConstraint(intermediateSourceSet, IdeMultiplatformImport.SourceSetConstraint.isJvmAndAndroid, isMatchExpected = false)
            assertConstraint(intermediateSourceSet, IdeMultiplatformImport.SourceSetConstraint.isLeaf, isMatchExpected = false)
            assertConstraint(intermediateSourceSet, IdeMultiplatformImport.SourceSetConstraint.isNative, isMatchExpected = false)
            assertConstraint(intermediateSourceSet, IdeMultiplatformImport.SourceSetConstraint.isSinglePlatformType, isMatchExpected = true)
            assertConstraint(intermediateSourceSet, IdeMultiplatformImport.SourceSetConstraint.unconstrained, isMatchExpected = true)
        }
    }

    @Test
    fun `test JVM + JS + native targets with natural hierarchy`() {
        val project = buildMppProject()
        val kotlin = project.multiplatformExtension

        kotlin.applyDefaultHierarchyTemplate()

        kotlin.jvm()
        kotlin.js(KotlinJsCompilerType.IR)
        kotlin.linuxX64()
        kotlin.linuxArm64()

        val jsMain = kotlin.sourceSets.getByName("jsMain")
        val jsTest = kotlin.sourceSets.getByName("jsTest")
        val jvmMain = kotlin.sourceSets.getByName("jvmMain")
        val jvmTest = kotlin.sourceSets.getByName("jvmTest")
        val linuxArm64Main = kotlin.sourceSets.getByName("linuxArm64Main")
        val linuxArm64Test = kotlin.sourceSets.getByName("linuxArm64Test")
        val linuxX64Main = kotlin.sourceSets.getByName("linuxX64Main")
        val linuxX64Test = kotlin.sourceSets.getByName("linuxX64Test")
        val nativeMain = kotlin.sourceSets.getByName("nativeMain")
        val nativeTest = kotlin.sourceSets.getByName("nativeTest")
        val linuxMain = kotlin.sourceSets.getByName("linuxMain")
        val linuxTest = kotlin.sourceSets.getByName("linuxTest")
        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val commonTest = kotlin.sourceSets.getByName("commonTest")

        project.evaluate()

        for (commonSourceSet in listOf(commonMain, commonTest)) {
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.isAndroid, isMatchExpected = false)
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.isJvmAndAndroid, isMatchExpected = false)
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.isLeaf, isMatchExpected = false)
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.isNative, isMatchExpected = false)
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.isSinglePlatformType, isMatchExpected = false)
            assertConstraint(commonSourceSet, IdeMultiplatformImport.SourceSetConstraint.unconstrained, isMatchExpected = true)
        }

        for (jvmSourceSet in listOf(jvmMain, jvmTest)) {
            assertConstraint(jvmSourceSet, IdeMultiplatformImport.SourceSetConstraint.isAndroid, isMatchExpected = false)
            assertConstraint(jvmSourceSet, IdeMultiplatformImport.SourceSetConstraint.isJvmAndAndroid, isMatchExpected = false)
            assertConstraint(jvmSourceSet, IdeMultiplatformImport.SourceSetConstraint.isLeaf, isMatchExpected = true)
            assertConstraint(jvmSourceSet, IdeMultiplatformImport.SourceSetConstraint.isNative, isMatchExpected = false)
            assertConstraint(jvmSourceSet, IdeMultiplatformImport.SourceSetConstraint.isSinglePlatformType, isMatchExpected = true)
            assertConstraint(jvmSourceSet, IdeMultiplatformImport.SourceSetConstraint.unconstrained, isMatchExpected = true)
        }

        for (jsSourceSet in listOf(jsMain, jsTest)) {
            assertConstraint(jsSourceSet, IdeMultiplatformImport.SourceSetConstraint.isAndroid, isMatchExpected = false)
            assertConstraint(jsSourceSet, IdeMultiplatformImport.SourceSetConstraint.isJvmAndAndroid, isMatchExpected = false)
            assertConstraint(jsSourceSet, IdeMultiplatformImport.SourceSetConstraint.isLeaf, isMatchExpected = true)
            assertConstraint(jsSourceSet, IdeMultiplatformImport.SourceSetConstraint.isNative, isMatchExpected = false)
            assertConstraint(jsSourceSet, IdeMultiplatformImport.SourceSetConstraint.isSinglePlatformType, isMatchExpected = true)
            assertConstraint(jsSourceSet, IdeMultiplatformImport.SourceSetConstraint.unconstrained, isMatchExpected = true)
        }

        for (nativeSharedSourceSet in listOf(linuxMain, linuxTest, nativeMain, nativeTest)) {
            assertConstraint(nativeSharedSourceSet, IdeMultiplatformImport.SourceSetConstraint.isAndroid, isMatchExpected = false)
            assertConstraint(nativeSharedSourceSet, IdeMultiplatformImport.SourceSetConstraint.isJvmAndAndroid, isMatchExpected = false)
            assertConstraint(nativeSharedSourceSet, IdeMultiplatformImport.SourceSetConstraint.isLeaf, isMatchExpected = false)
            assertConstraint(nativeSharedSourceSet, IdeMultiplatformImport.SourceSetConstraint.isNative, isMatchExpected = true)
            assertConstraint(nativeSharedSourceSet, IdeMultiplatformImport.SourceSetConstraint.isSinglePlatformType, isMatchExpected = true)
            assertConstraint(nativeSharedSourceSet, IdeMultiplatformImport.SourceSetConstraint.unconstrained, isMatchExpected = true)
        }

        for (nativeLeafSourceSet in listOf(linuxX64Main, linuxX64Test, linuxArm64Main, linuxArm64Test)) {
            assertConstraint(nativeLeafSourceSet, IdeMultiplatformImport.SourceSetConstraint.isAndroid, isMatchExpected = false)
            assertConstraint(nativeLeafSourceSet, IdeMultiplatformImport.SourceSetConstraint.isJvmAndAndroid, isMatchExpected = false)
            assertConstraint(nativeLeafSourceSet, IdeMultiplatformImport.SourceSetConstraint.isLeaf, isMatchExpected = true)
            assertConstraint(nativeLeafSourceSet, IdeMultiplatformImport.SourceSetConstraint.isNative, isMatchExpected = true)
            assertConstraint(nativeLeafSourceSet, IdeMultiplatformImport.SourceSetConstraint.isSinglePlatformType, isMatchExpected = true)
            assertConstraint(nativeLeafSourceSet, IdeMultiplatformImport.SourceSetConstraint.unconstrained, isMatchExpected = true)
        }
    }

    private fun assertConstraint(
        sourceSet: KotlinSourceSet,
        constraint: IdeMultiplatformImport.SourceSetConstraint,
        isMatchExpected: Boolean,
    ) {
        assert(constraint(sourceSet) == isMatchExpected) {
            "Constraint mismatch: ${constraintNames[constraint]} for source set ${sourceSet.name} is expected to be $isMatchExpected"
        }
    }

    private val constraintNames = IdentityHashMap<IdeMultiplatformImport.SourceSetConstraint, String>().apply {
        this[IdeMultiplatformImport.SourceSetConstraint.isAndroid] = "isAndroid"
        this[IdeMultiplatformImport.SourceSetConstraint.isJvmAndAndroid] = "isJvmAndAndroid"
        this[IdeMultiplatformImport.SourceSetConstraint.isLeaf] = "isLeaf"
        this[IdeMultiplatformImport.SourceSetConstraint.isNative] = "isNative"
        this[IdeMultiplatformImport.SourceSetConstraint.isSinglePlatformType] = "isSinglePlatformType"
        this[IdeMultiplatformImport.SourceSetConstraint.unconstrained] = "unconstrained"
    }

    private fun buildMppProject() = buildProject {
        enableDependencyVerification(false)
        applyMultiplatformPlugin()
        repositories.mavenLocal()
        repositories.mavenCentralCacheRedirector()
    }

    private fun buildMppProjectWithAndroidPlugin() = buildProject {
        enableDependencyVerification(false)
        applyMultiplatformPlugin()
        plugins.apply("com.android.library")
        androidExtension.compileSdkVersion(33)
        repositories.mavenLocal()
        repositories.mavenCentralCacheRedirector()
        repositories.google()
    }
}

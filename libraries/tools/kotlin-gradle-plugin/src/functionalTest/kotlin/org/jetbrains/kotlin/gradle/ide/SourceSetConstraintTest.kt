/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName", "DuplicatedCode")

package org.jetbrains.kotlin.gradle.ide

import org.jetbrains.kotlin.gradle.applyMultiplatformPlugin
import org.jetbrains.kotlin.gradle.buildProject
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.enableDependencyVerification
import org.jetbrains.kotlin.gradle.kpm.idea.mavenCentralCacheRedirector
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.ide.IdeMultiplatformImport
import org.jetbrains.kotlin.gradle.utils.androidExtension
import org.junit.Test
import java.util.IdentityHashMap

class SourceSetConstraintTest {
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
            commonSourceSet.assertConstraints(
                isAndroid = false,
                isJvmAndAndroid = false,
                isLeaf = false,
                isNative = false,
                isSinglePlatformType = true,
            )
        }

        for (jvmSourceSet in listOf(jvmMain, jvmTest)) {
            jvmSourceSet.assertConstraints(
                isAndroid = false,
                isJvmAndAndroid = false,
                isLeaf = true,
                isNative = false,
                isSinglePlatformType = true,
            )
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
            commonSourceSet.assertConstraints(
                isAndroid = false,
                isJvmAndAndroid = false,
                isLeaf = false,
                isNative = false,
                isSinglePlatformType = true,
            )
        }

        for (jsSourceSet in listOf(jsMain, jsTest)) {
            jsSourceSet.assertConstraints(
                isAndroid = false,
                isJvmAndAndroid = false,
                isLeaf = true,
                isNative = false,
                isSinglePlatformType = true,
            )
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
            commonSourceSet.assertConstraints(
                isAndroid = false,
                isJvmAndAndroid = false,
                isLeaf = false,
                isNative = true,
                isSinglePlatformType = true,
            )
        }

        for (linuxSourceSet in listOf(linuxMain, linuxTest)) {
            linuxSourceSet.assertConstraints(
                isAndroid = false,
                isJvmAndAndroid = false,
                isLeaf = true,
                isNative = true,
                isSinglePlatformType = true,
            )
        }
    }

    @Test
    fun `test JVM + Android project`() {
        val project = buildMppProjectWithAndroidPlugin()
        val kotlin = project.multiplatformExtension
        kotlin.jvm()
        kotlin.android()

        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val commonTest = kotlin.sourceSets.getByName("commonTest")
        val jvmMain = kotlin.sourceSets.getByName("jvmMain")
        val jvmTest = kotlin.sourceSets.getByName("jvmTest")
        val androidMain = kotlin.sourceSets.getByName("androidMain")
        val androidUnitTest = kotlin.sourceSets.getByName("androidTest")
        val androidInstrumentedTest = kotlin.sourceSets.getByName("androidAndroidTest")

        project.evaluate()

        commonMain.assertConstraints(
            isAndroid = false,
            isJvmAndAndroid = false, // TODO (kirpichenkov): investigate and explain/fix metadata jvm + android shared non-test compilations
            isLeaf = false,
            isNative = false,
            isSinglePlatformType = false,
        )

        commonTest.assertConstraints(
            isAndroid = false,
            isJvmAndAndroid = true,
            isLeaf = false,
            isNative = false,
            isSinglePlatformType = false,
        )

        for (jvmSourceSet in listOf(jvmMain, jvmTest)) {
            jvmSourceSet.assertConstraints(
                isAndroid = false,
                isJvmAndAndroid = false,
                isLeaf = true,
                isNative = false,
                isSinglePlatformType = true,
            )
        }

        for (androidSourceSet in listOf(androidMain, androidUnitTest, androidInstrumentedTest)) {
            androidSourceSet.assertConstraints(
                isAndroid = true,
                isJvmAndAndroid = false,
                isLeaf = true,
                isNative = false,
                isSinglePlatformType = true,
            )
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
            commonSourceSet.assertConstraints(
                isAndroid = false,
                isJvmAndAndroid = false,
                isLeaf = false,
                isNative = false,
                isSinglePlatformType = false,
            )
        }

        for (linuxSourceSet in listOf(linuxMain, linuxTest)) {
            linuxSourceSet.assertConstraints(
                isAndroid = false,
                isJvmAndAndroid = false,
                isLeaf = true,
                isNative = true,
                isSinglePlatformType = true,
            )
        }

        for (jvmSourceSet in listOf(jvmMain, jvmTest)) {
            jvmSourceSet.assertConstraints(
                isAndroid = false,
                isJvmAndAndroid = false,
                isLeaf = true,
                isNative = false,
                isSinglePlatformType = true,
            )
        }

        for (intermediateSourceSet in listOf(jvmIntermediateMain, jvmIntermediateTest)) {
            intermediateSourceSet.assertConstraints(
                isAndroid = false,
                isJvmAndAndroid = false,
                isLeaf = false,
                isNative = false,
                isSinglePlatformType = true,
            )
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
            commonSourceSet.assertConstraints(
                isAndroid = false,
                isJvmAndAndroid = false,
                isLeaf = false,
                isNative = false,
                isSinglePlatformType = false,
            )
        }

        for (linuxSourceSet in listOf(linuxMain, linuxTest)) {
            linuxSourceSet.assertConstraints(
                isAndroid = false,
                isJvmAndAndroid = false,
                isLeaf = true,
                isNative = true,
                isSinglePlatformType = true,
            )
        }

        for (jvmSourceSet in listOf(jvmMain, jvmTest)) {
            jvmSourceSet.assertConstraints(
                isAndroid = false,
                isJvmAndAndroid = false,
                isLeaf = true,
                isNative = false,
                isSinglePlatformType = true,
            )
        }

        for (intermediateSourceSet in listOf(linuxIntermediateMain, linuxIntermediateTest)) {
            intermediateSourceSet.assertConstraints(
                isAndroid = false,
                isJvmAndAndroid = false,
                isLeaf = false,
                isNative = true,
                isSinglePlatformType = true,
            )
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
            commonSourceSet.assertConstraints(
                isAndroid = false,
                isJvmAndAndroid = false,
                isLeaf = false,
                isNative = false,
                isSinglePlatformType = false,
            )
        }

        for (linuxSourceSet in listOf(linuxMain, linuxTest)) {
            linuxSourceSet.assertConstraints(
                isAndroid = false,
                isJvmAndAndroid = false,
                isLeaf = true,
                isNative = true,
                isSinglePlatformType = true,
            )
        }

        for (jsSourceSet in listOf(jsMain, jsTest)) {
            jsSourceSet.assertConstraints(
                isAndroid = false,
                isJvmAndAndroid = false,
                isLeaf = true,
                isNative = false,
                isSinglePlatformType = true,
            )
        }

        for (intermediateSourceSet in listOf(jsIntermediateMain, jsIntermediateTest)) {
            intermediateSourceSet.assertConstraints(
                isAndroid = false,
                isJvmAndAndroid = false,
                isLeaf = false,
                isNative = false,
                isSinglePlatformType = true,
            )
        }
    }

    @Test
    fun `test JVM + JS + native targets with natural hierarchy`() {
        val project = buildMppProject()
        val kotlin = project.multiplatformExtension

        kotlin.targetHierarchy.default()

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
            commonSourceSet.assertConstraints(
                isAndroid = false,
                isJvmAndAndroid = false,
                isLeaf = false,
                isNative = false,
                isSinglePlatformType = false,
            )
        }

        for (jvmSourceSet in listOf(jvmMain, jvmTest)) {
            jvmSourceSet.assertConstraints(
                isAndroid = false,
                isJvmAndAndroid = false,
                isLeaf = true,
                isNative = false,
                isSinglePlatformType = true,
            )
        }

        for (jsSourceSet in listOf(jsMain, jsTest)) {
            jsSourceSet.assertConstraints(
                isAndroid = false,
                isJvmAndAndroid = false,
                isLeaf = true,
                isNative = false,
                isSinglePlatformType = true,
            )
        }

        for (nativeSharedSourceSet in listOf(linuxMain, linuxTest, nativeMain, nativeTest)) {
            nativeSharedSourceSet.assertConstraints(
                isAndroid = false,
                isJvmAndAndroid = false,
                isLeaf = false,
                isNative = true,
                isSinglePlatformType = true,
            )
        }

        for (nativeLeafSourceSet in listOf(linuxX64Main, linuxX64Test, linuxArm64Main, linuxArm64Test)) {
            nativeLeafSourceSet.assertConstraints(
                isAndroid = false,
                isJvmAndAndroid = false,
                isLeaf = true,
                isNative = true,
                isSinglePlatformType = true,
            )
        }
    }

    private fun KotlinSourceSet.assertConstraints(
        isAndroid: Boolean,
        isJvmAndAndroid: Boolean,
        isLeaf: Boolean,
        isNative: Boolean,
        isSinglePlatformType: Boolean,
    ) {
        assertConstraint(this, IdeMultiplatformImport.SourceSetConstraint.isAndroid, isAndroid)
        assertConstraint(this, IdeMultiplatformImport.SourceSetConstraint.isJvmAndAndroid, isJvmAndAndroid)
        assertConstraint(this, IdeMultiplatformImport.SourceSetConstraint.isLeaf, isLeaf)
        assertConstraint(this, IdeMultiplatformImport.SourceSetConstraint.isNative, isNative)
        assertConstraint(this, IdeMultiplatformImport.SourceSetConstraint.isSinglePlatformType, isSinglePlatformType)
        assertConstraint(this, IdeMultiplatformImport.SourceSetConstraint.unconstrained, true)
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

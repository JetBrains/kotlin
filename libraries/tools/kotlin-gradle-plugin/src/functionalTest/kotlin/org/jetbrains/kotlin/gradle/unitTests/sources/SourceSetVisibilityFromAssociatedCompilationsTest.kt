/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.gradle.unitTests.sources

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.getVisibleSourceSetsFromAssociateCompilations
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.util.capitalizeDecapitalize.decapitalizeAsciiOnly
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SourceSetVisibilityFromAssociatedCompilationsTest {
    private val project = buildProjectWithMPP()
    private val kotlin = project.multiplatformExtension.apply {
        applyDefaultHierarchyTemplate()
    }

    @Test
    fun testBasicSuccessful() {
        kotlin.jvm()
        kotlin.linuxX64()
        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val commonTest = kotlin.sourceSets.getByName("commonTest")
        val jvmMain = kotlin.sourceSets.getByName("jvmMain")
        val jvmTest = kotlin.sourceSets.getByName("jvmTest")

        jvmTest.assertVisibleSourceSetsFromAssociatedCompilations(commonMain, jvmMain)
        commonTest.assertVisibleSourceSetsFromAssociatedCompilations(commonMain)
    }

    @Test
    fun testBambooSourceSetStructureVisibility() {
        kotlin.apply {
            jvm()
            iosArm64()
            iosX64()

            sourceSets.apply {
                // NB: nativeTest sees iosMain because it participates only in iosX64 and iosArm64 test compilations.
                nativeTest.get().assertVisibleSourceSetsFromAssociatedCompilations(
                    iosMain.get(),
                    appleMain.get(),
                    nativeMain.get(),
                    commonMain.get(),
                )
                commonTest.get().assertVisibleSourceSetsFromAssociatedCompilations(commonMain.get())
            }
        }
    }

    @Test
    fun testCustomCompilationWithoutAssociation() {
        val jvm = kotlin.jvm()
        val commonTest = kotlin.sourceSets.getByName("commonTest")
        val jvmSpecialTest = kotlin.sourceSets.create("jvmSpecialTest")

        jvmSpecialTest.dependsOn(commonTest)

        val jvmSpecialTestCompilation = jvm.compilations.create("specialTest") // note: No association with jvmMain!
        assertEquals(jvmSpecialTest, jvmSpecialTestCompilation.defaultSourceSet)

        jvmSpecialTest.assertVisibleSourceSetsFromAssociatedCompilations(*arrayOf())
    }

    @Test
    fun testCustomCompilationAssociatesWithMainCompilation() {
        val jvm = kotlin.jvm()
        kotlin.linuxX64()

        val commonTest = kotlin.sourceSets.getByName("commonTest")
        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val jvmMain = kotlin.sourceSets.getByName("jvmMain")
        val jvmSpecialTest = kotlin.sourceSets.create("jvmSpecialTest")

        jvmSpecialTest.dependsOn(commonTest)

        val jvmSpecialTestCompilation = jvm.compilations.create("specialTest")
        assertEquals(jvmSpecialTest, jvmSpecialTestCompilation.defaultSourceSet)

        jvmSpecialTestCompilation.associateWith(jvm.compilations.getByName("main"))

        jvmSpecialTest.assertVisibleSourceSetsFromAssociatedCompilations(commonMain, jvmMain)
    }

    @Test
    fun testInferenceForHierarchy() {
        kotlin.jvm()
        kotlin.js()
        kotlin.linuxX64("linux")

        listOf("Main", "Test").forEach { suffix ->
            val common = kotlin.sourceSets.getByName("common$suffix")
            val jvmAndJs = kotlin.sourceSets.create("jvmAndJs$suffix")
            val linuxAndJs = kotlin.sourceSets.create("linuxAndJs$suffix")
            val jvm = kotlin.sourceSets.getByName("jvm$suffix")
            val linux = kotlin.sourceSets.getByName("linux$suffix")
            val js = kotlin.sourceSets.getByName("js$suffix")

            jvmAndJs.dependsOn(common)
            linuxAndJs.dependsOn(common)

            jvm.dependsOn(jvmAndJs)
            js.dependsOn(jvmAndJs)
            js.dependsOn(linuxAndJs)
            linux.dependsOn(linuxAndJs)
        }

        "commonMain".assertVisibleSourceSetsFromAssociatedCompilations(*arrayOf())
        "jvmMain".assertVisibleSourceSetsFromAssociatedCompilations(*arrayOf())
        "jvmAndJsMain".assertVisibleSourceSetsFromAssociatedCompilations(*arrayOf())

        "commonTest".assertVisibleSourceSetsFromAssociatedCompilations("commonMain")
        "jvmAndJsTest".assertVisibleSourceSetsFromAssociatedCompilations("commonMain", "jvmAndJsMain")
        "linuxAndJsTest".assertVisibleSourceSetsFromAssociatedCompilations("commonMain", "linuxAndJsMain")
        "jvmTest".assertVisibleSourceSetsFromAssociatedCompilations("commonMain", "jvmAndJsMain", "jvmMain")
    }

    @Test
    fun testInferenceThroughIndirectAssociation() {
        kotlin.jvm()
        kotlin.js()

        listOf(null, "Main", "Test", "IntegrationTest").zipWithNext().forEach { (previousSuffix, suffix) ->
            val common = kotlin.sourceSets.maybeCreate("common$suffix")
            val jvm = kotlin.sourceSets.maybeCreate("jvm$suffix")
            val js = kotlin.sourceSets.maybeCreate("js$suffix")

            if (previousSuffix != null) {
                assertNotNull(suffix)
                jvm.dependsOn(common)
                js.dependsOn(common)

                val previousJvmCompilation = kotlin.jvm().compilations.maybeCreate(previousSuffix.decapitalizeAsciiOnly())
                val jvmCompilation = kotlin.jvm().compilations.maybeCreate(suffix.decapitalizeAsciiOnly())
                assertEquals(jvm, jvmCompilation.defaultSourceSet)
                jvmCompilation.associateWith(previousJvmCompilation)

                val previousJsCompilation = kotlin.js().compilations.maybeCreate(previousSuffix.decapitalizeAsciiOnly())
                val jsCompilation = kotlin.js().compilations.maybeCreate(suffix.decapitalizeAsciiOnly())
                assertEquals(js, jsCompilation.defaultSourceSet)
                jsCompilation.associateWith(previousJsCompilation)
            }
        }

        "commonIntegrationTest".assertVisibleSourceSetsFromAssociatedCompilations("commonMain", "commonTest")
        "jvmIntegrationTest".assertVisibleSourceSetsFromAssociatedCompilations("commonMain", "jvmMain", "commonTest", "jvmTest")
    }

    @Test
    fun testVisibleSourceSetsIntersection() {
        /* Given Following Project Structure:
              commonTest
                  |
               nativeTest
                  |
               linuxTest
                /       \
           linuxX64Test  linuxArm64Test -associatedWith-> linuxArm64Main2

           by default each ${x}Test source sees ${x}Main and its transitive dependencies.
        */
        kotlin.linuxArm64 {
            val test = compilations.getByName("test")
            val main2 = compilations.create("main2")
            test.associateWith(main2)
        }
        kotlin.linuxX64()

        // Assert that linuxTest can see only source sets that
        // *ALL* its underlying compilations (linuxX64Test, linuxArm64Test)
        // can see as well (i.e. intersection of their visible source sets)
        "linuxX64Test".assertVisibleSourceSetsFromAssociatedCompilations(
            "commonMain",
            "nativeMain",
            "linuxMain",
            "linuxX64Main", // unique for given source set
        )
        "linuxArm64Test".assertVisibleSourceSetsFromAssociatedCompilations(
            "commonMain",
            "nativeMain",
            "linuxMain",
            "linuxArm64Main", // unique for given source set
            "linuxArm64Main2" // unique for given source set
        )
        "linuxTest".assertVisibleSourceSetsFromAssociatedCompilations("commonMain", "nativeMain", "linuxMain")
    }

    private fun String.assertVisibleSourceSetsFromAssociatedCompilations(
        vararg expectedVisibleSourceSets: String
    ) = assertEquals(
        expectedVisibleSourceSets.toSet(),
        getVisibleSourceSetsFromAssociateCompilations(kotlin.sourceSets.getByName(this)).map { it.name }.toSet()
    )

    private fun KotlinSourceSet.assertVisibleSourceSetsFromAssociatedCompilations(
        vararg expectedVisibleSourceSets: KotlinSourceSet
    ) = assertEquals(
        expectedVisibleSourceSets.map { it.name }.toSet(),
        getVisibleSourceSetsFromAssociateCompilations(this).map { it.name }.toSet()
    )
}
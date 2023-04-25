/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.gradle.unitTests.sources

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.UnsatisfiedSourceSetVisibilityException
import org.jetbrains.kotlin.gradle.plugin.sources.checkSourceSetVisibilityRequirements
import org.jetbrains.kotlin.gradle.plugin.sources.getVisibleSourceSetsFromAssociateCompilations
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.util.capitalizeDecapitalize.decapitalizeAsciiOnly
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class SourceSetVisibilityInferenceTest {
    private val project = buildProjectWithMPP()
    private val kotlin = project.multiplatformExtension.apply {
        targetHierarchy.default()
    }

    @Test
    fun testBasicSuccessful() {
        kotlin.jvm()
        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val commonTest = kotlin.sourceSets.getByName("commonTest")
        val jvmMain = kotlin.sourceSets.getByName("jvmMain")
        val jvmTest = kotlin.sourceSets.getByName("jvmTest")

        commonTest.requiresVisibilityOf(commonMain)
        jvmTest.requiresVisibilityOf(jvmMain)

        jvmTest.checkInferredSourceSetsVisibility(commonMain, jvmMain)
        checkSourceSetVisibilityRequirements(kotlin.sourceSets)
    }

    @Test
    fun testFailureWithNoAssociation() {
        val jvm = kotlin.jvm()
        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val commonTest = kotlin.sourceSets.getByName("commonTest")
        val jvmMain = kotlin.sourceSets.getByName("jvmMain")
        val jvmSpecialTest = kotlin.sourceSets.create("jvmSpecialTest")

        commonTest.requiresVisibilityOf(commonMain)
        jvmSpecialTest.requiresVisibilityOf(jvmMain)
        jvmSpecialTest.dependsOn(commonTest)

        val jvmTestCompilation = jvm.compilations.getByName("test")
        val jvmSpecialTestCompilation = jvm.compilations.create("specialTest") // note: No association with jvmMain!
        assertEquals(jvmSpecialTest, jvmSpecialTestCompilation.defaultSourceSet)

        jvmSpecialTest.checkInferredSourceSetsVisibility(*arrayOf())

        assertFailsWith<UnsatisfiedSourceSetVisibilityException> {
            checkSourceSetVisibilityRequirements(setOf(jvmSpecialTest))
        }.apply {
            assertEquals(jvmSpecialTest, sourceSet)
            assertEquals(emptyList(), visibleSourceSets)
            assertEquals(setOf(jvmMain), requiredButNotVisible)
            assertEquals(setOf(jvmSpecialTestCompilation), compilations)
        }

        assertFailsWith<UnsatisfiedSourceSetVisibilityException> {
            checkSourceSetVisibilityRequirements(setOf(commonTest))
        }.apply {
            assertEquals(commonTest, sourceSet)
            assertEquals(emptyList(), visibleSourceSets)
            assertEquals(setOf(commonMain), requiredButNotVisible)
            assertEquals(setOf(jvmTestCompilation, jvmSpecialTestCompilation), compilations)
        }
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

            if (suffix == "Test") {
                jvmAndJs.requiresVisibilityOf(kotlin.sourceSets.getByName("jvmAndJsMain"))
                linuxAndJs.requiresVisibilityOf(kotlin.sourceSets.getByName("linuxAndJsMain"))
                jvm.requiresVisibilityOf(kotlin.sourceSets.getByName("jvmMain"))
                linux.requiresVisibilityOf(kotlin.sourceSets.getByName("linuxMain"))
                js.requiresVisibilityOf(kotlin.sourceSets.getByName("jsMain"))
            }

            jvmAndJs.dependsOn(common)
            linuxAndJs.dependsOn(common)

            jvm.dependsOn(jvmAndJs)
            js.dependsOn(jvmAndJs)
            js.dependsOn(linuxAndJs)
            linux.dependsOn(linuxAndJs)
        }

        "commonMain".checkInferredSourceSetsVisibility(*arrayOf())
        "jvmMain".checkInferredSourceSetsVisibility(* arrayOf())
        "jvmAndJsMain".checkInferredSourceSetsVisibility(*arrayOf())

        "commonTest".checkInferredSourceSetsVisibility("commonMain")
        "jvmAndJsTest".checkInferredSourceSetsVisibility("commonMain", "jvmAndJsMain")
        "linuxAndJsTest".checkInferredSourceSetsVisibility("commonMain", "linuxAndJsMain")
        "jvmTest".checkInferredSourceSetsVisibility("commonMain", "jvmAndJsMain", "jvmMain")

        checkSourceSetVisibilityRequirements(kotlin.sourceSets)
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
                common.requiresVisibilityOf(kotlin.sourceSets.getByName("common$previousSuffix"))
                jvm.requiresVisibilityOf(kotlin.sourceSets.getByName("jvm$previousSuffix"))
                js.requiresVisibilityOf(kotlin.sourceSets.getByName("js$previousSuffix"))
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

        "commonIntegrationTest".checkInferredSourceSetsVisibility("commonMain", "commonTest")
        "jvmIntegrationTest".checkInferredSourceSetsVisibility("commonMain", "jvmMain", "commonTest", "jvmTest")
        checkSourceSetVisibilityRequirements(kotlin.sourceSets)
    }

    @Test
    fun testInferenceThroughIndirectAssociationWithMissingAssociateWith() {
        kotlin.jvm()
        kotlin.js()

        listOf(null, "Main", "Test", "IntegrationTest").zipWithNext().forEach { (previousSuffix, suffix) ->
            val common = kotlin.sourceSets.maybeCreate("common$suffix")
            val jvm = kotlin.sourceSets.maybeCreate("jvm$suffix")
            val js = kotlin.sourceSets.maybeCreate("js$suffix")

            if (previousSuffix != null) {
                assertNotNull(suffix)
                common.requiresVisibilityOf(kotlin.sourceSets.getByName("common$previousSuffix"))
                jvm.requiresVisibilityOf(kotlin.sourceSets.getByName("jvm$previousSuffix"))
                js.requiresVisibilityOf(kotlin.sourceSets.getByName("js$previousSuffix"))
                jvm.dependsOn(common)
                js.dependsOn(common)

                val jvmCompilation = kotlin.jvm().compilations.maybeCreate(suffix.decapitalizeAsciiOnly())
                assertEquals(jvm, jvmCompilation.defaultSourceSet)

                val jsCompilation = kotlin.js().compilations.maybeCreate(suffix.decapitalizeAsciiOnly())
                assertEquals(js, jsCompilation.defaultSourceSet)
            }
        }

        val commonIntegrationTest = kotlin.sourceSets.getByName("commonIntegrationTest")
        commonIntegrationTest.requiresVisibilityOf(kotlin.sourceSets.getByName("commonMain"))

        assertFailsWith<UnsatisfiedSourceSetVisibilityException> {
            checkSourceSetVisibilityRequirements(setOf(commonIntegrationTest))
        }.apply {
            assertEquals(commonIntegrationTest, this.sourceSet)
            assertEquals(setOf(), visibleSourceSets.map { it.name }.toSet())
            assertEquals(setOf("commonTest", "commonMain"), requiredButNotVisible.map { it.name }.toSet())
            assertEquals(
                setOf(
                    kotlin.jvm().compilations.getByName("integrationTest"),
                    kotlin.js().compilations.getByName("integrationTest")
                ),
                compilations.toSet()
            )
        }
    }

    private fun String.checkInferredSourceSetsVisibility(
        vararg expectedVisibleSourceSets: String
    ) = assertEquals(
        expectedVisibleSourceSets.toSet(),
        getVisibleSourceSetsFromAssociateCompilations(kotlin.sourceSets.getByName(this).internal.compilations).map { it.name }.toSet()
    )

    private fun KotlinSourceSet.checkInferredSourceSetsVisibility(
        vararg expectedVisibleSourceSets: KotlinSourceSet
    ) = assertEquals(
        expectedVisibleSourceSets.map { it.name }.toSet(),
        getVisibleSourceSetsFromAssociateCompilations(this.internal.compilations).map { it.name }.toSet()
    )
}
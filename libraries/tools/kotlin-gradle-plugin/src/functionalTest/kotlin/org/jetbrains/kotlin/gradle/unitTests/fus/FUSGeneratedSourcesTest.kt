/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.fus

import org.jetbrains.kotlin.gradle.dsl.kotlinJvmExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FUSGeneratedSourcesTest {

    @Test
    fun kmpGeneratedSourcesAreNotReported() {
        val project = buildProjectWithMPP(
            preApplyCode = enableFusOnCI
        ) {
            with(multiplatformExtension) {
                jvm()
                linuxX64()
            }
        }

        project.evaluate()

        assertTrue(
            project.collectedFusConfigurationTimeMetrics.booleanMetrics.keys.none {
                it.name == BooleanMetrics.KOTLIN_GENERATED_SOURCES_USED.name
            },
            "FUS event is present for generated sources"
        )
    }

    @Test
    fun jvmGeneratedSourcesAreNotReported() {
        val project = buildProjectWithJvm(
            preApplyCode = enableFusOnCI
        )

        project.evaluate()

        assertTrue(
            project.collectedFusConfigurationTimeMetrics.booleanMetrics.keys.none {
                it.name == BooleanMetrics.KOTLIN_GENERATED_SOURCES_USED.name
            },
            "FUS event is present for generated sources"
        )
    }

    @Test
    fun kmpGeneratedSourcesAreReported() {
        val project = buildProjectWithMPP(preApplyCode = enableFusOnCI) {
            with(multiplatformExtension) {
                jvm()
                linuxX64()

                sourceSets.commonMain {
                    generatedKotlin.srcDir("src/commonGen")
                }
            }
        }

        project.evaluate()

        assertNotNull(
            project.collectedFusConfigurationTimeMetrics.booleanMetrics.entries.singleOrNull {
                it.key.name == BooleanMetrics.KOTLIN_GENERATED_SOURCES_USED.name && it.value
            }
        )
    }

    @Test
    fun jvmGeneratedSourcesAreReported() {
        val project = buildProjectWithJvm(preApplyCode = enableFusOnCI) {
            kotlinJvmExtension.sourceSets.getByName("main").generatedKotlin.srcDir("src/gen")
        }

        project.evaluate()

        assertNotNull(
            project.collectedFusConfigurationTimeMetrics.booleanMetrics.entries.singleOrNull {
                it.key.name == BooleanMetrics.KOTLIN_GENERATED_SOURCES_USED.name && it.value
            }
        )
    }

    @Test
    fun generatorTaskIsReported() {
        val project = buildProjectWithJvm(preApplyCode = enableFusOnCI) {
            val generatorTask = project.tasks.register("generator") {
                val outputDirectory = project.layout.projectDirectory.dir("src/main/kotlinGen")
                it.outputs.dir(outputDirectory)
                it.doLast {
                    outputDirectory.file("generated.kt").asFile.writeText(
                        //language=kotlin
                        """
                        fun printHello() {
                             println("hello")
                        }
                        """.trimIndent()
                    )
                }
            }

            kotlinJvmExtension.sourceSets.getByName("main").generatedKotlin.srcDir(generatorTask)
        }

        project.evaluate()

        assertNotNull(
            project.collectedFusConfigurationTimeMetrics.booleanMetrics.entries.singleOrNull {
                it.key.name == BooleanMetrics.KOTLIN_GENERATED_SOURCES_USED.name && it.value
            }
        )
    }
}
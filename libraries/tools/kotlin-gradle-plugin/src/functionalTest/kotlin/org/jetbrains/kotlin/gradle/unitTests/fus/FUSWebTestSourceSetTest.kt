/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.fus

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalWasmDsl::class)
class FUSWebTestSourceSetTest {
    @Test
    fun emptyWebTestSourcesAreNotReported() {
        val project = buildProjectWithMPP(preApplyCode = enableFusOnCI) {
            with(multiplatformExtension) {
                js { nodejs() }
                wasmJs()
            }
        }

        project.evaluate()

        assertTrue(
            project.collectedFusConfigurationTimeMetrics.booleanMetrics.keys.none {
                it.name == BooleanMetrics.KOTLIN_WEB_TEST_SOURCES_USED.name
            },
            "FUS event is present for webTest sources"
        )
    }

    @Test
    fun explicitlyAddedWebTestSourcesAreReported() {
        val project = buildProjectWithMPP(preApplyCode = enableFusOnCI) {
            with(multiplatformExtension) {
                js { browser() }
                wasmJs { browser() }
            }
        }

        val sharedFile = project.layout.projectDirectory.file("src/webTest/kotlin/shared.kt").asFile
        sharedFile.parentFile.mkdirs()
        sharedFile.writeText("fun shared() = 1")

        project.evaluate()

        assertNotNull(
            project.collectedFusConfigurationTimeMetrics.booleanMetrics.entries.singleOrNull {
                it.key == BooleanMetrics.KOTLIN_WEB_TEST_SOURCES_USED && it.value
            }
        )
    }

}
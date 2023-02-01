/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.regressionTests

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.junit.Test
import kotlin.test.fail

class KT37051CInteropArtifactTest {

    @Test
    fun `test - cinterop artifact on linuxX64`() {
        val project = buildProjectWithMPP()
        val kotlin = project.multiplatformExtension
        val linuxTarget = kotlin.linuxX64()
        val mainCompilation = linuxTarget.compilations.getByName("main")
        val cinterop = mainCompilation.cinterops.create("libc")

        val apiElements = project.configurations.getByName(linuxTarget.apiElementsConfigurationName)

        if (apiElements.allDependencies.isNotEmpty()) {
            fail("Expected no dependencies in apiElements: ${apiElements.allDependencies}")
        }

        if (apiElements.artifacts.size != 2) {
            fail("Expected two artifacts in apiElements: main output and cinterop. Found: ${apiElements.artifacts}")
        }

        val cinteropArtifact = apiElements.artifacts.filter { artifact -> artifact.classifier == "cinterop-libc" }
            .apply { if (size != 1) fail("Expected only one cinterop artifact: Found $this") }
            .first()

        if (cinteropArtifact.buildDependencies.getDependencies(null) != setOf(project.tasks.getByName(cinterop.interopProcessingTaskName))) {
            fail("Expected cinterop artifact to contain the corresponding cinterop process as task dependency")
        }
    }
}
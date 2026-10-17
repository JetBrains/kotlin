/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.regressionTests

import org.jetbrains.kotlin.gradle.dsl.metadataTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEquals
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import kotlin.test.Test

class KT85034MppMetadataTargetInvokePublicationCreated {
    @Test
    fun `KT-85034 - metadata target mavenPublication action is invoked for root publication`() {
        val project = buildProjectWithMPP(
            preApplyCode = {
                plugins.apply("maven-publish")
            }
        )
        val kotlin = project.multiplatformExtension
        val configuredPublications = mutableListOf<String>()

        kotlin.metadataTarget.mavenPublication {
            configuredPublications += name
        }
        kotlin.jvm()

        project.evaluate()

        assertEquals(
            listOf("kotlinMultiplatform"),
            configuredPublications,
        ) {
            "kotlin metadata target mavenPublication action should be invoked only once for root publication"
        }
    }
}

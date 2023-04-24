/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.regressionTests

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.javaSourceSets
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import kotlin.test.Test
import kotlin.test.fail

class KT41506WithJavaSourceSet {
    @Test
    fun `test that arbitrary compilation can be created with java enabled in Multiplatform project`() {
        val project = buildProjectWithMPP {
            kotlin {
                jvm {
                    compilations.create("integrationTest")
                    withJava()
                }
            }
        }
        project.evaluate()

        val jvmTarget = project.multiplatformExtension.targets.getByName("jvm") as KotlinJvmTarget
        if ("integrationTest" !in jvmTarget.compilations.names) {
            fail("Expected jvm compilation 'integrationTest' is not found")
        }

        if ("jvmIntegrationTest" !in project.multiplatformExtension.sourceSets.names) {
            fail("Expected kotlin source set 'jvmIntegrationTest' id not found")
        }

        if ("integrationTest" !in project.javaSourceSets.names) {
            fail("Expected java source set 'integrationTest' id not found")
        }
    }
}
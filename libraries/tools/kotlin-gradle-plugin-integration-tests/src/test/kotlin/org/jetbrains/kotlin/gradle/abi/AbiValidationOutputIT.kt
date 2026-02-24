/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalAbiValidation::class)

package org.jetbrains.kotlin.gradle.abi

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.abi.utils.abiValidation
import org.jetbrains.kotlin.gradle.abi.utils.jvmProject
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName

@JvmGradlePluginTests
class AbiValidationOutputIT : KGPBaseTest() {

    /**
     * Split the contents of the `api` directory between `checkApiDir` and `updateKotlinAbi`
     */
    @DisplayName("KT-84365")
    @GradleTest
    fun testSharedReferenceDirectory(
        gradleVersion: GradleVersion,
    ) {
        abstract class ApiTask : DefaultTask() {
            @get:InputFile
            @get:PathSensitive(PathSensitivity.RELATIVE)
            abstract val file: RegularFileProperty

            @TaskAction
            fun action() {
            }
        }

        jvmProject(gradleVersion) {
            projectPath.toFile().resolve("api").mkdirs()
            projectPath.toFile().resolve("api").resolve("my.file").createNewFile()

            buildScriptInjection {
                project.tasks.register("checkApiDir", ApiTask::class.java) {
                    it.file.set(project.layout.projectDirectory.dir("api").file("my.file"))
                }
            }

            abiValidation()

            build("checkApiDir", "updateKotlinAbi")
        }
    }
}
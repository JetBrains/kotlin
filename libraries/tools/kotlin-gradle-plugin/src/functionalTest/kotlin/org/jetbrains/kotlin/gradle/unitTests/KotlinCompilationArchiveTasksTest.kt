/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_CREATE_ARCHIVE_TASKS_FOR_CUSTOM_COMPILATIONS
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationArchiveTasks
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.fail

class KotlinCompilationArchiveTasksTest {
    private fun Project.enableKotlinCompilationArchiveTasksCreation(enabled: Boolean = true) {
        propertiesExtension.set(KOTLIN_CREATE_ARCHIVE_TASKS_FOR_CUSTOM_COMPILATIONS, enabled.toString())
    }

    private val Project.kotlinCompilationsArchiveTasks: KotlinCompilationArchiveTasks
        get() = extensions.getByName("kotlinCompilationsArchiveTasks") as KotlinCompilationArchiveTasks

    val testProject: Project = buildProject {
        enableKotlinCompilationArchiveTasksCreation()
        applyMultiplatformPlugin()

        kotlin {
            jvm()
            linuxX64()
        }
    }

    @Test
    fun `custom jvm compilation has archive task`() {
        testProject.kotlin {
            jvm {
                val customCompilation = compilations.create("custom")
                val archiveTaskProvider = project.kotlinCompilationsArchiveTasks.getArchiveTaskOrNull(customCompilation)
                assertNotNull(archiveTaskProvider)
                val archiveFile = archiveTaskProvider.get().archiveFile.get().asFile
                if (!archiveFile.endsWith("libs/jvmCustom.jar")) fail("Unexpected archive file for compilation $customCompilation")

                assertEquals(archiveTaskProvider.name, customCompilation.internal.archiveTaskName)
            }
        }
    }

    @Test
    fun `custom native compilation has archive`() {
        testProject.kotlin {
            linuxX64 {
                val customCompilation = compilations.create("custom")
                val archiveTaskProvider = project.kotlinCompilationsArchiveTasks.getArchiveTaskOrNull(customCompilation)
                assertNotNull(archiveTaskProvider)
                val archiveFile = archiveTaskProvider.get().archiveFile.get().asFile
                if (!archiveFile.endsWith("libs/linuxX64Custom.klib"))
                    fail("Unexpected archive file for compilation $customCompilation: $archiveFile")
            }
        }
    }

    @Test
    fun `main and test compilations should not have archive task`() {
        testProject.multiplatformExtension.targets.flatMap { it.compilations }.forEach { compilation ->
            if (compilation.internal.archiveTaskName != null)
                fail("Archive tasks should not be created for default compilations, but $compilation has it")
        }
    }

    @Test
    fun `archive tasks should not be created for custom compilation when feature flag is not set`() {
        val project = buildProject {
            enableKotlinCompilationArchiveTasksCreation(enabled = false)
            applyMultiplatformPlugin()
            kotlin {
                jvm().compilations.create("custom")
                linuxX64().compilations.create("custom")
            }
        }
        project.multiplatformExtension.targets.flatMap { it.compilations }.forEach { compilation ->
            if (compilation.internal.archiveTaskName != null)
                fail("Archive tasks should not be created")
        }
    }

    @Test
    fun `archive tasks should not be created for any android compilation`() {
        val project = buildProject {
            enableKotlinCompilationArchiveTasksCreation()
            applyMultiplatformPlugin()
            androidLibrary {
                compileSdk = 33
                productFlavors {
                    create("customFlavor")
                }
            }
            kotlin {
                linuxX64()
                androidTarget()
            }
        }

        project.multiplatformExtension.androidTarget().compilations.forEach { compilation ->
            if (compilation.internal.archiveTaskName != null)
                fail("Archive tasks should not be created for $compilation")
        }
    }
}
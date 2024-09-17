/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinApiPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinJvmFactory
import org.jetbrains.kotlin.gradle.util.buildProject
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class KotlinCompileToolTest {

    @get:Rule
    val tmpDir = TemporaryFolder()

    private lateinit var project: Project
    private lateinit var kotlinJvmFactory: KotlinJvmFactory

    @Before
    fun setUpProject() {
        project = buildProject {}
        kotlinJvmFactory = project.plugins.apply(KotlinApiPlugin::class.java)
    }

    @Test
    fun callingSourceShouldAddSources() {
        val task = kotlinJvmFactory.registerKotlinJvmCompileTask("test", "test").get()
        val kotlinSourceFiles = project.testKotlinSourceFiles()

        task.source(kotlinSourceFiles[0], kotlinSourceFiles[1])
        task.source(kotlinSourceFiles[2])

        assertFalse(task.sources.isEmpty)
        assertEquals(
            kotlinSourceFiles,
            task.sources.files.toList(),
        )
    }

    @Test
    fun callingSetSourceShouldReplaceSources() {
        val task = kotlinJvmFactory.registerKotlinJvmCompileTask("test", "test").get()
        val kotlinSourceFiles = project.testKotlinSourceFiles()

        task.source(kotlinSourceFiles[0], kotlinSourceFiles[1])
        task.setSource(kotlinSourceFiles[2])

        assertFalse(task.sources.isEmpty)
        assertEquals(
            listOf(kotlinSourceFiles[2]),
            task.sources.files.toList(),
        )
    }

    private fun Project.testKotlinSourceFiles(): List<File> {
        val kotlinDir = tmpDir.newFolder("kotlin")
        return listOf(
            kotlinDir.resolveCreating("a.kt"),
            kotlinDir.resolveCreating("b.kt"),
            kotlinDir.resolveCreating("c.kt"),
        )
    }

    private fun File.resolveCreating(fileName: String) = resolve(fileName).also {
        it.createNewFile()
    }
}
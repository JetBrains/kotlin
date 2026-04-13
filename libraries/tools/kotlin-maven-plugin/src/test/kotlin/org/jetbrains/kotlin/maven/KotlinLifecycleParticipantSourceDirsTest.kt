/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven

import org.apache.maven.model.Build
import org.apache.maven.model.Model
import org.apache.maven.project.MavenProject
import java.io.File
import kotlin.test.*


class KotlinLifecycleParticipantSourceDirsTest {
    // Check <sourceDirectory>
    @Test
    fun `hasMavenBuildSourceDirectoryOverride - returns false when source dir is default`() {
        val basedir = File("/project")
        val project = projectWithSourceDirectory(File(basedir, "src/main/java").absolutePath, basedir)
        assertFalse(KotlinLifecycleParticipant.hasMavenBuildSourceDirectoryOverride(project))
    }

    @Test
    fun `hasMavenBuildSourceDirectoryOverride - returns true when source dir is custom`() {
        val basedir = File("/project")
        val project = projectWithSourceDirectory(File(basedir, "src/customMain/kotlin").absolutePath, basedir)
        assertTrue(KotlinLifecycleParticipant.hasMavenBuildSourceDirectoryOverride(project))
    }

    @Test
    fun `hasMavenBuildSourceDirectoryOverride - returns false when source dir is null`() {
        val basedir = File("/project")
        val project = projectWithSourceDirectory(null, basedir)
        assertFalse(KotlinLifecycleParticipant.hasMavenBuildSourceDirectoryOverride(project))
    }

    @Test
    fun `hasMavenBuildSourceDirectoryOverride - handles relative paths`() {
        val basedir = File("/project")
        val project = projectWithSourceDirectory("src/customMain/kotlin", basedir)
        assertTrue(KotlinLifecycleParticipant.hasMavenBuildSourceDirectoryOverride(project))
    }

    @Test
    fun `hasMavenBuildSourceDirectoryOverride - relative default path returns false`() {
        val basedir = File("/project")
        val project = projectWithSourceDirectory("src/main/java", basedir)
        assertFalse(KotlinLifecycleParticipant.hasMavenBuildSourceDirectoryOverride(project))
    }

    // Check <testSourceDirectory>
    @Test
    fun `hasMavenBuildTestSourceDirectoryOverride - returns false when test source dir is default`() {
        val basedir = File("/project")
        val project = projectWithTestSourceDirectory(File(basedir, "src/test/java").absolutePath, basedir)
        assertFalse(KotlinLifecycleParticipant.hasMavenBuildTestSourceDirectoryOverride(project))
    }

    @Test
    fun `hasMavenBuildTestSourceDirectoryOverride - returns true when test source dir is custom`() {
        val basedir = File("/project")
        val project = projectWithTestSourceDirectory(File(basedir, "src/customTest/kotlin").absolutePath, basedir)
        assertTrue(KotlinLifecycleParticipant.hasMavenBuildTestSourceDirectoryOverride(project))
    }

    @Test
    fun `hasMavenBuildTestSourceDirectoryOverride - returns false when test source dir is null`() {
        val basedir = File("/project")
        val project = projectWithTestSourceDirectory(null, basedir)
        assertFalse(KotlinLifecycleParticipant.hasMavenBuildTestSourceDirectoryOverride(project))
    }

    @Test
    fun `hasMavenBuildTestSourceDirectoryOverride - handles relative paths`() {
        val basedir = File("/project")
        val project = projectWithTestSourceDirectory("src/customTest/kotlin", basedir)
        assertTrue(KotlinLifecycleParticipant.hasMavenBuildTestSourceDirectoryOverride(project))
    }

    @Test
    fun `hasMavenBuildTestSourceDirectoryOverride - relative default path returns false`() {
        val basedir = File("/project")
        val project = projectWithTestSourceDirectory("src/test/java", basedir)
        assertFalse(KotlinLifecycleParticipant.hasMavenBuildTestSourceDirectoryOverride(project))
    }

    // -------------------------------------------------------------------------
    // Helper builders
    // -------------------------------------------------------------------------

    private fun projectWithSourceDirectory(sourceDir: String?, basedir: File): MavenProject {
        val model = Model()
        val build = Build()
        build.sourceDirectory = sourceDir
        model.build = build
        val project = MavenProject(model)
        project.setFile(File(basedir, "pom.xml"))
        return project
    }

    private fun projectWithTestSourceDirectory(testSourceDir: String?, basedir: File): MavenProject {
        val model = Model()
        val build = Build()
        build.testSourceDirectory = testSourceDir
        model.build = build
        val project = MavenProject(model)
        project.setFile(File(basedir, "pom.xml"))
        return project
    }
}

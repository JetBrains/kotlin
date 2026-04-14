/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven

import org.apache.maven.model.Build
import org.apache.maven.model.Model
import org.apache.maven.project.MavenProject
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.*


class KotlinLifecycleParticipantSourceDirsTest {
    // Check <sourceDirectory>
    @Test
    fun `hasMavenBuildSourceDirectoryOverride - returns false when source dir is default`(@TempDir tempDir: Path) {
        val baseDir = tempDir.toFile()
        val sourceDir = tempDir.resolve("src").resolve("main").resolve("java").toFile()
        val project = projectWithSourceDirectory(sourceDir.absolutePath, baseDir)
        assertFalse(KotlinLifecycleParticipant.hasMavenBuildSourceDirectoryOverride(project))
    }

    @Test
    fun `hasMavenBuildSourceDirectoryOverride - returns true when source dir is custom`(@TempDir tempDir: Path) {
        val baseDir = tempDir.toFile()
        val sourceDir = tempDir.resolve("src").resolve("customMain").resolve("kotlin").toFile()
        val project = projectWithSourceDirectory(sourceDir.absolutePath, baseDir)
        assertTrue(KotlinLifecycleParticipant.hasMavenBuildSourceDirectoryOverride(project))
    }

    @Test
    fun `hasMavenBuildSourceDirectoryOverride - returns false when source dir is null`(@TempDir tempDir: Path) {
        val baseDir = tempDir.toFile()
        val project = projectWithSourceDirectory(null, baseDir)
        assertFalse(KotlinLifecycleParticipant.hasMavenBuildSourceDirectoryOverride(project))
    }

    @Test
    fun `hasMavenBuildSourceDirectoryOverride - handles relative paths`(@TempDir tempDir: Path) {
        val baseDir = tempDir.toFile()
        val project = projectWithSourceDirectory("src/customMain/kotlin", baseDir)
        assertTrue(KotlinLifecycleParticipant.hasMavenBuildSourceDirectoryOverride(project))
    }

    @Test
    fun `hasMavenBuildSourceDirectoryOverride - relative default path returns false`(@TempDir tempDir: Path) {
        val baseDir = tempDir.toFile()
        val project = projectWithSourceDirectory("src/main/java", baseDir)
        assertFalse(KotlinLifecycleParticipant.hasMavenBuildSourceDirectoryOverride(project))
    }

    // Check <testSourceDirectory>
    @Test
    fun `hasMavenBuildTestSourceDirectoryOverride - returns false when test source dir is default`(@TempDir tempDir: Path) {
        val baseDir = tempDir.toFile()
        val sourceDir = tempDir.resolve("src").resolve("test").resolve("java").toFile()
        val project = projectWithTestSourceDirectory(sourceDir.absolutePath, baseDir)
        assertFalse(KotlinLifecycleParticipant.hasMavenBuildTestSourceDirectoryOverride(project))
    }

    @Test
    fun `hasMavenBuildTestSourceDirectoryOverride - returns true when test source dir is custom`(@TempDir tempDir: Path) {
        val baseDir = tempDir.toFile()
        val sourceDir = tempDir.resolve("src").resolve("customTest").resolve("kotlin").toFile()
        val project = projectWithTestSourceDirectory(sourceDir.absolutePath, baseDir)
        assertTrue(KotlinLifecycleParticipant.hasMavenBuildTestSourceDirectoryOverride(project))
    }

    @Test
    fun `hasMavenBuildTestSourceDirectoryOverride - returns false when test source dir is null`(@TempDir tempDir: Path) {
        val baseDir = tempDir.toFile()
        val project = projectWithTestSourceDirectory(null, baseDir)
        assertFalse(KotlinLifecycleParticipant.hasMavenBuildTestSourceDirectoryOverride(project))
    }

    @Test
    fun `hasMavenBuildTestSourceDirectoryOverride - handles relative paths`(@TempDir tempDir: Path) {
        val baseDir = tempDir.toFile()
        val project = projectWithTestSourceDirectory("src/customTest/kotlin", baseDir)
        assertTrue(KotlinLifecycleParticipant.hasMavenBuildTestSourceDirectoryOverride(project))
    }

    @Test
    fun `hasMavenBuildTestSourceDirectoryOverride - relative default path returns false`(@TempDir tempDir: Path) {
        val baseDir = tempDir.toFile()
        val project = projectWithTestSourceDirectory("src/test/java", baseDir)
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

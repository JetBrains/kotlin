/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.jvm

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.gradle.utils.javaSourceSets
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KotlinJvmWithJavaAndJavaSourceSetTest {

    private val baseJvmProject = buildProjectWithMPP {
        kotlin {
            jvm {
                @Suppress("DEPRECATION")
                withJava()
            }
        }
    }

    @Test
    fun shouldCreateMainJavaSourceSets() {
        baseJvmProject.evaluate()

        val mainSourceSet = baseJvmProject.javaSourceSets.findByName(
            baseJvmProject.multiplatformExtension.jvmMainCompilation.defaultSourceSet.name
        )
        assertNotNull(mainSourceSet)
        val srcDirs = mainSourceSet.java.srcDirs
        assertTrue(
            srcDirs.size == 1,
            "Main Java source set contains more than one SourceDirectorySet: $srcDirs"
        )
        assertTrue(
            srcDirs.single().endsWith("src/jvmMain/java"),
            "Main java source set does not use 'src/jvmMain/java' as SourceDirectorySet"
        )

        val legacyMainSourceSet = baseJvmProject.javaSourceSets.findByName("main")
        assertNotNull(legacyMainSourceSet)
        val legacySrcDirs = legacyMainSourceSet.java.srcDirs
        assertTrue(
            legacySrcDirs.size == 1,
            "Legacy Main Java source set contains more than one SourceDirectorySet: $legacySrcDirs"
        )
        assertTrue(
            legacySrcDirs.single().endsWith("src/jvmMain/java"),
            "Legacy main java source set does not use 'src/jvmMain/java' as SourceDirectorySet"
        )
    }

    @Test
    fun javaSourceSetsOutputShouldBeIncludedInFinalArtifact() {
        baseJvmProject.evaluate()

        val jvmMainCompilation = baseJvmProject.multiplatformExtension.jvmMainCompilation
        assertTrue(
            jvmMainCompilation.output.classesDirs.files.containsAll(
                baseJvmProject.jvmSourceSet(jvmMainCompilation).output.classesDirs.files
            )
        )
        assertTrue(
            jvmMainCompilation.output.classesDirs.files.containsAll(
                baseJvmProject.javaSourceSets.getByName("main").output.classesDirs.files
            )
        )
    }

    @Test
    fun mainJavaCompileTaskIsIncludedAsClassesTaskDependency() {
        baseJvmProject.evaluate()

        val jvmMainCompilation = baseJvmProject.multiplatformExtension.jvmMainCompilation
        val jvmMainClassesTask = baseJvmProject.tasks.getByName(jvmMainCompilation.compileAllTaskName)
        val javaProcessResourcesTask = baseJvmProject.tasks.getByName(jvmMainCompilation.defaultJavaSourceSet.processResourcesTaskName)

        jvmMainClassesTask.assertNoCircularTaskDependencies()
        jvmMainClassesTask.assertTaskDependenciesEquals(
            setOf(
                jvmMainCompilation.compileTaskProvider.get(),
                jvmMainCompilation.compileJavaTaskProvider!!.get(),
                jvmMainCompilation.defaultCompileJavaProvider.get(),
                baseJvmProject.tasks.getByName(jvmMainCompilation.processResourcesTaskName),
                javaProcessResourcesTask,
            )
        )
        assertFalse(
            jvmMainCompilation.defaultCompileJavaProvider.get().enabled,
            "Default Java compilation task should be disabled to avoid conflict with task created via 'withJava()"
        )
        assertFalse(javaProcessResourcesTask.enabled, "Java process resources task '${javaProcessResourcesTask.name} should be disabled")
    }

    private val KotlinMultiplatformExtension.jvmMainCompilation
        get() = jvm().compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME) as KotlinJvmCompilation

    private fun Project.jvmSourceSet(compilation: KotlinJvmCompilation) =
        javaSourceSets.getByName(compilation.defaultSourceSet.name)
}
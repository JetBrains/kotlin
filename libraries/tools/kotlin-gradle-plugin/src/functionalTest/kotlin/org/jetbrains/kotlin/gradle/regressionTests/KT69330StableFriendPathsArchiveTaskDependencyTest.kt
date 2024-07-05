/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.regressionTests

import org.gradle.api.internal.project.ProjectInternal
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.mavenCentralCacheRedirector
import org.jetbrains.kotlin.gradle.dsl.kotlinJvmExtension
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_ARCHIVES_TASK_OUTPUT_AS_FRIEND_ENABLED
import org.jetbrains.kotlin.gradle.plugin.mpp.decoratedInstance
import org.jetbrains.kotlin.gradle.util.*
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KT69330StableFriendPathsArchiveTaskDependencyTest {
    @Test
    fun `test KT-69330 - Task dependency is created so friendPaths are stable at compile time`() {
        val project = setupProject()

        assertTrue(hasTestJarDependencyInTest(project), "Expected that test jar is part of the generated friendPath")

        project.tasks.getByName("compileTestKotlin").assertDependsOn(project.tasks.getByName("jar"))
    }


    @Test
    fun `test KT-69330 - archivesTaskOutputAsFriendModule=false disables the generation of friendPaths with associated archive`() {
        val project = setupProject()

        project.propertiesExtension[KOTLIN_ARCHIVES_TASK_OUTPUT_AS_FRIEND_ENABLED] = "false"

        assertFalse(hasTestJarDependencyInTest(project), "Expected that test jar is not a part of the generated friendPath")

        project.tasks.getByName("compileTestKotlin").assertNotDependsOn(project.tasks.getByName("jar"))
    }

    private fun setupProject(): ProjectInternal {
        val project = buildProject()
        project.plugins.apply("java-library")
        project.applyKotlinJvmPlugin()
        project.repositories.mavenLocal()
        project.repositories.mavenCentralCacheRedirector()
        return project
    }

    private fun hasTestJarDependencyInTest(project: ProjectInternal): Boolean {
        val testCompilationImpl = project.kotlinJvmExtension.target.compilations.getByName("test").decoratedInstance.compilation
        return testCompilationImpl.friendPaths.any { it.asPath.endsWith("build/libs/test.jar") }
    }
}
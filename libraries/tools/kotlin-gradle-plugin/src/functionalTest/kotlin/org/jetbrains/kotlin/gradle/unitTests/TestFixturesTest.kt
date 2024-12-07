/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.testing.Test as TestTask
import org.gradle.kotlin.dsl.repositories
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.mavenCentralCacheRedirector
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import kotlin.test.Test

class TestFixturesTest {
    @Test
    fun testNoDuplicatedClasspathInJvm() = testNoDuplicatedResourcesInClasspath(
        buildProjectWithJvm(
            projectBuilder = {
                withName("KT-68278")
            }
        )
    )

    @Test
    fun testNoDuplicatedClasspathInKmp() = testNoDuplicatedResourcesInClasspath(
        buildProjectWithMPP(
            projectBuilder = {
                withName("KT-68278")
            }) {
            kotlin { jvm() { withJava() } }
        },
        "jvm"
    )

    private fun testNoDuplicatedResourcesInClasspath(project: ProjectInternal, targetPrefix: String? = null) = with(project) {
        plugins.apply("java-test-fixtures")
        repositories {
            mavenLocal()
            mavenCentralCacheRedirector()
        }
        evaluate()
        val testClasspath = (project.tasks.withType<TestTask>().findByName(lowerCamelCaseName(targetPrefix, "test"))
            ?: error("No test task")).classpath.files
        val jar = project.tasks.withType<Jar>().findByName(lowerCamelCaseName(targetPrefix, "jar")) ?: error("No jar task")
        val resourcesDirectory = project.tasks.withType<ProcessResources>().findByName(lowerCamelCaseName(targetPrefix, "processResources"))
            ?: error("No process resources task")
        val mainResourcesEntries = testClasspath.filter {
            it == jar.archiveFile.get().asFile || it == resourcesDirectory.destinationDir
        }
        assert(mainResourcesEntries.size == 1) {
            "Expected to see the main resources entry once, but got:\n${testClasspath.joinToString(separator = "\n")}"
        }
    }
}
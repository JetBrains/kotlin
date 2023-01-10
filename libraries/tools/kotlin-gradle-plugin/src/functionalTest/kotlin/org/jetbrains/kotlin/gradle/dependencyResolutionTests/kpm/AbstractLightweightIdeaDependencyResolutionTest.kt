/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.dependencyResolutionTests.kpm

import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.commonizer.KonanDistribution
import org.jetbrains.kotlin.commonizer.platformLibsDir
import org.jetbrains.kotlin.compilerRunner.konanHome
import org.jetbrains.kotlin.gradle.idea.testFixtures.kpm.TestIdeaKpmBinaryDependencyMatcher
import org.jetbrains.kotlin.gradle.plugin.mpp.enabledOnCurrentHost
import org.jetbrains.kotlin.gradle.util.enableDefaultStdlibDependency
import org.jetbrains.kotlin.gradle.util.enableDependencyVerification
import org.jetbrains.kotlin.konan.target.KonanTarget

/**
 * Base API applicable for test cases that intend to test idea dependency resolution for kpm  model builders.
 * This tests will be capable of resolving all kind of dependencies that do not rely on any tasks to run beforehand.
 * This test will support dependencies previously published from kotlin.git (like stdlib), or dependencies
 * reachable through mavenCentral (cache redirector)
 *
 * The intended usage is to use the [buildProject] function, setting up a suitable Gradle project.
 */
abstract class AbstractLightweightIdeaDependencyResolutionTest {

    fun buildProject(builder: ProjectBuilder.() -> Unit = {}): ProjectInternal {
        return (ProjectBuilder.builder().also(builder).build()).also { project ->
            project.enableDependencyVerification(false)
            project.enableDefaultStdlibDependency(false)
            project.repositories.mavenLocal()
            project.repositories.mavenCentralCacheRedirector()
        } as ProjectInternal
    }

    val Project.konanDistribution get() = KonanDistribution(project.konanHome)

    fun Project.nativePlatformLibraries(target: KonanTarget): TestIdeaKpmBinaryDependencyMatcher? =
        TestIdeaKpmBinaryDependencyMatcher.InDirectory(project.konanDistribution.platformLibsDir.resolve(target.name))
            .takeIf { target.enabledOnCurrentHost }
}

fun RepositoryHandler.mavenCentralCacheRedirector(): MavenArtifactRepository =
    maven { it.setUrl("https://cache-redirector.jetbrains.com/maven-central") }

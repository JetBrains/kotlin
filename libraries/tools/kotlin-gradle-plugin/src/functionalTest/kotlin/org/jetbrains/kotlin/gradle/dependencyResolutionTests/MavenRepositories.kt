/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dependencyResolutionTests

import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.kotlin.dsl.repositories
import org.jetbrains.kotlin.gradle.util.enableDependencyVerification

fun RepositoryHandler.mavenCentralCacheRedirector(): MavenArtifactRepository =
    maven { it.setUrl("https://cache-redirector.jetbrains.com/maven-central") }

/**
 * Resolves Kotlin build artifacts from `build/repo` via a standard `maven { url }` repository.
 *
 * The path is forwarded by the `functionalTest` task as `-DkotlinBuildRepo`. Both dev machines
 * and CI use this same repository, so resolution behaviour is identical in both environments.
 */
fun RepositoryHandler.kotlinBuildDeps(): MavenArtifactRepository {
    val buildRepo = System.getProperty("kotlinBuildRepo")
        ?: error("kotlinBuildRepo is not set; functionalTest task must provide it")
    return maven { it.url = java.io.File(buildRepo).toURI() }
}

fun Project.configureRepositoriesForTests() {
    allprojects {
        // disable because tests can resolve mock or 3rd party dependencies,
        // they are not part of Kotlin's verification-metadata.xml
        enableDependencyVerification(false)
        repositories {
            kotlinBuildDeps()
            mavenCentralCacheRedirector()
        }
    }
}

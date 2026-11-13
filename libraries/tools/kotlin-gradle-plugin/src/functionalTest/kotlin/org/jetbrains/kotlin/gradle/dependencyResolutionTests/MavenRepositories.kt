/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dependencyResolutionTests

import dev.adamko.gradle.dev_publish.devMavenRepo
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.jetbrains.kotlin.gradle.util.enableDependencyVerification

fun RepositoryHandler.mavenCentralCacheRedirector(): MavenArtifactRepository =
    maven { it.setUrl("https://cache-redirector.jetbrains.com/maven-central") }

/**
 * Resolves Kotlin build artifacts from the dev-publish repository via a standard `maven { url }` repository.
 */
fun RepositoryHandler.kotlinBuildDeps(): MavenArtifactRepository =
    maven { it.url = devMavenRepo().toUri() }

fun Project.configureRepositoriesForTests() {
    allprojects {
        // disable because tests can resolve mock or 3rd party dependencies,
        // they are not part of Kotlin's verification-metadata.xml
        it.enableDependencyVerification(false)
        with(it.repositories) {
            kotlinBuildDeps()
            mavenCentralCacheRedirector()
        }
    }
}

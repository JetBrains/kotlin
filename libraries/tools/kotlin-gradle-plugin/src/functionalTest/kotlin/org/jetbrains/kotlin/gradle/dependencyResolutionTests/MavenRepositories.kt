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
 * Resolves Kotlin build artifacts from the appropriate repository for the current environment.
 *
 * When `-DkotlinBuildRepo` is set (dev machines), resolves from `build/repo` via a standard
 * `maven { url }` repository. When `maven.repo.local` is set instead (CI), falls back to
 * `mavenLocal()` pointing at that external path.
 */
fun RepositoryHandler.kotlinBuildDeps(): MavenArtifactRepository {
    val buildRepo = System.getProperty("kotlinBuildRepo")
    return if (buildRepo != null) {
        // Dev: build/repo — standard remote Maven layout, resolved via the remote Maven resolver.
        maven { it.setUrl(java.io.File(buildRepo).toURI()) }
    } else {
        // CI: maven.repo.local is set externally; resolve via Maven Local.
        mavenLocal()
    }
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

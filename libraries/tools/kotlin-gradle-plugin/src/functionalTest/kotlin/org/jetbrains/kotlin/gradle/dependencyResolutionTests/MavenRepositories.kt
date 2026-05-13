/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dependencyResolutionTests

import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.kotlin.dsl.repositories
import org.jetbrains.kotlin.gradle.util.enableDependencyVerification
import java.io.File

fun RepositoryHandler.mavenCentralCacheRedirector(): MavenArtifactRepository =
    maven { it.setUrl("https://cache-redirector.jetbrains.com/maven-central") }

/**
 * Build-local Maven repo with current Kotlin build artifacts (kotlin-stdlib, kotlin-test, etc.)
 * copied from publishToMavenLocal output by the populateFunctionalTestRepo task.
 */
fun RepositoryHandler.kotlinBuildDeps(): ArtifactRepository? {
    val depsDir = System.getProperty("functionalTestDepsDir") ?: return null
    return maven { repo ->
        repo.name = "kotlinBuildDeps"
        repo.url = File(depsDir).toURI()
    }
}

/**
 * Maven repo mirror with third-party dependency metadata (checked into git as .tar.gz,
 * extracted at build time into build/mavenRepoMirror/).
 */
fun RepositoryHandler.mavenRepoMirror(): ArtifactRepository? {
    val mirrorDir = System.getProperty("mavenRepoMirrorDir") ?: return null
    return maven { repo ->
        repo.name = "mavenRepoMirror"
        repo.url = File(mirrorDir).toURI()
        // Exclude org.jetbrains.kotlin — current build artifacts come from kotlinBuildDeps().
        // The mirror has old kotlin versions that would conflict with version alignment.
        repo.mavenContent { content ->
            content.excludeGroupByRegex("org\\.jetbrains\\.kotlin.*")
        }
    }
}

fun Project.configureRepositoriesForTests() {
    allprojects {
        // disable because tests can resolve mock or 3rd party dependencies,
        // they are not part of Kotlin's verification-metadata.xml
        enableDependencyVerification(false)
        repositories {
            kotlinBuildDeps()
            mavenRepoMirror()
            mavenCentralCacheRedirector()
        }
    }
}

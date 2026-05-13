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
 * Local Maven repo mirror containing third-party dependencies with real metadata
 * (POM, Gradle Module Metadata) but stub binary files (empty JARs/klibs).
 * Checked into git under `src/functionalTest/resources/mavenRepoMirror/`.
 *
 * This replaces `mavenCentralCacheRedirector()` for third-party deps so that
 * tests don't need network access and don't depend on `~/.m2`.
 */
fun RepositoryHandler.kotlinBuildDeps(): ArtifactRepository? {
    // Try system property first (set by the test task), then fall back to classpath resource
    val depsDir = System.getProperty("functionalTestDepsDir")
    if (depsDir != null) {
        return maven { repo ->
            repo.name = "kotlinBuildDeps"
            repo.url = File(depsDir).toURI()
        }
    }
    return null
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
        // Exclude org.jetbrains.kotlin — current SNAPSHOT artifacts come from filteredMavenLocal().
        // The mirror has old kotlin versions that would conflict with version alignment.
        repo.mavenContent { content ->
            content.excludeGroupByRegex("org\\.jetbrains\\.kotlin.*")
        }
    }
}

@Deprecated("Use kotlinBuildDeps() + mavenRepoMirror()", ReplaceWith("kotlinBuildDeps()"))
fun RepositoryHandler.filteredMavenLocal(): MavenArtifactRepository = mavenLocal { repo ->
    repo.mavenContent { it.includeGroupByRegex(".*jetbrains.*") }
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
            @Suppress("DEPRECATION")
            filteredMavenLocal()
        }
    }
}

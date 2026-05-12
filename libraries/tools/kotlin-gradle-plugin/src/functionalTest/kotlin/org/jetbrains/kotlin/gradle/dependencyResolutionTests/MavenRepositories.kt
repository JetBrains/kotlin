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
 * Flat directory repository containing JetBrains Kotlin build artifacts.
 * The path is passed from the test task via the `functionalTestDepsDir` system property,
 * pointing to `build/functionalTestDependencies/` which is populated by the
 * `syncFunctionalTestDependencies` task from explicit project dependencies.
 *
 * This replaces `mavenLocal()` so tests don't depend on shared mutable `~/.m2` state.
 * Third-party transitive dependencies come from [mavenCentralCacheRedirector].
 */
fun RepositoryHandler.kotlinBuildDeps(): ArtifactRepository? {
    val depsDir = System.getProperty("functionalTestDepsDir") ?: return null
    return maven { repo ->
        repo.name = "kotlinBuildDeps"
        repo.url = File(depsDir).toURI()
        // Content filter prevents this repo from shadowing mavenCentral for unrelated artifacts.
        // Include all groups that have stubs in the build-local repo.
        repo.mavenContent { c -> c.includeGroupByRegex("org\\.jetbrains\\.kotlin.*|org\\.jetbrains\\.kotlinx.*|org\\.jetbrains|org\\.test|com\\.arkivanov\\..*|com\\.squareup\\..*|io\\.ktor.*|junit|org\\.hamcrest") }
    }
}

@Deprecated("Use kotlinBuildDeps() + mavenCentralCacheRedirector()", ReplaceWith("kotlinBuildDeps()"))
fun RepositoryHandler.filteredMavenLocal(): MavenArtifactRepository = mavenLocal { repo ->
    repo.mavenContent { it.includeGroupByRegex(".*jetbrains.*") }
}

fun Project.configureRepositoriesForTests() {
    allprojects {
        // disable because tests can resolve mock or 3rd party dependencies,
        // they are not part of Kotlin's verification-metadata.xml
        enableDependencyVerification(false)
        repositories {
            mavenCentralCacheRedirector()
            kotlinBuildDeps()
        }
    }
}
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
import java.io.File

fun RepositoryHandler.mavenCentralCacheRedirector(): MavenArtifactRepository =
    maven { it.setUrl("https://cache-redirector.jetbrains.com/maven-central") }

/**
 * Build-local Maven repository containing published KGP artifacts.
 * Takes priority over [filteredMavenLocal] so that fresh SNAPSHOT artifacts from the
 * current build are used instead of stale ones potentially cached in `~/.m2`.
 *
 * The path is passed via the `functionalTestRepoPath` system property
 * and points to `<rootProject>/build/functional-test-repo/`.
 */
fun RepositoryHandler.buildLocalRepo(): MavenArtifactRepository? {
    val repoPath = System.getProperty("functionalTestRepoPath") ?: return null
    return maven { repo ->
        repo.name = "buildLocalFunctionalTest"
        repo.url = File(repoPath).toURI()
        repo.mavenContent { it.includeGroupByRegex("org\\.jetbrains\\.kotlin.*") }
    }
}

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
            buildLocalRepo()
            filteredMavenLocal()
        }
    }
}
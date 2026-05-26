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
 * Build-local Kotlin artifacts via Maven local repository format.
 *
 * The kotlin-gradle-plugin build script redirects maven.repo.local to build/functionalTestDependencies
 * (unless already set externally, e.g. on CI), so this resolves to build-local Kotlin artifacts
 * rather than ~/.m2 on developer machines.
 *
 * Uses unfiltered mavenLocal(): filtering via mavenContent switches Gradle to the remote Maven
 * resolver which requires maven-metadata.xml and cannot resolve non-unique SNAPSHOTs produced
 * by publishToMavenLocal. Since build/functionalTestDependencies only contains Kotlin artifacts,
 * no content filter is needed — other artifacts naturally fall through to subsequent repos.
 */
fun RepositoryHandler.kotlinBuildDeps(): MavenArtifactRepository = mavenLocal()

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

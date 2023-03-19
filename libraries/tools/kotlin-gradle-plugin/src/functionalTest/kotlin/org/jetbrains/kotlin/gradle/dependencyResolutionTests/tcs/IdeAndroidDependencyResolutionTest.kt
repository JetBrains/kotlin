/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.dependencyResolutionTests.tcs

import org.jetbrains.kotlin.gradle.dependencyResolutionTests.mavenCentralCacheRedirector
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinBinaryDependency
import org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers.IdeArtifactResolutionQuerySourcesAndDocumentationResolver
import org.jetbrains.kotlin.gradle.plugin.ide.kotlinIdeMultiplatformImport
import org.jetbrains.kotlin.gradle.plugin.ide.kotlinIdeMultiplatformImportStatistics
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.gradle.utils.androidExtension
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.fail

/**
 * Android is not supposed to resolve any dependencies in this import.
 * 'Android' refers to the 'classic' Android plugins (like com.android.application, ...library, ...)
 */
class IdeAndroidDependencyResolutionTest {

    private val project = buildProject {
        enableDefaultStdlibDependency(true)
        enableDependencyVerification(false)
        setMultiplatformAndroidSourceSetLayoutVersion(2)
        applyMultiplatformPlugin()
        plugins.apply("com.android.library")
        androidExtension.compileSdkVersion(33)
        repositories.mavenLocal()
        repositories.mavenCentralCacheRedirector()

        multiplatformExtension.apply {
            android()
            sourceSets.getByName("commonMain").dependencies {
                implementation("com.arkivanov.mvikotlin:mvikotlin:3.0.2")
            }
        }
    }.evaluate()

    private val androidSourceSets = project.multiplatformExtension.android().compilations.flatMap { it.kotlinSourceSets }
        .ifEmpty { fail("Expected at least one Android SourceSet") }

    @BeforeTest
    fun checkEnvironment() {
        assumeAndroidSdkAvailable()
    }

    @Test
    fun `test - android source sets do not resolve binary dependencies`() {
        androidSourceSets.forEach { sourceSet ->
            val binaryDependencies = project.kotlinIdeMultiplatformImport.resolveDependencies(sourceSet)
                .filterIsInstance<IdeaKotlinBinaryDependency>()

            if (binaryDependencies.isNotEmpty()) {
                fail("Expected no binary dependencies being resolved for Android. Found $binaryDependencies")
            }
        }
    }

    @Test
    fun `test - Android SourceSets - do not execute Sources And Documentation Resolver`() {
        androidSourceSets.forEach { sourceSet ->
            project.kotlinIdeMultiplatformImport.resolveDependencies(sourceSet)
            if (
                IdeArtifactResolutionQuerySourcesAndDocumentationResolver::class.java in
                project.kotlinIdeMultiplatformImportStatistics.getExecutionTimes()
            ) {
                fail("${IdeArtifactResolutionQuerySourcesAndDocumentationResolver::class.simpleName} as executed on ${sourceSet.name}")
            }
        }
    }

}

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import com.android.build.gradle.LibraryExtension
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.get
import org.jetbrains.kotlin.gradle.plugin.AndroidGradlePluginVersion
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.util.androidLibrary
import org.jetbrains.kotlin.gradle.util.assertContainsDiagnostic
import org.jetbrains.kotlin.gradle.util.assertNoDiagnostics
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.test.MuteableTestRule
import org.junit.Rule
import org.junit.Test

@Suppress("FunctionName")
class AndroidPublicationNotConfiguredTest {
    @get:Rule val muteableTestRule = MuteableTestRule()

    private fun testProject(block: ProjectInternal.() -> Unit): ProjectInternal {
        val project = buildProject()
        project.androidLibrary {}
        project.block()

        return project
    }

    private fun ProjectInternal.applyKotlinAndroid() {
        plugins.apply("kotlin-android")
    }

    private fun ProjectInternal.applyMavenPublish() {
        plugins.apply("maven-publish")
    }

    private fun ProjectInternal.configureAndroidPublication(componentName: String) {
        val androidExtension = extensions.getByName("android") as LibraryExtension
        androidExtension.publishing.singleVariant(componentName) { }
    }

    private fun ProjectInternal.assertDiagnosticReported() {
        evaluate()
        assertContainsDiagnostic(KotlinToolingDiagnostics.AndroidPublicationNotConfigured)
    }

    private fun ProjectInternal.assertNoDiagnosticReported() {
        evaluate()
        assertNoDiagnostics(KotlinToolingDiagnostics.AndroidPublicationNotConfigured)
    }

    private fun ProjectInternal.createPublicationFrom(componentName: String, publicationName: String = componentName) {
        val publishing = extensions.getByType(PublishingExtension::class.java)
        publishing.publications.create(publicationName, MavenPublication::class.java) { publication ->
            afterEvaluate {
                publication.groupId = "com.example"
                publication.artifactId = "lib"
                publication.version = "1.0"

                publication.from(components[componentName])
            }
        }
    }

    @Test
    fun `android library with release publication no configuration on agp side`() {
        val project = testProject {
            applyKotlinAndroid()
            applyMavenPublish()
            createPublicationFrom("release")
        }
        project.assertDiagnosticReported()
    }

    @Test
    fun `android library with debug publication no configuration on agp side`() {
        val project = testProject {
            applyKotlinAndroid()
            applyMavenPublish()
            createPublicationFrom("debug")
        }
        project.assertDiagnosticReported()
    }

    @Test
    fun `android library with debug and release publication no configuration on agp side`() {
        val project = testProject {
            applyKotlinAndroid()
            applyMavenPublish()
            createPublicationFrom("debug")
            createPublicationFrom("release")
        }
        project.assertDiagnosticReported()
    }

    @Test
    fun `android library without publications`() {
        val project = testProject {
            applyKotlinAndroid()
            applyMavenPublish()
        }
        project.assertNoDiagnosticReported()
    }

    @Test
    fun `android library with only release publication configured on agp side`() {
        val project = testProject {
            applyKotlinAndroid()
            applyMavenPublish()
            configureAndroidPublication("release")
            createPublicationFrom("debug")
            createPublicationFrom("release")
        }
        project.assertDiagnosticReported()
    }

    @Test
    fun `android library with release publication configured on agp side`() {
        val project = testProject {
            applyKotlinAndroid()
            applyMavenPublish()
            configureAndroidPublication("release")
            createPublicationFrom("release")
        }
        project.assertNoDiagnosticReported()
    }

    @Test
    fun `android library with debug publication configured on agp side`() {
        val project = testProject {
            applyKotlinAndroid()
            applyMavenPublish()
            configureAndroidPublication("debug")
            createPublicationFrom("debug")
        }
        project.assertNoDiagnosticReported()
    }
}
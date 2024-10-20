/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import com.android.build.gradle.LibraryExtension
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.internal.publication.MavenPublicationInternal
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics.AndroidReleasePublicationIsNotConsumable
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinTargetSoftwareComponent
import org.jetbrains.kotlin.gradle.util.androidLibrary
import org.jetbrains.kotlin.gradle.util.applyMultiplatformPlugin
import org.jetbrains.kotlin.gradle.util.assertContainsDiagnostic
import org.jetbrains.kotlin.gradle.util.assertNoDiagnostics
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.checkDiagnostics
import org.jetbrains.kotlin.gradle.util.publishing
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.fail

@Suppress("FunctionName")
class AndroidReleasePublicationIsNotConsumableTest {

    private fun testProject(block: ProjectInternal.() -> Unit): ProjectInternal {
        val project = buildProject()
        project.applyMultiplatformPlugin()
        project.androidLibrary {}
        project.plugins.apply("maven-publish")

        project.multiplatformExtension.androidTarget()
        project.multiplatformExtension.jvm()

        project.block()

        return project
    }

    private fun ProjectInternal.configureAndroidFlavors() {
        val androidExtension = extensions.getByName("android") as LibraryExtension
        androidExtension.apply {
            flavorDimensions += "flavor"
            productFlavors {
                create("foo") {
                    it.dimension = "flavor"
                }
                create("bar") {
                    it.dimension = "flavor"
                }
            }
        }
    }

    private fun ProjectInternal.addCustomAndroidBuildType() {
        val androidExtension = extensions.getByName("android") as LibraryExtension
        androidExtension.apply {
            buildTypes.create("custom")
        }
    }

    @Test
    fun `single release publication - diagnostic not reported`() {
        val project = testProject {
            multiplatformExtension.androidTarget {
                publishLibraryVariants("release")
            }
        }
        project.evaluate()
        project.assertNoDiagnostics(AndroidReleasePublicationIsNotConsumable)
        project.assertPublicationBuildTypeAttributeEquals("androidRelease", null)
    }

    @Test
    fun `single debug publication - diagnostic not reported`() {
        val project = testProject {
            multiplatformExtension.androidTarget {
                publishLibraryVariants("debug")
            }
        }
        project.evaluate()
        project.assertNoDiagnostics(AndroidReleasePublicationIsNotConsumable)
        project.assertPublicationBuildTypeAttributeEquals("androidDebug", "debug")
    }

    @Test
    fun `release and debug publications - diagnostic reported`() {
        val project = testProject {
            multiplatformExtension.androidTarget {
                publishLibraryVariants("release", "debug")
            }

            group = "org.jetbrains.kotlin.sample"
            version = "1.0"

            publishing.publications.withType(MavenPublication::class.java).configureEach { publication ->
                afterEvaluate { publication.artifactId = "lib-${publication.name}" }
            }
        }
        project.evaluate()
        project.assertContainsDiagnostic(AndroidReleasePublicationIsNotConsumable)
        project.assertPublicationBuildTypeAttributeEquals("androidRelease", "release")
        project.assertPublicationBuildTypeAttributeEquals("androidDebug", "debug")
        project.checkDiagnostics("AndroidReleasePublicationIsNotConsumableTest/releaseAndDebugPublicationsConfigured")
    }

    @Test
    fun `release and debug publications keepBuildTypeAttr is false - diagnostic not reported`() {
        val project = testProject {
            extraProperties.set("kotlin.android.buildTypeAttribute.keep", false)
            multiplatformExtension.androidTarget {
                publishLibraryVariants("release", "debug")
            }
        }
        project.evaluate()
        project.assertNoDiagnostics(AndroidReleasePublicationIsNotConsumable)
        project.assertPublicationBuildTypeAttributeEquals("androidRelease", null)
        project.assertPublicationBuildTypeAttributeEquals("androidDebug", null)
    }

    @Test
    fun `all variants publication - diagnostic reported`() {
        val project = testProject {
            multiplatformExtension.androidTarget {
                publishAllLibraryVariants()
            }
        }
        project.evaluate()
        project.assertContainsDiagnostic(AndroidReleasePublicationIsNotConsumable)
        project.assertPublicationBuildTypeAttributeEquals("androidRelease", "release")
        project.assertPublicationBuildTypeAttributeEquals("androidDebug", "debug")
    }

    @Test
    fun `empty android publications - diagnostic not reported`() {
        val project = testProject {
            multiplatformExtension.androidTarget {
                publishLibraryVariants = emptyList()
            }
        }
        project.evaluate()
        project.assertNoDiagnostics(AndroidReleasePublicationIsNotConsumable)
    }

    @Test
    fun `only flavored release publication - diagnostic not reported`() {
        val project = testProject {
            configureAndroidFlavors()
            multiplatformExtension.androidTarget {
                publishLibraryVariants("fooRelease", "barRelease")
            }
        }
        project.evaluate()
        project.assertNoDiagnostics(AndroidReleasePublicationIsNotConsumable)
        project.assertPublicationBuildTypeAttributeEquals("androidFooRelease", null)
        project.assertPublicationBuildTypeAttributeEquals("androidBarRelease", null)
    }

    @Test
    fun `only flavored debug publication - diagnostic not reported`() {
        val project = testProject {
            configureAndroidFlavors()
            multiplatformExtension.androidTarget {
                publishLibraryVariants("fooDebug", "barDebug")
            }
        }
        project.evaluate()
        project.assertNoDiagnostics(AndroidReleasePublicationIsNotConsumable)
        project.assertPublicationBuildTypeAttributeEquals("androidFooDebug", "debug")
        project.assertPublicationBuildTypeAttributeEquals("androidBarDebug", "debug")
    }


    @Test
    fun `single custom build type publication - diagnostic not reported`() {
        val project = testProject {
            addCustomAndroidBuildType()
            multiplatformExtension.androidTarget {
                publishLibraryVariants("custom")
            }
        }
        project.evaluate()
        project.assertNoDiagnostics(AndroidReleasePublicationIsNotConsumable)
        project.assertPublicationBuildTypeAttributeEquals("androidCustom", "custom")
    }

    @Test
    fun `custom and debug build type publication - diagnostic not reported`() {
        val project = testProject {
            addCustomAndroidBuildType()
            multiplatformExtension.androidTarget {
                publishLibraryVariants("custom", "debug")
            }
        }
        project.evaluate()
        project.assertNoDiagnostics(AndroidReleasePublicationIsNotConsumable)
        project.assertPublicationBuildTypeAttributeEquals("androidCustom", "custom")
        project.assertPublicationBuildTypeAttributeEquals("androidDebug", "debug")
    }

    @Test
    fun `custom and release build type publication - diagnostic reported`() {
        val project = testProject {
            addCustomAndroidBuildType()
            multiplatformExtension.androidTarget {
                publishLibraryVariants("custom", "release")
            }
        }
        project.evaluate()
        project.assertContainsDiagnostic(AndroidReleasePublicationIsNotConsumable)
        project.assertPublicationBuildTypeAttributeEquals("androidCustom", "custom")
        project.assertPublicationBuildTypeAttributeEquals("androidRelease", "release")
    }

    private fun ProjectInternal.assertPublicationBuildTypeAttributeEquals(publicationName: String, expected: String?) {
        val publication = publishing.publications.findByName(publicationName)
        if (publication == null) fail("Can't find publication '$publicationName'. Available publications: ${publishing.publications.names}")
        publication as MavenPublicationInternal
        val component = publication.component.get() as KotlinTargetSoftwareComponent
        val buildTypeAttribute = "com.android.build.api.attributes.BuildTypeAttr"
        component.usages.forEach { usage ->
            val attributesMap = usage.attributes.toStringMap
            assertEquals(expected, attributesMap[buildTypeAttribute])
        }
    }

    private val AttributeContainer.toStringMap: Map<String, String>
        get() = keySet().associate { attribute -> attribute.name to getAttribute(attribute).toString() }
}
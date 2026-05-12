/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName", "DuplicatedCode")

package org.jetbrains.kotlin.gradle.dependencyResolutionTests.tcs

import com.android.build.gradle.internal.publishing.AndroidArtifacts
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.configureRepositoriesForTests
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.mavenCentralCacheRedirector
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinResolvedBinaryDependency
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.assertMatches
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.binaryCoordinates
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.ide.IdeDependencyResolver.Companion.gradleArtifact
import org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers.IdeBinaryDependencyResolver
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.gradle.utils.androidExtension
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class IdeBinaryDependencyResolverTest {

    @Test
    fun `test - MVIKotlin - on jvm and linux platform source sets`() {
        val project = buildProject {
            enableDefaultStdlibDependency(false)
            configureRepositoriesForTests()
            applyMultiplatformPlugin()
        }

        val kotlin = project.multiplatformExtension
        kotlin.applyDefaultHierarchyTemplate()

        kotlin.jvm()
        kotlin.linuxX64()

        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val jvmMain = kotlin.sourceSets.getByName("jvmMain")
        val linuxX64Main = kotlin.sourceSets.getByName("linuxX64Main")
        val jvmTest = kotlin.sourceSets.getByName("jvmTest")
        val linuxX64Test = kotlin.sourceSets.getByName("linuxX64Test")

        commonMain.dependencies {
            implementation("org.test:mock-kmp-lib:1.0")
        }

        /* This resolver shall refuse to resolve for dependencies for metadata based dependencies */
        IdeBinaryDependencyResolver().resolve(commonMain).assertMatches()

        val jvmDependencies = listOf(
            binaryCoordinates("org.test:mock-kmp-lib-jvm:1.0"),
            binaryCoordinates("org.test:mock-transitive-a-jvm:1.0"),
            binaryCoordinates("org.test:mock-transitive-b-jvm:1.0"),
            binaryCoordinates("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.10"),
            binaryCoordinates("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.7.10"),
            binaryCoordinates("org.jetbrains.kotlin:kotlin-stdlib:1.7.10"),
            binaryCoordinates("org.jetbrains:annotations:13.0")
        )

        val linuxDependencies = listOf(
            binaryCoordinates("org.test:mock-kmp-lib-linuxx64:1.0"),
            binaryCoordinates("org.test:mock-transitive-a-linuxx64:1.0"),
            binaryCoordinates("org.test:mock-transitive-b-linuxx64:1.0"),
            binaryCoordinates("org.jetbrains.kotlin:kotlin-stdlib:1.7.10"),
            binaryCoordinates("org.jetbrains:annotations:13.0"),

            /* Special in K/N: There are no 'runtimeOnly' dependencies */
            binaryCoordinates("org.test:mock-kmp-lib-rx-linuxx64:1.0"),
            binaryCoordinates("org.test:mock-kmp-lib-utils-linuxx64:1.0"),
            binaryCoordinates("org.test:mock-kmp-lib-rx2-linuxx64:1.0"),
            binaryCoordinates("org.test:mock-transitive-c-linuxx64:1.0"),
        )

        IdeBinaryDependencyResolver().resolve(jvmMain).assertMatches(jvmDependencies)
        IdeBinaryDependencyResolver().resolve(jvmTest).assertMatches(jvmDependencies)
        IdeBinaryDependencyResolver().resolve(linuxX64Main).assertMatches(linuxDependencies)
        IdeBinaryDependencyResolver().resolve(linuxX64Test).assertMatches(linuxDependencies)
    }

    @Test
    fun `test - android artifact transformation`() {
        assertAndroidSdkAvailable()

        /* Setup simple project that can resolve MVIKotlin */
        val project = buildProject {
            enableDefaultStdlibDependency(false)
            enableDependencyVerification(false)
            applyMultiplatformPlugin()
            plugins.apply("com.android.library")
            androidExtension.configureDefaults()
            repositories.mavenCentralCacheRedirector()
        }

        /* Setup android target and add MVIKotlin dependency */
        val kotlin = project.multiplatformExtension
        kotlin.applyDefaultHierarchyTemplate()
        @Suppress("DEPRECATION")
        kotlin.androidTarget()
        val commonMain = kotlin.sourceSets.getByName("commonMain")
        commonMain.dependencies {
            implementation("org.test:mock-kmp-lib:1.0")
        }

        project.evaluate()

        /*
        Resolve dependencies on commonMain with platform attributes from Android,
        then find the mock-kmp-lib-android dependency (aar)
        */
        val resolvedDependency = IdeBinaryDependencyResolver(
            artifactResolutionStrategy = IdeBinaryDependencyResolver.ArtifactResolutionStrategy.PlatformLikeSourceSet(
                setupPlatformResolutionAttributes = {
                    attribute(KotlinPlatformType.attribute, KotlinPlatformType.androidJvm)
                },
                setupArtifactViewAttributes = {
                    attribute(AndroidArtifacts.ARTIFACT_TYPE, AndroidArtifacts.ArtifactType.CLASSES_JAR.type)
                }
            )
        ).resolve(commonMain).filterIsInstance<IdeaKotlinResolvedBinaryDependency>()
            .find { it.coordinates?.module == "mock-kmp-lib-android" }
            ?: fail("Missing mock-kmp-lib-android dependency")

        /* Check that the Android dependency got resolved as .jar */
        if (resolvedDependency.classpath.isEmpty()) fail("Expected at least one file in dependency classpath")
        resolvedDependency.classpath.forEach { file ->
            assertEquals("jar", file.extension, "Expected jar file. Found ${file.name}")
        }

        /* Check if we *really* matched the Android release variant */
        (resolvedDependency.gradleArtifact ?: fail("Missing gradleArtifact")).let { artifact ->
            if (!artifact.variant.displayName.endsWith("releaseRuntimeElements-published"))
                fail("Expected release variant matched. Found ${artifact.variant.displayName}")
        }
    }
}

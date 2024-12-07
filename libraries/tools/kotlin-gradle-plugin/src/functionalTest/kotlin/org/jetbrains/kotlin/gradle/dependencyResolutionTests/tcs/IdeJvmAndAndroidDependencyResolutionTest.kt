/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.dependencyResolutionTests.tcs

import org.jetbrains.kotlin.gradle.util.mockGenerateProjectStructureMetadataTaskOutputs
import org.gradle.api.Project
import org.gradle.api.plugins.JavaLibraryPlugin
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.mavenCentralCacheRedirector
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinBinaryDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinResolvedBinaryDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinSourceDependency
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.*
import org.jetbrains.kotlin.gradle.internal.dsl.KotlinMultiplatformSourceSetConventionsImpl.commonMain
import org.jetbrains.kotlin.gradle.internal.dsl.KotlinMultiplatformSourceSetConventionsImpl.commonTest
import org.jetbrains.kotlin.gradle.internal.dsl.KotlinMultiplatformSourceSetConventionsImpl.dependencies
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers.IdeJvmAndAndroidPlatformBinaryDependencyResolver
import org.jetbrains.kotlin.gradle.plugin.ide.kotlinIdeMultiplatformImport
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.gradle.utils.androidExtension
import kotlin.test.BeforeTest
import kotlin.test.Test

class IdeJvmAndAndroidDependencyResolutionTest {

    @BeforeTest
    fun checkEnvironment() {
        assertAndroidSdkAvailable()
    }

    private fun Project.configureAndroidAndMultiplatform(enableDefaultStdlib: Boolean = false) {
        enableDefaultStdlibDependency(enableDefaultStdlib)
        enableDependencyVerification(false)
        setMultiplatformAndroidSourceSetLayoutVersion(2)
        applyMultiplatformPlugin()
        plugins.apply("com.android.library")
        androidExtension.configureDefaults()
        if (enableDefaultStdlib) repositories.mavenLocal()
        repositories.mavenCentralCacheRedirector()

        project.multiplatformExtension.applyHierarchyTemplate {
            common {
                group("jvmAndAndroid") {
                    withJvm()
                    withAndroidTarget()
                }
            }
        }

        project.multiplatformExtension.jvm()
        project.multiplatformExtension.androidTarget()

    }

    private fun Project.assertBinaryDependencies(sourceSetName: String, notation: Any) {
        project.kotlinIdeMultiplatformImport
            .resolveDependencies(sourceSetName)
            .filterIsInstance<IdeaKotlinResolvedBinaryDependency>()
            .assertMatches(notation)
    }

    @Test
    fun `test - MVIKotlin - on jvmAndAndroidMain`() {
        val project = buildProject { configureAndroidAndMultiplatform(enableDefaultStdlib = false) }
        val kotlin = project.multiplatformExtension
        kotlin.sourceSets.getByName("commonMain").dependencies {
            implementation("com.arkivanov.mvikotlin:mvikotlin:3.0.2")
        }

        project.evaluate()

        val jvmAndAndroidDependencies = listOf(
            binaryCoordinates("com.arkivanov.mvikotlin:mvikotlin-jvm:3.0.2"),
            binaryCoordinates("com.arkivanov.essenty:lifecycle-jvm:0.4.2"),
            binaryCoordinates("com.arkivanov.essenty:instance-keeper-jvm:0.4.2"),
            binaryCoordinates("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.10"), // transitive
            binaryCoordinates("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.7.10"),
            binaryCoordinates("org.jetbrains.kotlin:kotlin-stdlib:1.7.10"),
            binaryCoordinates("org.jetbrains:annotations:13.0"),
        )

        IdeJvmAndAndroidPlatformBinaryDependencyResolver(project).resolve(kotlin.sourceSets.getByName("jvmAndAndroidMain"))
            .assertMatches(jvmAndAndroidDependencies)

        IdeJvmAndAndroidPlatformBinaryDependencyResolver(project).resolve(kotlin.sourceSets.getByName("jvmAndAndroidTest"))
            .assertMatches(jvmAndAndroidDependencies)
    }

    @Test
    fun `test - project to multiplatform project dependency`() {
        val root = buildProject { setMultiplatformAndroidSourceSetLayoutVersion(2) }
        val producer = buildProject({ withParent(root).withName("producer") }) { configureAndroidAndMultiplatform() }
        val consumer = buildProject({ withParent(root).withName("consumer") }) { configureAndroidAndMultiplatform() }

        root.evaluate()
        producer.evaluate()
        consumer.evaluate()

        producer.mockGenerateProjectStructureMetadataTaskOutputs()

        root.allprojects { project ->
            project.repositories.mavenLocal()
            project.repositories.mavenCentral()
        }

        consumer.multiplatformExtension.sourceSets.getByName("commonMain").dependencies {
            implementation(project(":producer"))
        }

        consumer.kotlinIdeMultiplatformImport.resolveDependencies("jvmAndAndroidMain").assertMatches(
            dependsOnDependency(":consumer/commonMain"),
            regularSourceDependency(":producer/commonMain"),
            regularSourceDependency(":producer/jvmAndAndroidMain"),
        )

        consumer.kotlinIdeMultiplatformImport.resolveDependencies("jvmAndAndroidTest").assertMatches(
            dependsOnDependency(":consumer/commonTest"),
            friendSourceDependency(":consumer/commonMain"),
            friendSourceDependency(":consumer/jvmAndAndroidMain"),
            regularSourceDependency(":producer/commonMain"),
            regularSourceDependency(":producer/jvmAndAndroidMain"),
        )
    }

    @Test
    fun `test - project to jvm project dependency`() {
        val root = buildProject()

        val producer = buildProject({ withParent(root).withName("producer") }) { applyKotlinJvmPlugin() }
        val consumer = buildProject({ withParent(root).withName("consumer") }) { configureAndroidAndMultiplatform() }

        consumer.multiplatformExtension.sourceSets.commonMain.dependencies {
            implementation(producer)
        }

        root.evaluate()
        producer.evaluate()
        consumer.evaluate()

        consumer.kotlinIdeMultiplatformImport.resolveDependencies("commonMain")
            .filter { it !is IdeaKotlinBinaryDependency }
            .assertMatches(projectArtifactDependency(IdeaKotlinSourceDependency.Type.Regular, ":producer", FilePathRegex(".*producer.jar")))

        consumer.kotlinIdeMultiplatformImport.resolveDependencies("jvmAndAndroidMain")
            .filter { it !is IdeaKotlinBinaryDependency }
            .assertMatches(
                dependsOnDependency(":consumer/commonMain"),
                projectArtifactDependency(IdeaKotlinSourceDependency.Type.Regular, ":producer", FilePathRegex(".*producer.jar"))
            )
    }

    @Test
    fun `test - project to jvm (java only) project dependency`() {
        val root = buildProject()

        val producer = buildProject({ withParent(root).withName("producer") }) { plugins.apply(JavaLibraryPlugin::class.java) }
        val consumer = buildProject({ withParent(root).withName("consumer") }) { configureAndroidAndMultiplatform() }

        consumer.multiplatformExtension.sourceSets.commonMain.dependencies {
            implementation(producer)
        }

        root.evaluate()
        producer.evaluate()
        consumer.evaluate()

        consumer.kotlinIdeMultiplatformImport.resolveDependencies("commonMain")
            .filter { it !is IdeaKotlinBinaryDependency }
            .assertMatches(projectArtifactDependency(IdeaKotlinSourceDependency.Type.Regular, ":producer", FilePathRegex(".*producer.jar")))
    }


    @Test
    fun `test - KT-59020 - transitive project dependency to self`() {
        val root = buildProject { setMultiplatformAndroidSourceSetLayoutVersion(2) }
        val a = buildProject({ withParent(root).withName("a") }) { configureAndroidAndMultiplatform() }
        val b = buildProject({ withParent(root).withName("b") }) { configureAndroidAndMultiplatform() }

        b.multiplatformExtension.sourceSets.commonMain.dependencies {
            api(project(":a"))
        }

        a.multiplatformExtension.sourceSets.commonTest.dependencies {
            api(project(":b"))
        }


        root.evaluate()
        a.evaluate()
        b.evaluate()

        b.mockGenerateProjectStructureMetadataTaskOutputs()
        a.mockGenerateProjectStructureMetadataTaskOutputs()

        a.kotlinIdeMultiplatformImport.resolveDependencies("jvmAndAndroidTest").assertMatches(
            friendSourceDependency(":a/commonMain"),
            friendSourceDependency(":a/jvmAndAndroidMain"),
            dependsOnDependency(":a/commonTest"),
            regularSourceDependency(":b/commonMain"),
            regularSourceDependency(":b/jvmAndAndroidMain"),
        )
    }

    @Test
    fun `test - KT-62029 - transitive dependencies form project dependency`() {
        val root = buildProject { enableDefaultStdlibDependency(false) }
        val producer = buildProject({ withName("producer").withParent(root) }) { configureAndroidAndMultiplatform() }
        val consumer = buildProject({ withName("consumer").withParent(root) }) { configureAndroidAndMultiplatform() }

        producer.multiplatformExtension.sourceSets.commonMain.dependencies {
            api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.2")
        }

        consumer.multiplatformExtension.sourceSets.commonMain.dependencies {
            implementation(producer)
        }

        root.evaluate()
        producer.evaluate()
        consumer.evaluate()

        producer.mockGenerateProjectStructureMetadataTaskOutputs()

        consumer.kotlinIdeMultiplatformImport.resolveDependencies("commonMain").assertMatches(
            regularSourceDependency(":producer/commonMain"),
            regularSourceDependency(":producer/jvmAndAndroidMain"),
            binaryCoordinates(Regex(".*stdlib.*")),
            binaryCoordinates(Regex("org.jetbrains:annotations.*")),
            binaryCoordinates("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.7.2")
        )

    }

    @Test
    fun `test - default stdlib with no other dependencies`() {
        val project = buildProject { configureAndroidAndMultiplatform(enableDefaultStdlib = true) }
        project.evaluate()

        val stdlibVersion = project.getKotlinPluginVersion()
        val stdlibDependencies = listOf(
            binaryCoordinates("org.jetbrains.kotlin:kotlin-stdlib:${stdlibVersion}"),
            binaryCoordinates("org.jetbrains:annotations:13.0"),
        )

        project.assertBinaryDependencies("commonMain", stdlibDependencies)
        project.assertBinaryDependencies("jvmAndAndroidMain", stdlibDependencies)
        project.assertBinaryDependencies("commonTest", stdlibDependencies)
        project.assertBinaryDependencies("jvmAndAndroidTest", stdlibDependencies)
    }

    @Test
    fun `test - MVIKotlin - binary dependencies - with stdlib enabled by default`() {
        val project = buildProject { configureAndroidAndMultiplatform(enableDefaultStdlib = true) }
        val kotlin = project.multiplatformExtension
        kotlin.sourceSets.getByName("commonMain").dependencies {
            implementation("com.arkivanov.mvikotlin:mvikotlin:3.2.1")
        }

        project.evaluate()

        val kgpVersion = project.getKotlinPluginVersion()
        val jvmAndAndroidDependencies = listOf(
            binaryCoordinates("com.arkivanov.mvikotlin:mvikotlin-jvm:3.2.1"),
            binaryCoordinates("com.arkivanov.essenty:lifecycle-jvm:1.0.0"),
            binaryCoordinates("com.arkivanov.essenty:instance-keeper-jvm:1.0.0"),
            legacyStdlibJdkDependencies(),
            binaryCoordinates("org.jetbrains.kotlin:kotlin-stdlib:$kgpVersion"),
            binaryCoordinates("org.jetbrains:annotations:13.0"),
        )

        project.assertBinaryDependencies("commonMain", jvmAndAndroidDependencies)
        project.assertBinaryDependencies("jvmAndAndroidMain", jvmAndAndroidDependencies)
        project.assertBinaryDependencies("commonTest", jvmAndAndroidDependencies)
        project.assertBinaryDependencies("jvmAndAndroidTest", jvmAndAndroidDependencies)
    }

    @Test
    fun `test existence of kotlin-test dependencies for commonTest source set with only JVM+Android target`() {
        val project = buildProject { configureAndroidAndMultiplatform(true) }

        project.multiplatformExtension.sourceSets.getByName("commonTest").dependencies {
            implementation(kotlin("test"))
        }

        project.evaluate()

        val stdlibVersion = project.getKotlinPluginVersion()
        val stdlibDependencies = listOf(
            binaryCoordinates(Regex(".*kotlin-stdlib.*")),
            binaryCoordinates(Regex("org\\.jetbrains:annotations:.*")),
            binaryCoordinates("org.jetbrains.kotlin:kotlin-test-junit:${stdlibVersion}"),
            binaryCoordinates("org.jetbrains.kotlin:kotlin-test:${stdlibVersion}"),
            binaryCoordinates("junit:junit:4.13.2"),
            binaryCoordinates("org.hamcrest:hamcrest-core:1.3"),
        )

        project.assertBinaryDependencies("commonTest", stdlibDependencies)
    }

    @Test
    fun `KT-71444 Android and JVM fails to transitively resolve kotlin-stdlib-common when it hasn't explicit version set`() {
        val project = buildProject { configureAndroidAndMultiplatform(enableDefaultStdlib = true) }
        val kotlin = project.multiplatformExtension
        kotlin.sourceSets.getByName("commonMain").dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.2")
        }

        project.evaluate()

        val kgpVersion = project.getKotlinPluginVersion()
        // This list is exhaustive. We don't expect to see kotlin-stdlib-common as it should be replaced by kotlin-stdlib
        val jvmAndAndroidDependencies = listOf(
            binaryCoordinates("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.7.2"),
            binaryCoordinates("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.7.2"),
            binaryCoordinates("org.jetbrains.kotlin:kotlin-stdlib:$kgpVersion"),
            binaryCoordinates("org.jetbrains:annotations:13.0"),
        )

        project.assertBinaryDependencies("jvmAndAndroidMain", jvmAndAndroidDependencies)
        project.assertBinaryDependencies("jvmAndAndroidTest", jvmAndAndroidDependencies)
    }
}

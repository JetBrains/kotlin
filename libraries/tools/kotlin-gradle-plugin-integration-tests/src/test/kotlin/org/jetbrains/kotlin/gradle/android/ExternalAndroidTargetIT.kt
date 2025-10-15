/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android

import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.gradle.KOTLIN_VERSION
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.assertMatches
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.binaryCoordinates
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.dependsOnDependency
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.friendSourceDependency
import org.jetbrains.kotlin.gradle.plugin.AndroidGradlePluginVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.jetbrainsAnnotationDependencies
import org.jetbrains.kotlin.gradle.util.kotlinStdlibDependencies
import org.jetbrains.kotlin.gradle.util.resolveIdeDependencies
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.io.path.moveTo
import kotlin.io.path.readText
import kotlin.test.fail

// We are using the latest available AGP in this test suite as a max version
// to ensure AGP and MPP integration is not broken.
// This integration allows AGP to configure android target in MPP.
@AndroidTestVersions(
    minVersion = TestVersions.AGP.AGP_82,
    maxVersion = TestVersions.AGP.AGP_811,
    additionalVersions = [
        TestVersions.AGP.AGP_83,
        TestVersions.AGP.AGP_84,
        TestVersions.AGP.AGP_85,
        TestVersions.AGP.AGP_86,
        TestVersions.AGP.AGP_87,
        TestVersions.AGP.AGP_88,
        TestVersions.AGP.AGP_89,
        TestVersions.AGP.AGP_810,
    ],
)
@AndroidGradlePluginTests
class ExternalAndroidTargetIT : KGPBaseTest() {

    @GradleAndroidTest
    fun `test - simple project - build`(
        gradleVersion: GradleVersion,
        androidVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "externalAndroidTarget-simple",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = androidVersion),
            buildJdk = jdkVersion.location
        ) {
            modifyProjectForAGPVersion(androidVersion)

            build("assemble") {
                assertTasksExecuted(":bundleAndroidMainAar")
                assertFileInProjectExists("build/outputs/aar/externalAndroidTarget-simple.aar")
            }
        }
    }

    @GradleAndroidTest
    fun `test - simple project - testOnJvm`(
        gradleVersion: GradleVersion, androidVersion: String, jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "externalAndroidTarget-simple",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = androidVersion),
            buildJdk = jdkVersion.location
        ) {
            modifyProjectForAGPVersion(androidVersion)

            // Use different task name based on the AGP version
            val agpVersion = TestVersions.AgpCompatibilityMatrix.fromVersion(androidVersion)
            val taskName = when {
                agpVersion >= TestVersions.AgpCompatibilityMatrix.AGP_88 -> "testAndroidHostTest"
                else -> "testAndroidTestOnJvm"
            }

            build(taskName, forwardBuildOutput = true) {
                // Check for different output text based on the AGP version
                when {
                    agpVersion >= TestVersions.AgpCompatibilityMatrix.AGP_88 ->
                        assertOutputContains("AndroidHostTest")
                    else ->
                        assertOutputContains("AndroidTestOnJvm")
                }
                assertOutputContains("useCommonMain: CommonMain")
            }
        }
    }

    @GradleAndroidTest
    fun `test - simple project - ide dependency resolution`(
        gradleVersion: GradleVersion, androidVersion: String, jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "externalAndroidTarget-simple",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = androidVersion),
            buildJdk = jdkVersion.location,
        ) {
            modifyProjectForAGPVersion(androidVersion)
            resolveIdeDependencies(
                buildOptions = buildOptions.suppressAgpWarningSinceGradle814(gradleVersion)
            ) { dependencies ->
                dependencies["androidMain"].assertMatches(
                    kotlinStdlibDependencies,
                    jetbrainsAnnotationDependencies,
                    dependsOnDependency(":/commonMain")
                )

                // Use different source set name based on the AGP version
                val agpVersion = TestVersions.AgpCompatibilityMatrix.fromVersion(androidVersion)
                val sourceSetName = when {
                    agpVersion >= TestVersions.AgpCompatibilityMatrix.AGP_88 -> "androidHostTest"
                    else -> "androidTestOnJvm"
                }

                dependencies[sourceSetName].assertMatches(
                    kotlinStdlibDependencies,
                    jetbrainsAnnotationDependencies,
                    dependsOnDependency(":/commonTest"),
                    binaryCoordinates("junit:junit:4.13.2"),
                    binaryCoordinates("org.hamcrest:hamcrest-core:1.3"),
                    friendSourceDependency(":/commonMain"),
                    friendSourceDependency(":/androidMain"),
                )
            }
        }
    }

    @GradleAndroidTest
    fun `test - simple project - pom dependencies rewritten`(
        gradleVersion: GradleVersion, androidVersion: String, jdkVersion: JdkVersions.ProvidedJdk, @TempDir localRepoDir: Path,
    ) {
        val lowestAGPVersion = AndroidGradlePluginVersion(TestVersions.AGP.AGP_810)
        val currentAGPVersion = AndroidGradlePluginVersion(androidVersion)
        val buildOptions = if (currentAGPVersion < lowestAGPVersion) {
            // https://issuetracker.google.com/issues/389951197
            defaultBuildOptions.disableIsolatedProjects()
        } else {
            defaultBuildOptions
        }
        project(
            "externalAndroidTarget-project2project",
            gradleVersion,
            buildOptions = buildOptions
                .copy(androidVersion = androidVersion),
            buildJdk = jdkVersion.location,
            localRepoDir = localRepoDir
        ) {
            modifyProjectForAGPVersion(androidVersion)

            build("publish") {
                val pomFile = localRepoDir.resolve("app/app-android/1.0/app-android-1.0.pom")
                assertFileExists(pomFile)

                fun String.removeWhiteSpaces() = replace("\\s+".toRegex(), "")
                val pomText = pomFile.readText()
                val expectedDependency = """
                    <dependency>
                      <groupId>sample</groupId>
                      <artifactId>tcs-android</artifactId>
                      <version>2.0</version>
                      <scope>compile</scope>
                    </dependency>
                """.trimIndent()

                if (expectedDependency.removeWhiteSpaces() !in pomText.removeWhiteSpaces())
                    fail("Expected to find\n$expectedDependency\nIn POM file\n$pomText")
            }
        }
    }

    @AndroidTestVersions(minVersion = TestVersions.AGP.AGP_811)
    @GradleAndroidTest
    fun `KT-81249 - works with parcelize`(
        gradleVersion: GradleVersion, androidVersion: String, jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "android-multiplatorm-library-with-parcelize",
            gradleVersion,
            buildOptions = defaultBuildOptions
                .copy(androidVersion = androidVersion)
                .copy(compilerArgumentsLogLevel = "warning"),
            buildJdk = jdkVersion.location,
        ) {
            build("assemble") {
                val parcelizeJar = "kotlin-parcelize-compiler-$KOTLIN_VERSION.jar"

                assertTasksExecuted(":compileAndroidMain")
                @Suppress("DEPRECATION")
                val compileAndroidArguments = extractTaskCompilerArguments<K2JVMCompilerArguments>(":compileAndroidMain")
                if (compileAndroidArguments.pluginClasspaths.orEmpty().none { File(it).name == parcelizeJar }) {
                    fail("Expected '$parcelizeJar' to be passed as a plugin classpath to the Kotlin compiler for :compileAndroidMain")
                }

                assertTasksExecuted(":compileKotlinJvm")
                @Suppress("DEPRECATION")
                val compileJvmArguments = extractTaskCompilerArguments<K2JVMCompilerArguments>(":compileKotlinJvm")
                if (compileJvmArguments.pluginClasspaths.orEmpty().any { File(it).name == parcelizeJar }) {
                    fail("Expected '$parcelizeJar' to NOT be passed as a plugin classpath to the Kotlin compiler for :compileKotlinJvm")
                }
            }
        }
    }

    @GradleAndroidTest
    @AndroidTestVersions(minVersion = TestVersions.AGP.AGP_811)
    fun `KT-81060_transform_metadata_dependencies_doesnt_fail_on_configuration_cache_deserialization`(
        gradleVersion: GradleVersion, androidVersion: String, jdkVersion: JdkVersions.ProvidedJdk,
    ) {

        // value source is always executed even with Configuration Cache
        abstract class ClassLoaderHashCode : ValueSource<String, ValueSourceParameters.None> {
            override fun obtain(): String {
                val hash = this.javaClass.classLoader.hashCode()
                println("ClassLoader hash code: #$hash.")
                return """¯\_(ツ)_/¯""" // returned string shouldn't change to not invalidate the configuration cache
            }
        }

        val testProject = project(
            "kt-81060_agp_configuration_cache_class_not_found",
            gradleVersion,
            buildOptions = defaultBuildOptions
                .copy(androidVersion = androidVersion)
                .enableIsolatedProjects(),
            buildJdk = jdkVersion.location,
        ) {
            subProject("composeApp").buildScriptInjection {
                val vs = project.providers.of(ClassLoaderHashCode::class.java) {}
                // this will make value source part of Configuration Cache key -> it will be always executed
                vs.get()
            }
        }

        var hashCode = ""
        testProject.build(":composeApp:transformCommonMainDependenciesMetadata", "--dry-run") {
            hashCode = output.substringAfter("ClassLoader hash code: #").substringBefore(".")
            assertConfigurationCacheStored()
        }

        // This drops previous project Class Loader from cache
        project("empty", gradleVersion) {
            build("help")
            build("help")
        }

        testProject.build(":composeApp:transformCommonMainDependenciesMetadata", "--dry-run") {
            val newHashCode = output.substringAfter("ClassLoader hash code: #").substringBefore(".")
            if (hashCode == newHashCode) {
                fail("ClassLoader hash code is not changed. It seems that Gradle Daemon didn't drop the Classloader Cache, and Heuristic didn't work. Please find another way to verify this issue.")
            }
            assertConfigurationCacheReused()
        }
    }

    private fun TestProject.modifyProjectForAGPVersion(androidVersion: String) {
        val agpVersion = TestVersions.AgpCompatibilityMatrix.fromVersion(androidVersion)
        buildGradleKts.modify {
            val withAndroidTestMethod = when {
                agpVersion >= TestVersions.AgpCompatibilityMatrix.AGP_88 -> "withHostTest {}"
                else -> "withAndroidTestOnJvm {}"
            }
            val androidTestSourceSetName = when {
                agpVersion >= TestVersions.AgpCompatibilityMatrix.AGP_88 -> "androidHostTest"
                else -> "androidTestOnJvm"
            }
            it.replace("<host-test-dsl>", withAndroidTestMethod)
                .replace("<host-test-source-set-name>", androidTestSourceSetName)
        }

        if (agpVersion >= TestVersions.AgpCompatibilityMatrix.AGP_88) {
            projectPath.resolve("src/androidTestOnJvm")
                .moveTo(projectPath.resolve("src/androidHostTest"))
        }
    }
}

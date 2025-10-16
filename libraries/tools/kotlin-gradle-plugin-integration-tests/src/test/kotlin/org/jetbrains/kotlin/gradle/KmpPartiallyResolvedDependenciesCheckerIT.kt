package org.jetbrains.kotlin.gradle

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.assertMatches
import org.jetbrains.kotlin.gradle.idea.testFixtures.utils.*
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.testbase.BuildOptions
import org.jetbrains.kotlin.gradle.testbase.BuildOptions.ConfigurationCacheValue
import org.jetbrains.kotlin.gradle.testbase.GradleAndroidTest
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.MppGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.assertHasDiagnostic
import org.jetbrains.kotlin.gradle.testbase.assertNoDiagnostic
import org.jetbrains.kotlin.gradle.testbase.assertOutputContains
import org.jetbrains.kotlin.gradle.testbase.assertOutputDoesNotContain
import org.jetbrains.kotlin.gradle.testbase.assertTasksExecuted
import org.jetbrains.kotlin.gradle.testbase.build
import org.jetbrains.kotlin.gradle.testbase.buildAndFail
import org.jetbrains.kotlin.gradle.testbase.buildScriptInjection
import org.jetbrains.kotlin.gradle.testbase.compileStubSourceWithSourceSetName
import org.jetbrains.kotlin.gradle.testbase.plugins
import org.jetbrains.kotlin.gradle.testbase.project
import org.jetbrains.kotlin.gradle.testbase.settingsBuildScriptInjection
import org.jetbrains.kotlin.gradle.uklibs.PublisherConfiguration
import org.jetbrains.kotlin.gradle.uklibs.addPublishedProjectToRepositories
import org.jetbrains.kotlin.gradle.uklibs.applyJvm
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.include
import org.jetbrains.kotlin.gradle.uklibs.includeBuild
import org.jetbrains.kotlin.gradle.uklibs.publish
import org.jetbrains.kotlin.gradle.uklibs.publishJava
import org.jetbrains.kotlin.gradle.util.kotlinStdlibDependencies
import org.jetbrains.kotlin.gradle.util.kotlinNativeDistributionDependencies
import org.jetbrains.kotlin.gradle.util.resolveIdeDependencies
import org.junit.jupiter.api.DisplayName

@DisplayName("Tests for diagnostic about partially unresolved KMP dependencies")
@MppGradlePluginTests
class KmpPartiallyResolvedDependenciesCheckerIT : KGPBaseTest() {

    @GradleTest
    fun `partially resolved kmp dependencies checker - permits resolvable compilations`(gradleVersion: GradleVersion) {
        val producer = project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                }
            }
        }.publish(publisherConfiguration = PublisherConfiguration(group = "foo"))

        val consumer = project("empty", gradleVersion) {
            addPublishedProjectToRepositories(producer)
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    js()
                    iosArm64()

                    sourceSets.commonMain.dependencies {
                        implementation(producer.rootCoordinate)
                    }
                }
            }
        }

        /**
         * Make sure resolvable compilations succeed, but emit diagnostic
         */
        consumer.build("compileKotlinIosArm64") {
            assertHasDiagnostic(KotlinToolingDiagnostics.PartiallyResolvedKmpDependencies)
        }

        consumer.buildAndFail("compileKotlinJs") {
            assertHasDiagnostic(KotlinToolingDiagnostics.PartiallyResolvedKmpDependencies)
        }

        consumer.resolveIdeDependencies(
            buildOptions = defaultBuildOptions.copy(configurationCache = ConfigurationCacheValue.DISABLED),
        ) { container ->
            container["commonMain"].assertMatches(
                kotlinStdlibDependencies,
                unresolvedDependenciesDiagnosticMatcher(dependencyName = "foo:empty"),
            )
        }
    }

    @GradleTest
    fun `partially resolved kmp dependencies checker - smoke test project dependency`(gradleVersion: GradleVersion) {
        val consumer = project("empty", gradleVersion) {
            val producer = project("empty", gradleVersion) {
                plugins {
                    kotlin("multiplatform")
                }
                buildScriptInjection {
                    project.applyMultiplatform {
                        linuxArm64()
                        sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                    }
                }
            }

            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    linuxArm64()
                    jvm()

                    sourceSets.commonMain.dependencies {
                        implementation(project(":producer"))
                    }
                }
            }

            include(producer, "producer")
        }

        consumer.buildAndFail("compileKotlinJvm") {
            assertHasDiagnostic(KotlinToolingDiagnostics.PartiallyResolvedKmpDependencies)
        }

        consumer.build("compileKotlinLinuxArm64") {
            assertHasDiagnostic(KotlinToolingDiagnostics.PartiallyResolvedKmpDependencies)
        }
    }


    @GradleTest
    fun `partially resolved kmp dependencies checker - smoke test included build`(gradleVersion: GradleVersion) {
        val consumer = project("empty", gradleVersion) {
            val producer = project("empty", gradleVersion) {
                settingsBuildScriptInjection {
                    settings.rootProject.name = "foo"
                }
                plugins {
                    kotlin("multiplatform")
                }
                buildScriptInjection {
                    project.applyMultiplatform {
                        linuxArm64()
                        sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                    }
                }
            }

            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    linuxArm64()
                    jvm()

                    sourceSets.commonMain.dependencies {
                        implementation("foo:bar:1.0")
                    }
                }
            }

            includeBuild(producer) {
                dependencySubstitution {
                    it.substitute(it.module("foo:bar")).using(
                        it.project(":")
                    )
                }
            }
        }

        consumer.build("compileKotlinLinuxArm64") {
            assertHasDiagnostic(KotlinToolingDiagnostics.PartiallyResolvedKmpDependencies)
        }
        consumer.buildAndFail("compileKotlinJvm") {
            // FIXME: KT-79204 - Java task tries to compute cross-project dependencies before we have a chance to emit diagnostic
            // assertHasDiagnostic(KotlinToolingDiagnostics.PartiallyResolvedKmpDependencies)
        }
    }

    @GradleAndroidTest
    fun `partially resolved kmp dependencies checker - doesn't trigger AGP checker about configurations resolved at configuration time - KT-79559`(
        gradleVersion: GradleVersion,
        agpVersion: String,
    ) {
        val consumer = project("empty", gradleVersion, buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion)) {
            val producer = project("empty", gradleVersion) {
                plugins {
                    kotlin("multiplatform")
                }
                buildScriptInjection {
                    project.applyMultiplatform {
                        linuxArm64()
                        sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                    }
                }
            }

            plugins {
                kotlin("multiplatform")
                id("com.android.library")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    linuxArm64()
                    @Suppress("DEPRECATION")
                    androidTarget()
                    jvm()

                    sourceSets.commonMain.dependencies {
                        implementation(project(":producer"))
                    }

                    androidLibrary.compileSdk = 33
                    androidLibrary.namespace = "foo"
                }
            }

            include(producer, "producer")
        }

        consumer.build("compileKotlinLinuxArm64") {
            assertOutputDoesNotContain("Configuration 'jvmCompileClasspath' was resolved during configuration time")
            assertHasDiagnostic(KotlinToolingDiagnostics.PartiallyResolvedKmpDependencies)
        }
        consumer.buildAndFail("compileKotlinJvm") {
            // See: KT-79559
            assertNoDiagnostic(KotlinToolingDiagnostics.PartiallyResolvedKmpDependencies)
        }
        consumer.buildAndFail("compileDebugKotlinAndroid") {
            assertNoDiagnostic(KotlinToolingDiagnostics.PartiallyResolvedKmpDependencies)
        }
    }

    @GradleTest
    fun `partially resolved kmp dependencies checker - platform specific dependencies produce diagnostic`(gradleVersion: GradleVersion) {
        val producer = project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    js()
                    jvm()
                    iosArm64()
                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                }
            }
        }.publish(
            deriveBuildOptions = {
                // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
                defaultBuildOptions.copy(isolatedProjects = BuildOptions.IsolatedProjectsMode.DISABLED)
            }
        )

        val consumer = project("empty", gradleVersion) {
            addPublishedProjectToRepositories(producer)
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    js()
                    jvm()
                    iosArm64()

                    sourceSets.commonMain.dependencies {
                        implementation("${producer.group}:${producer.name}-js:${producer.version}")
                    }
                }
            }
        }

        consumer.buildAndFail("compileKotlinJvm") {
            assertHasDiagnostic(KotlinToolingDiagnostics.PartiallyResolvedKmpDependencies)
        }
        consumer.buildAndFail("compileKotlinIosArm64") {
            assertHasDiagnostic(KotlinToolingDiagnostics.PartiallyResolvedKmpDependencies)
        }
        consumer.build(
            "compileKotlinJs",
            // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
            buildOptions = defaultBuildOptions.copy(isolatedProjects = BuildOptions.IsolatedProjectsMode.DISABLED)
        ) {
            assertHasDiagnostic(KotlinToolingDiagnostics.PartiallyResolvedKmpDependencies)
            assertTasksExecuted(":checkKotlinGradlePluginConfigurationErrors")
        }
    }

    @GradleTest
    fun `partially resolved kmp dependencies checker - KGP JVM dependencies produce diagnostic`(gradleVersion: GradleVersion) {
        val producer = project("empty", gradleVersion) {
            plugins {
                kotlin("jvm")
            }
            buildScriptInjection {
                project.applyJvm {
                    sourceSets.getByName("main").compileStubSourceWithSourceSetName()
                }
            }
        }.publishJava()

        val consumer = project("empty", gradleVersion) {
            addPublishedProjectToRepositories(producer)
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    jvm()
                    iosArm64()
                    iosX64()

                    sourceSets.commonMain.dependencies {
                        implementation(producer.rootCoordinate)
                    }
                }
            }
        }

        consumer.build("compileKotlinJvm") {
            assertHasDiagnostic(KotlinToolingDiagnostics.PartiallyResolvedKmpDependencies)
        }
        consumer.buildAndFail("compileKotlinIosArm64") {
            assertHasDiagnostic(KotlinToolingDiagnostics.PartiallyResolvedKmpDependencies)
        }
    }

    /**
     * This case explodes in :kotlinx-coroutines/serialization-bom
     */
    @GradleTest
    fun `partially resolved kmp dependencies checker - cross-project configuration doesn't explode with the checker`(gradleVersion: GradleVersion) {
        val consumer = project("empty", gradleVersion) {
            val producer = project("empty", gradleVersion) {
                plugins {
                    kotlin("multiplatform")
                }
                buildScriptInjection {
                    project.applyMultiplatform {
                        jvm()
                        iosArm64()
                        iosX64()
                        sourceSets.getByName("commonMain").compileStubSourceWithSourceSetName()
                        sourceSets.commonMain.dependencies {
                            implementation(project(":"))
                        }
                    }
                }
            }

            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    jvm()
                    iosArm64()

                    sourceSets.getByName("commonMain").compileStubSourceWithSourceSetName()
                    project.project(":producer").afterEvaluate {
                        sourceSets.getByName("commonMain").dependencies {
                            implementation(kotlin("stdlib"))
                        }
                    }
                }
            }

            include(producer, "producer")
        }

        consumer.buildAndFail(
            ":producer:assemble",
            buildOptions = defaultBuildOptions.copy(
                isolatedProjects = BuildOptions.IsolatedProjectsMode.DISABLED,
            )
        ) {
            assertHasDiagnostic(KotlinToolingDiagnostics.PartiallyResolvedKmpDependencies)
        }
        consumer.build(
            "compileKotlinJvm",
            buildOptions = defaultBuildOptions.copy(
                isolatedProjects = BuildOptions.IsolatedProjectsMode.DISABLED,
            )
        ) {
            assertHasDiagnostic(KotlinToolingDiagnostics.PartiallyResolvedKmpDependencies)
        }
    }

    @GradleTest
    fun `KT-79315 - cross-project configuration that materializes other projects doesn't explode with the checker`(gradleVersion: GradleVersion) {
        val parent = project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform").apply(false)
            }
            buildScriptInjection {
                project.allprojects {
                    it.plugins.apply("org.jetbrains.kotlin.multiplatform")
                }
            }
            val a = project("empty", gradleVersion) {
                buildScriptInjection {
                    project.applyMultiplatform {
                        jvm()
                        iosArm64()
                        iosX64()
                        sourceSets.getByName("commonMain").compileStubSourceWithSourceSetName()
                        sourceSets.commonMain.dependencies {
                            implementation(project(":b"))
                        }
                    }
                    project.tasks.all {
                        // Force all tasks (including the checker task) to materialize eagerly
                    }
                }
            }
            val b = project("empty", gradleVersion) {
                buildScriptInjection {
                    project.applyMultiplatform {
                        jvm()
                        iosArm64()
                        sourceSets.getByName("commonMain").compileStubSourceWithSourceSetName()
                    }
                }
            }

            include(a, "a")
            include(b, "b")
        }

        parent.buildAndFail(
            ":a:assemble",
            buildOptions = defaultBuildOptions.copy(
                isolatedProjects = BuildOptions.IsolatedProjectsMode.DISABLED,
            )
        ) {
            // The fact that we don't emit the diagnostic is not desirable, but it's a consequence of suppressing this check before projectsEvaluated
            assertNoDiagnostic(KotlinToolingDiagnostics.PartiallyResolvedKmpDependencies)
        }
        parent.build(
            ":a:compileKotlinJvm",
            buildOptions = defaultBuildOptions.copy(
                isolatedProjects = BuildOptions.IsolatedProjectsMode.DISABLED,
            )
        ) {
            assertNoDiagnostic(KotlinToolingDiagnostics.PartiallyResolvedKmpDependencies)
        }
    }

}
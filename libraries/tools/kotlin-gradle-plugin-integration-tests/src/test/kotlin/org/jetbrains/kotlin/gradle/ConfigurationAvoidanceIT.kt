/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.attributes.Attribute
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.include
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.appendText
import kotlin.test.assertEquals

@DisplayName("Tasks configuration avoidance")
class ConfigurationAvoidanceIT : KGPBaseTest() {

    @JvmGradlePluginTests
    @DisplayName("JVM unrelated tasks are not configured")
    @GradleTest
    fun testUnrelatedTaskNotConfigured(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            val compilationConfiguredTasks = configuredTasks().buildAndReturn("compileKotlin")
            if (gradleVersion < GradleVersion.version("8.0")) {
                assertEquals(
                    mapOf(
                        ":" to setOf(
                            "checkKotlinGradlePluginConfigurationErrors",
                            "clean",
                            "compileDeployJava",
                            "compileJava",
                            "compileKotlin",
                            "compileTestJava",
                            "jar"
                        )
                    ),
                    compilationConfiguredTasks,
                )
            } else {
                assertEquals(
                    mapOf(
                        ":" to setOf(
                            "checkKotlinGradlePluginConfigurationErrors",
                            "clean",
                            "compileJava",
                            "compileKotlin",
                        )
                    ),
                    compilationConfiguredTasks,
                )
            }
        }
    }

    @JvmGradlePluginTests
    @DisplayName("KGP/Jvm does not eagerly configure any tasks")
    @GradleTest
    fun testJvmConfigurationAvoidance(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            assertEquals(
                mapOf(":" to setOf("clean", "help")),
                configuredTasks().buildAndReturn("--dry-run"),
            )
        }
    }

    @OtherGradlePluginTests
    @DisplayName("KGP/Kapt does not eagerly configure any tasks")
    @GradleTest
    fun testKaptConfigurationAvoidance(gradleVersion: GradleVersion) {
        project("kapt2/simple", gradleVersion) {
            assertEquals(
                mapOf(":" to setOf("clean", "help")),
                configuredTasks().buildAndReturn("--dry-run"),
            )
        }
    }

    @AndroidGradlePluginTests
    @DisplayName("Android unrelated tasks are not configured")
    @GradleAndroidTest
    fun testAndroidUnrelatedTaskNotConfigured(
        gradleVersion: GradleVersion,
        agpVersion: String,
        providedJdk: JdkVersions.ProvidedJdk
    ) {
        project(
            "AndroidProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = providedJdk.location
        ) {
            gradleProperties.appendText("android.defaults.buildfeatures.aidl=true")

            assertEquals(
                mapOf(
                    ":" to setOf("help"),
                    ":Android" to setOf("clean"),
                    ":Lib" to setOf("clean"),
                    ":Test" to setOf("clean"),
                ),
                configuredTasks().buildAndReturn("--dry-run"),
            )
            build("help")
        }
    }

    @JsGradlePluginTests
    @DisplayName("JS unrelated tasks are not configured")
    @GradleTest
    fun jsNoTasksConfigured(gradleVersion: GradleVersion) {
        project("kotlin-js-plugin-project", gradleVersion) {
            assertEquals(
                mapOf(":" to setOf("clean", "help")),
                configuredTasks().buildAndReturn("help"),
            )
        }
    }

    @MppGradlePluginTests
    @DisplayName("MPP unrelated tasks are not configured")
    @GradleTest
    fun mppNoTasksConfigured(gradleVersion: GradleVersion) {
        project("new-mpp-lib-and-app/sample-app", gradleVersion) {
            assertEquals(
                mapOf(":" to setOf("clean", "help")),
                configuredTasks().buildAndReturn("help"),
            )
        }
    }

    @MppGradlePluginTests
    @DisplayName("Check configuredTasks dumping utility dumps expected set of tasks")
    @GradleTest
    fun configuredTasksSanityCheck(gradleVersion: GradleVersion) {
        val a = project("empty", gradleVersion) {
            buildScriptInjection {
                project.tasks.register("unrelated")
                project.tasks.register("transitiveInA")
                project.tasks.register("direct") {
                    it.dependsOn("transitiveInA")
                }
            }
        }
        val b = project("empty", gradleVersion) {
            buildScriptInjection {
                project.tasks.register("unrelated")
                project.tasks.register("transitiveInB")
                project.tasks.register("direct") {
                    it.dependsOn("transitiveInB")
                }
            }
        }

        val configuredTasks = project("empty", gradleVersion) {
            settingsBuildScriptInjection {
                settings.rootProject.name = "root"
            }
            include(a, "projectA")
            include(b, "projectB")
        }.configuredTasks().buildAndReturn("direct")
        assertEquals(
            mapOf(
                ":projectA" to setOf("direct", "transitiveInA"),
                ":projectB" to setOf("direct", "transitiveInB"),
            ),
            configuredTasks,
        )
    }

    private fun TestProject.configuredTasks() = buildScriptReturn {
        val configuredTasks = ConcurrentHashMap<String, MutableSet<String>>()
        project.gradle.allprojects { project ->
            project.tasks.configureEach { task ->
                configuredTasks.getOrPut(
                    project.path, { Collections.newSetFromMap(ConcurrentHashMap()) }
                ).add(task.name)
            }
        }
        return@buildScriptReturn configuredTasks
    }

    @JvmGradlePluginTests
    @DisplayName("JVM early configuration resolution")
    @GradleTest
    fun testEarlyConfigurationsResolutionKotlin(gradleVersion: GradleVersion) {
        val eagerlyResolvedConfigurations = testEarlyConfigurationsResolution("kotlinProject", gradleVersion)
        if (gradleVersion < GradleVersion.version("8.0")) {
            assertEquals(
                mapOf(
                    ":" to setOf(
                        "compileClasspath",
                        "compileOnly",
                        "kotlinCompilerPluginClasspathMain",
                        "kotlinCompilerPluginClasspath",
                        "implementation",
                        "api",
                    )
                ),
                eagerlyResolvedConfigurations,
            )
        } else {
            assertEquals(
                mapOf(
                    ":" to setOf(
                        "compileClasspath",
                        "compileOnly",
                        "kotlinCompilerClasspath",
                        "kotlinCompilerPluginClasspathMain",
                        "annotationProcessor",
                        "kotlinCompilerPluginClasspath",
                        "implementation",
                        "api"
                    )
                ),
                eagerlyResolvedConfigurations,
            )
        }
    }

    @JsGradlePluginTests
    @DisplayName("JS early configuration resolution")
    @GradleTest
    @TestMetadata("kotlin-js-browser-project")
    fun testEarlyConfigurationsResolutionKotlinJs(gradleVersion: GradleVersion) {
        val eagerlyResolvedConfigurations = testEarlyConfigurationsResolution("kotlin-js-browser-project", gradleVersion)
        if (gradleVersion < GradleVersion.version("8.0")) {
            assertEquals(
                mapOf(
                    ":base" to setOf(
                        "compileClasspath",
                        "compileOnly",
                        "testCompilationCompileOnly",
                        "testCompileOnly",
                        "compilationImplementation",
                        "testNpmAggregated",
                        "publicPackageJsonConfiguration",
                        "jsApiElements",
                        "implementation",
                        "testImplementation",
                        "compilationRuntimeOnly",
                        "compilationCompileOnly",
                        "compilationApi",
                        "npmAggregated",
                        "testCompilationApi",
                        "api",
                        "jsRuntimeElements",
                        "testApi",
                        "testRuntimeOnly",
                        "runtimeOnly",
                        "testCompilationImplementation",
                        "testCompilationRuntimeOnly"
                    ),
                    ":app" to setOf(
                        "compileClasspath",
                        "compileOnly",
                        "testCompilationCompileOnly",
                        "testCompileOnly",
                        "compilationImplementation",
                        "testNpmAggregated",
                        "implementation",
                        "testImplementation",
                        "runtimeClasspath",
                        "compilationRuntimeOnly",
                        "compilationCompileOnly",
                        "compilationApi",
                        "npmAggregated",
                        "testCompilationApi",
                        "api",
                        "testApi",
                        "testRuntimeOnly",
                        "runtimeOnly",
                        "testCompilationImplementation",
                        "testCompilationRuntimeOnly"
                    ),
                    ":lib" to setOf(
                        "compileClasspath",
                        "compileOnly",
                        "testCompilationCompileOnly",
                        "testCompileOnly",
                        "compilationImplementation",
                        "publicPackageJsonConfiguration",
                        "testNpmAggregated",
                        "jsApiElements",
                        "implementation",
                        "testImplementation",
                        "compilationCompileOnly",
                        "compilationRuntimeOnly",
                        "compilationApi",
                        "npmAggregated",
                        "testCompilationApi",
                        "api",
                        "jsRuntimeElements",
                        "testApi",
                        "testRuntimeOnly",
                        "runtimeOnly",
                        "testCompilationImplementation",
                        "testCompilationRuntimeOnly"
                    )
                ),
                eagerlyResolvedConfigurations,
            )
        } else {
            assertEquals(
                mapOf(
                    ":base" to setOf(
                        "testCompileOnly",
                        "archives",
                        "testNpmAggregated",
                        "publicPackageJsonConfiguration",
                        "jsApiElements",
                        "testImplementation",
                        "compilationRuntimeOnly",
                        "default",
                        "kotlinCompilerPluginClasspathMain",
                        "api",
                        "jsRuntimeElements",
                        "testApi",
                        "testRuntimeOnly",
                        "testCompilationImplementation",
                        "runtimeOnly",
                        "compileClasspath",
                        "compileOnly",
                        "testCompilationCompileOnly",
                        "compilationImplementation",
                        "commonFakeApiElements",
                        "implementation",
                        "jsSourcesElements",
                        "compilationCompileOnly",
                        "compilationApi",
                        "kotlinCompilerClasspath",
                        "npmAggregated",
                        "kotlinCompilerPluginClasspath",
                        "testCompilationApi",
                        "testCompilationRuntimeOnly"
                    ),
                    ":app" to setOf(
                        "compileClasspath",
                        "compileOnly",
                        "testCompilationCompileOnly",
                        "testCompileOnly",
                        "compilationImplementation",
                        "testNpmAggregated",
                        "implementation",
                        "testImplementation",
                        "runtimeClasspath",
                        "compilationRuntimeOnly",
                        "compilationCompileOnly",
                        "compilationApi",
                        "kotlinCompilerClasspath",
                        "npmAggregated",
                        "kotlinCompilerPluginClasspathMain",
                        "kotlinCompilerPluginClasspath",
                        "testCompilationApi",
                        "api",
                        "testApi",
                        "testRuntimeOnly",
                        "runtimeOnly",
                        "testCompilationImplementation",
                        "testCompilationRuntimeOnly"
                    ),
                    ":lib" to setOf(
                        "archives",
                        "testCompileOnly",
                        "publicPackageJsonConfiguration",
                        "testNpmAggregated",
                        "jsApiElements",
                        "testImplementation",
                        "compilationRuntimeOnly",
                        "default",
                        "kotlinCompilerPluginClasspathMain",
                        "api",
                        "jsRuntimeElements",
                        "testApi",
                        "testRuntimeOnly",
                        "testCompilationImplementation",
                        "runtimeOnly",
                        "compileClasspath",
                        "compileOnly",
                        "testCompilationCompileOnly",
                        "commonFakeApiElements",
                        "compilationImplementation",
                        "implementation",
                        "jsSourcesElements",
                        "compilationCompileOnly",
                        "compilationApi",
                        "kotlinCompilerClasspath",
                        "npmAggregated",
                        "kotlinCompilerPluginClasspath",
                        "testCompilationApi",
                        "testCompilationRuntimeOnly"
                    )
                ),
                eagerlyResolvedConfigurations,
            )
        }
    }

    @MppGradlePluginTests
    @DisplayName("Check eagerlyResolvedConfigurations dumping utility dumps expected set of configurations")
    @GradleTest
    fun eagerlyResolvedConfigurationsSanityCheck(gradleVersion: GradleVersion) {
        val producer = project("empty", gradleVersion) {
            buildScriptInjection {
                project.configurations.create("neverResolvedInProducer") {
                    it.isCanBeConsumed = false
                    it.isCanBeResolved = true
                }
                val resolvable = project.configurations.create("resolvableTransitivelyEagerlyInProducer") {
                    it.isCanBeConsumed = false
                    it.isCanBeResolved = true
                }
                val attribute = Attribute.of("attribute", String::class.java)
                project.configurations.create("consumableEagerlyInProducer") {
                    it.extendsFrom(resolvable)
                    it.isCanBeConsumed = true
                    it.isCanBeResolved = false
                    it.attributes.attribute(attribute, "value")
                }
            }
        }

        val consumer = project("empty", gradleVersion) {
            buildScriptInjection {
                val attribute = Attribute.of("attribute", String::class.java)
                project.configurations.create("resolvedEagerlyInConsumer") {
                    it.attributes.attribute(attribute, "value")
                    it.dependencies.add(project.dependencies.project(mapOf("path" to ":producer")))
                }.resolve()

                val resolvedAtExecution = project.configurations.create("resolvedAtExecutionInConsumer")
                project.tasks.register("execute") {
                    it.doLast {
                        resolvedAtExecution.resolve()
                    }
                }
            }
        }

        val resolvedConfigurations = project("empty", gradleVersion) {
            settingsBuildScriptInjection {
                settings.rootProject.name = "root"
            }
            include(producer, "producer")
            include(consumer, "consumer")
        }.eagerlyResolvedConfigurations().buildAndReturn(":consumer:execute")
        assertEquals(
            mapOf(
                ":producer" to setOf("consumableEagerlyInProducer", "resolvableTransitivelyEagerlyInProducer"),
                ":consumer" to setOf("resolvedEagerlyInConsumer"),
            ),
            resolvedConfigurations,
        )
    }

    private fun TestProject.eagerlyResolvedConfigurations(): ReturnFromBuildScriptAfterExecution<EagerlyResolvedConfigurationsReturn> {
        val eagerlyResolvedConfigurationsProperty = "eagerlyResolvedConfigurations"
        buildScriptInjection {
            val eagerlyResolvedConfigurations = EagerlyResolvedConfigurationsReturn()
            project.extraProperties.set(eagerlyResolvedConfigurationsProperty, eagerlyResolvedConfigurations)

            val configurationTime = AtomicBoolean(true)
            project.gradle.taskGraph.whenReady {
                configurationTime.set(false)
            }

            project.gradle.allprojects { project ->
                // This used to be forEach. Was this intentional?
                project.configurations.all { configuration ->
                    // withDependencies opposed to incoming.beforeResolve is called for resolvable and consumable configurations. Do we want both?
                    configuration.withDependencies {
                        if (configurationTime.get()) {
                            eagerlyResolvedConfigurations.getOrPut(
                                project.path, { Collections.newSetFromMap(ConcurrentHashMap()) }
                            ).add(configuration.name)
                        }
                    }
                }
            }
        }

        return buildScriptReturn {
            @Suppress("UNCHECKED_CAST")
            project.extraProperties.get(eagerlyResolvedConfigurationsProperty) as EagerlyResolvedConfigurationsReturn
        }
    }

    private fun testEarlyConfigurationsResolution(
        projectName: String,
        gradleVersion: GradleVersion,
        buildOptions: BuildOptions = defaultBuildOptions
    ) = project(
        projectName,
        gradleVersion,
        buildOptions = buildOptions
    ).eagerlyResolvedConfigurations().buildAndReturn(
        "assemble",
        "-m",
    )
}

private typealias EagerlyResolvedConfigurationsReturn = ConcurrentHashMap<String, MutableSet<String>>
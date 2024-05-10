/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.*
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText
import kotlin.test.assertTrue

@DisplayName("Multiplatform variant aware dependency resolution")
@MppGradlePluginTests
class VariantAwareDependenciesMppIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions = super.defaultBuildOptions.copy(
        nativeOptions = super.defaultBuildOptions.nativeOptions.copy(
            // Use kotlin-native bundle version provided by default in KGP, because it will be pushed in one of the known IT repos for sure
            version = null
        )
    )

    @DisplayName("JVM project could depend on multiplatform project")
    @GradleTest
    fun testJvmKtAppResolvesMppLib(gradleVersion: GradleVersion) {
        project("new-mpp-lib-and-app/sample-lib", gradleVersion) {
            includeOtherProjectAsSubmodule("simpleProject")

            subProject("simpleProject")
                .buildGradle
                .appendText(
                    """
                    |
                    |dependencies { implementation project(":") }
                    |
                    """.trimMargin()
                )

            testResolveAllConfigurations("simpleProject") { _, buildResult ->
                buildResult.assertOutputContains(">> :simpleProject:runtimeClasspath --> sample-lib-jvm6-1.0.jar")
            }
        }
    }

    @DisplayName("JS project could depend on multiplatform project")
    @GradleTest
    fun testJsKtAppResolvesMppLib(gradleVersion: GradleVersion) {
        project("new-mpp-lib-and-app/sample-lib", gradleVersion) {
            includeOtherProjectAsSubmodule("kotlin2JsInternalTest")

            subProject("kotlin2JsInternalTest")
                .buildGradle
                .appendText(
                    """
                    |
                    |dependencies { implementation rootProject }
                    |
                    """.trimMargin()
                )

            testResolveAllConfigurations("kotlin2JsInternalTest") { _, buildResult ->
                buildResult.assertOutputContains(">> :kotlin2JsInternalTest:runtimeClasspath --> sample-lib-nodejs-1.0.klib")
            }
        }
    }

    @DisplayName("Multiplatform project could depend on JVM project")
    @GradleTest
    fun testMppLibResolvesJvmKtApp(gradleVersion: GradleVersion) {
        project("new-mpp-lib-and-app/sample-lib", gradleVersion) {
            includeOtherProjectAsSubmodule("simpleProject")
            buildGradle.appendText(
                """
                    |
                    |dependencies { jvm6MainImplementation project(':simpleProject') }
                    |
                    """.trimMargin()
            )

            testResolveAllConfigurations()
        }
    }

    @DisplayName("Multiplatform project could depend on JS project")
    @GradleTest
    fun testMppLibResolvesJsKtApp(gradleVersion: GradleVersion) {
        project("new-mpp-lib-and-app/sample-lib", gradleVersion) {
            includeOtherProjectAsSubmodule("kotlin2JsInternalTest")

            buildGradle.appendText(
                """
                |
                |dependencies { nodeJsMainImplementation project(':kotlin2JsInternalTest') }
                |
                """.trimMargin()
            )

            testResolveAllConfigurations()
        }
    }

    @DisplayName("Gradle JVM project could depend on multiplatform project")
    @GradleTest
    fun testNonKotlinJvmAppResolvesMppLib(gradleVersion: GradleVersion) {
        project("new-mpp-lib-and-app/sample-lib", gradleVersion) {
            includeOtherProjectAsSubmodule("simpleProject")
            subProject("simpleProject").buildGradle.modify {
                // In Gradle 5.3+, the variants of a Kotlin MPP can't be disambiguated in a pure Java project's deprecated
                // configurations that don't have a proper 'org.gradle.usage' attribute value, see KT-30378
                it.checkedReplace("id \"org.jetbrains.kotlin.jvm\"", "") +
                        """
                    |
                    |configurations {
                    |    configure([compile, runtime, deployCompile, deployCompileOnly, deployRuntime,
                    |        testCompile, testRuntime, getByName('default')]) {
                    |        canBeResolved = false
                    |    }
                    |}
                    |
                    |dependencies { implementation rootProject }
                    |
                    """.trimMargin()
            }

            testResolveAllConfigurations("simpleProject")
        }
    }

    @DisplayName("Kotlin JVM project could depend on another Kotlin JVM project")
    @GradleTest
    fun testJvmKtAppResolvesJvmKtApp(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            includeOtherProjectAsSubmodule("jvmTarget")
            subProject("jvmTarget").buildGradle.modify {
                it.replace("kotlinOptions.jvmTarget = \"1.7\"", "kotlinOptions.jvmTarget = \"11\"") +
                        """
                        |
                        |dependencies { implementation rootProject }
                        |
                        """.trimMargin()
            }

            testResolveAllConfigurations("jvmTarget")
        }
    }

    @DisplayName("Multiplatform project could depend on Kotlin JVM and JS projects")
    @GradleTest
    fun testMppResolvesJvmAndJsKtLibs(gradleVersion: GradleVersion) {
        project("new-mpp-lib-and-app/sample-lib", gradleVersion) {
            includeOtherProjectAsSubmodule("simpleProject")
            includeOtherProjectAsSubmodule("kotlin2JsInternalTest")

            buildGradle.appendText(
                """
                |
                |dependencies {
                |   def jvmCompilations = kotlin.getTargets().getByName("jvm6").getCompilations()
                |   def jsCompilations = kotlin.getTargets().getByName("nodeJs").getCompilations()
                |
                |   def jvmMainImplConfigName = jvmCompilations.getByName("main").getImplementationConfigurationName()
                |   def jvmTestImplConfigName = jvmCompilations.getByName("test").getImplementationConfigurationName()
                |   def jsMainImplConfigName = jsCompilations.getByName("main").getImplementationConfigurationName()
                |   def jsTestImplConfigName = jsCompilations.getByName("test").getImplementationConfigurationName()
                |
                |   add(jvmMainImplConfigName, project(':simpleProject'))
                |   add(jvmTestImplConfigName, project(':simpleProject'))
                |   add(jsMainImplConfigName, project(':kotlin2JsInternalTest'))
                |   add(jsTestImplConfigName, project(':kotlin2JsInternalTest'))
                |}
                """.trimMargin()
            )

            testResolveAllConfigurations()
        }
    }

    @DisplayName("Kotlin JVM project could depend on multiplatform project in testRuntime configuration")
    @GradleTest
    fun testJvmKtAppDependsOnMppTestRuntime(gradleVersion: GradleVersion) {
        project("new-mpp-lib-and-app/sample-lib", gradleVersion) {
            includeOtherProjectAsSubmodule("simpleProject")

            subProject("simpleProject").buildGradle.appendText(
                """
                |
                |dependencies { testImplementation project(path: ':', configuration: 'jvm6RuntimeElements') }
                |
                """.trimMargin()
            )

            testResolveAllConfigurations("simpleProject") { _, buildResult ->
                buildResult.assertOutputContains(">> :simpleProject:testCompileClasspath --> sample-lib-jvm6-1.0.jar")
                buildResult.assertOutputContains(">> :simpleProject:testRuntimeClasspath --> sample-lib-jvm6-1.0.jar")
            }
        }
    }

    @DisplayName("Multiplatform project with Java plugin applied could be resolved in all configurations")
    @GradleTest
    fun testJvmWithJavaProjectCanBeResolvedInAllConfigurations(gradleVersion: GradleVersion) {
        project("new-mpp-jvm-with-java-multi-module", gradleVersion) {
            testResolveAllConfigurations("app")
        }
    }

    @DisplayName("Configurations with no explicit usage could be resolved")
    @GradleTest
    fun testConfigurationsWithNoExplicitUsageResolveRuntime(gradleVersion: GradleVersion) {
        // Starting with Gradle 5.0, plain Maven dependencies are represented as two variants, and resolving them to the API one leads
        // to transitive dependencies left out of the resolution results. We need to ensure that our attributes schema does not lead to the API
        // variants chosen over the runtime ones when resolving a configuration with no required Usage:
        project("simpleProject", gradleVersion) {
            buildGradle.appendText(
                """
                |
                |dependencies { implementation 'org.jetbrains.kotlin:kotlin-compiler-embeddable' }
                |
                |configurations {
                |    customConfiguration.extendsFrom implementation
                |    customConfiguration.canBeResolved(true)
                |}
                |
                """.trimMargin()
            )

            testResolveAllConfigurations { _, buildResult ->
                buildResult.assertOutputContains(
                    ">> :customConfiguration --> kotlin-compiler-embeddable-${defaultBuildOptions.kotlinVersion}.jar"
                )

                // Check that the transitive dependencies with 'runtime' scope are also available:
                buildResult.assertOutputContains(
                    ">> :customConfiguration --> kotlin-script-runtime-${defaultBuildOptions.kotlinVersion}.jar"
                )
            }
        }
    }

    @DisplayName("Elements configurations could be resolved correctly")
    @GradleTest
    fun testCompileAndRuntimeResolutionOfElementsConfigurations(gradleVersion: GradleVersion) {
        project("new-mpp-lib-and-app/sample-app", gradleVersion) {
            includeOtherProjectAsSubmodule(
                otherProjectName = "sample-lib",
                pathPrefix = "new-mpp-lib-and-app"
            )
            buildGradle.replaceText("'com.example:sample-lib:1.0'", "project(':sample-lib')")

            val isAtLeastGradle75 = gradleVersion >= GradleVersion.version("7.5")

            listOf("jvm6" to "Classpath", "nodeJs" to "Classpath").forEach { (target, suffix) ->
                build("dependencyInsight", "--configuration", "${target}Compile$suffix", "--dependency", "sample-lib") {
                    if (isAtLeastGradle75) {
                        assertOutputContains("Variant ${target}ApiElements")
                    } else {
                        assertOutputContains("variant \"${target}ApiElements\" [")
                    }
                }

                if (suffix == "Classpath") {
                    build("dependencyInsight", "--configuration", "${target}Runtime$suffix", "--dependency", "sample-lib") {
                        if (isAtLeastGradle75) {
                            assertOutputContains("Variant ${target}RuntimeElements")
                        } else {
                            assertOutputContains("variant \"${target}RuntimeElements\" [")
                        }
                    }
                }
            }
        }
    }

    @DisplayName("Custom configuration with dependency on multiplatform project could be resolved")
    @GradleTest
    fun testResolveDependencyOnMppInCustomConfiguration(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            buildGradle.appendText(
                """
                |
                |configurations.create("custom")
                |dependencies { custom("org.jetbrains.kotlinx:atomicfu:${TestVersions.ThirdPartyDependencies.KOTLINX_ATOMICFU}") }
                |tasks.register("resolveCustom") { doLast { println("###" + configurations.custom.toList()) } }
                |
                """.trimMargin()
            )

            build("resolveCustom") {
                val printedLine = output.lines().single { "###[" in it }.substringAfter("###")
                val items = printedLine.removeSurrounding("[", "]").split(", ")
                assertTrue(items.toString()) { items.any { "atomicfu-jvm" in it } }
            }
        }
    }
}

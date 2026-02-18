/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dependencyResolutionTests

import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.internal.dsl.KotlinMultiplatformSourceSetConventionsImpl.commonMain
import org.jetbrains.kotlin.gradle.internal.dsl.KotlinMultiplatformSourceSetConventionsImpl.commonTest
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.testing.PrettyPrint
import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.jetbrains.kotlin.gradle.unitTests.uklibs.GradleComponent
import org.jetbrains.kotlin.gradle.unitTests.uklibs.GradleMetadataComponent
import org.jetbrains.kotlin.gradle.unitTests.uklibs.GradleMetadataComponent.Variant
import org.jetbrains.kotlin.gradle.unitTests.uklibs.MavenComponent
import org.jetbrains.kotlin.gradle.unitTests.uklibs.generateMockRepository
import org.jetbrains.kotlin.gradle.unitTests.uklibs.jvmRuntimeAttributes
import org.jetbrains.kotlin.gradle.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests that verify dependencies added to the top-level block are visible in commonMain and commonTest source sets.
 */
class KotlinTopLevelDependenciesTest : SourceSetDependenciesResolution() {

    private fun Project.defaultTargets() {
        kotlin { jvm(); linuxX64(); js(); }
    }

    @Test
    fun topLevelDependenciesVisibleInCommonSourceSets() {
        val project = buildProjectWithMPP {
            defaultTargets()
            kotlin {
                dependencies {
                    api("test:api:1.0")
                    implementation("test:implementation:1.0")
                    compileOnly("test:compileOnly:1.0")
                    runtimeOnly("test:runtimeOnly:1.0")

                    testImplementation("test:test-implementation:1.0")
                    testCompileOnly("test:test-compileOnly:1.0")
                    testRuntimeOnly("test:test-runtimeOnly:1.0")

                    implementation(kotlin("gradle-plugin", "2.0.0"))
                    testImplementation(kotlin("test"))
                }

                sourceSets.commonMain.dependencies {
                    api("test:commonMainApi:1.0")
                    implementation("test:commonMainImplementation:1.0")
                    compileOnly("test:commonMainCompileOnly:1.0")
                    runtimeOnly("test:commonMainRuntimeOnly:1.0")
                }

                sourceSets.commonTest.dependencies {
                    implementation("test:commonTestImplementation:1.0")
                    compileOnly("test:commonTestCompileOnly:1.0")
                    runtimeOnly("test:commonTestRuntimeOnly:1.0")
                }
            }
        }
        project.evaluate()

        fun assertDependencies(configurationName: String, vararg dependencyNotations: String) {
            val configuration = project.configurations.getByName(configurationName)
            assertEquals(
                dependencyNotations.toList(),
                configuration.allDependencies.map { it.toString() },
                "Expected $configurationName to contain exactly ${dependencyNotations.toList()}"
            )
        }

        val commonMain = project.multiplatformExtension.sourceSets.commonMain.get()
        assertDependencies(
            commonMain.apiConfigurationName,
            "test:api:1.0",
            "test:commonMainApi:1.0",
        )
        assertDependencies(
            commonMain.implementationConfigurationName,
            "test:implementation:1.0",
            "org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.0",
            "test:commonMainImplementation:1.0",
        )
        assertDependencies(
            commonMain.compileOnlyConfigurationName,
            "test:compileOnly:1.0",
            "test:commonMainCompileOnly:1.0",
        )
        assertDependencies(
            commonMain.runtimeOnlyConfigurationName,
            "test:runtimeOnly:1.0",
            "test:commonMainRuntimeOnly:1.0",
        )

        val commonTest = project.multiplatformExtension.sourceSets.commonTest.get()
        assertDependencies(
            commonTest.implementationConfigurationName,
            "test:test-implementation:1.0",
            "org.jetbrains.kotlin:kotlin-test:",
            "test:commonTestImplementation:1.0",
        )
        assertDependencies(
            commonTest.compileOnlyConfigurationName,
            "test:test-compileOnly:1.0",
            "test:commonTestCompileOnly:1.0"
        )
        assertDependencies(
            commonTest.runtimeOnlyConfigurationName,
            "test:test-runtimeOnly:1.0",
            "test:commonTestRuntimeOnly:1.0"
        )
    }

    @Test
    fun topLevelDependenciesAreCorrectlyPropagatedToSourceSets() {
        assertSourceSetDependenciesResolution("topLevelDependenciesAreCorrectlyPropagatedToSourceSets.txt") { project ->
            project.defaultTargets()
            project.kotlin {
                dependencies {
                    api(mockedDependency("top-level-api", "1.0"))
                    implementation(mockedDependency("top-level-implementation", "1.0"))
                    compileOnly(mockedDependency("top-level-compileOnly", "1.0"))
                    runtimeOnly(mockedDependency("top-level-runtimeOnly", "1.0"))

                    testImplementation(mockedDependency("top-level-test-implementation", "1.0"))
                    testCompileOnly(mockedDependency("top-level-test-compileOnly", "1.0"))
                    testRuntimeOnly(mockedDependency("top-level-test-runtimeOnly", "1.0"))
                }
            }
        }
    }

    private fun jvmComponent(module: String): GradleComponent {
        val gradleComponentMetadata = GradleMetadataComponent.Component(
            group = "test",
            module = module,
            version = "1.0",
        )
        val mavenComponentMetadata = MavenComponent(
            gradleComponentMetadata.group, gradleComponentMetadata.module, gradleComponentMetadata.version,
            packaging = null,
            dependencies = listOf(),
            true,
        )
        val jvmRuntimeVariant = Variant(
            name = "jvmRuntimeElements",
            attributes = jvmRuntimeAttributes,
            files = listOf(
                GradleMetadataComponent.MockVariantFile(
                    artifactId = module,
                    version = "1.0",
                    extension = "jar",
                )
            ),
            dependencies = listOf()
        )
        return GradleComponent(
            GradleMetadataComponent(
                component = gradleComponentMetadata,
                variants = listOf(jvmRuntimeVariant),
            ),
            mavenComponentMetadata,
        )
    }

    @Test
    fun topLevelDependenciesAreCorrectlyPropagatedToResolvableConfigurationsInCompilation() {
        val repo = generateMockRepository(
            tempFolder,
            listOf(
                jvmComponent("top-level-api"),
                jvmComponent("top-level-implementation"),
                jvmComponent("top-level-compileOnly"),
                jvmComponent("top-level-runtimeOnly"),
                jvmComponent("top-level-test-implementation"),
                jvmComponent("top-level-test-compileOnly"),
                jvmComponent("top-level-test-runtimeOnly"),
            )
        )
        val project = buildProjectWithMPP(preApplyCode = {
            enableDefaultStdlibDependency(false)
            enableDefaultJsDomApiDependency(false)
        }) {
            project.defaultTargets()
            project.repositories.maven(repo)
            project.kotlin {
                dependencies {
                    api("test:top-level-api:1.0")
                    implementation("test:top-level-implementation:1.0")
                    compileOnly("test:top-level-compileOnly:1.0")
                    runtimeOnly("test:top-level-runtimeOnly:1.0")

                    testImplementation("test:top-level-test-implementation:1.0")
                    testCompileOnly("test:top-level-test-compileOnly:1.0")
                    testRuntimeOnly("test:top-level-test-runtimeOnly:1.0")
                }
            }
        }.evaluate()

        val resolvedPerConfigurationComponents: MutableMap<String, Set<String>> = mutableMapOf()
        project.multiplatformExtension.targets
            .filter { it.platformType != KotlinPlatformType.common }
            .forEach { target ->
                target.compilations.forEach { compilation ->
                    val compileClasspath = project.configurations.getByName(compilation.compileDependencyConfigurationName)
                    // Make sure resolution doesn't fail
                    compileClasspath.resolve()
                    resolvedPerConfigurationComponents[compilation.compileDependencyConfigurationName] = compileClasspath
                        .incoming.resolutionResult.allComponents.map { component ->
                            component.id.displayName
                        }.toSet()

                    compilation.runtimeDependencyConfigurationName?.let { runtimeConfiguration ->
                        val runtimeClasspath = project.configurations.getByName(runtimeConfiguration)
                        // Make sure resolution doesn't fail
                        runtimeClasspath.resolve()
                        resolvedPerConfigurationComponents[runtimeConfiguration] = runtimeClasspath
                            .incoming.resolutionResult.allComponents.map { component ->
                                component.id.displayName
                            }.toSet()
                    }
                }
            }

        assertEquals<PrettyPrint<Map<String, Set<String>>>>(
            mutableMapOf(
                "jsCompileClasspath" to mutableSetOf(
                    "root project :",
                    "test:top-level-api:1.0",
                    "test:top-level-compileOnly:1.0",
                    "test:top-level-implementation:1.0",
                ),
                "jsRuntimeClasspath" to mutableSetOf(
                    "root project :",
                    "test:top-level-api:1.0",
                    "test:top-level-implementation:1.0",
                    "test:top-level-runtimeOnly:1.0",
                ),
                "jsTestCompileClasspath" to mutableSetOf(
                    "root project :",
                    "test:top-level-api:1.0",
                    "test:top-level-compileOnly:1.0",
                    "test:top-level-implementation:1.0",
                    "test:top-level-test-compileOnly:1.0",
                    "test:top-level-test-implementation:1.0",
                ),
                "jsTestRuntimeClasspath" to mutableSetOf(
                    "root project :",
                    "test:top-level-api:1.0",
                    "test:top-level-implementation:1.0",
                    "test:top-level-runtimeOnly:1.0",
                    "test:top-level-test-implementation:1.0",
                    "test:top-level-test-runtimeOnly:1.0",
                ),
                "jvmCompileClasspath" to mutableSetOf(
                    "root project :",
                    "test:top-level-api:1.0",
                    "test:top-level-compileOnly:1.0",
                    "test:top-level-implementation:1.0",
                ),
                "jvmRuntimeClasspath" to mutableSetOf(
                    "root project :",
                    "test:top-level-api:1.0",
                    "test:top-level-implementation:1.0",
                    "test:top-level-runtimeOnly:1.0",
                ),
                "jvmTestCompileClasspath" to mutableSetOf(
                    "root project :",
                    "test:top-level-api:1.0",
                    "test:top-level-compileOnly:1.0",
                    "test:top-level-implementation:1.0",
                    "test:top-level-test-compileOnly:1.0",
                    "test:top-level-test-implementation:1.0",
                ),
                "jvmTestRuntimeClasspath" to mutableSetOf(
                    "root project :",
                    "test:top-level-api:1.0",
                    "test:top-level-implementation:1.0",
                    "test:top-level-runtimeOnly:1.0",
                    "test:top-level-test-implementation:1.0",
                    "test:top-level-test-runtimeOnly:1.0",
                ),
                "linuxX64CompileKlibraries" to mutableSetOf(
                    "root project :",
                    "test:top-level-api:1.0",
                    "test:top-level-compileOnly:1.0",
                    "test:top-level-implementation:1.0",
                ),
                "linuxX64TestCompileKlibraries" to mutableSetOf(
                    "root project :",
                    "test:top-level-api:1.0",
                    "test:top-level-implementation:1.0",
                    "test:top-level-test-compileOnly:1.0",
                    "test:top-level-test-implementation:1.0",
                ),
            ).prettyPrinted,
            resolvedPerConfigurationComponents.prettyPrinted,
        )
    }

}

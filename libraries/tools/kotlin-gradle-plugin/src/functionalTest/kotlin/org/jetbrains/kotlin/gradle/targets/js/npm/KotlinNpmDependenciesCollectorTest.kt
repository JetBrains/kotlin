/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.npm.KotlinNpmDependency
import org.jetbrains.kotlin.gradle.npm.npmDependenciesCollector
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.enableDefaultJsDomApiDependency
import org.jetbrains.kotlin.gradle.util.enableDefaultStdlibDependency
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.gradle.utils.property
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.test.assertEquals

/**
 * Test [org.jetbrains.kotlin.gradle.npm.KotlinNpmDependenciesCollector].
 */
class KotlinNpmDependenciesCollectorTest {

    @Test
    fun `npm dependencies declared in the deprecated kotlin DSL are collected`() {
        val project = buildProjectWithNpmDependencies()

        assertEquals(
            listOf(
                "DEV webpack:5.0.0",
                "NORMAL is-even:1.0.0",
                "OPTIONAL is-odd:3.0.0",
                "PEER react:18.0.0",
            ),
            project.collectedNpmDependencies().sorted(),
        )
    }

    @Test
    fun `npm dependencies are collected per source set`() {
        val project = buildProjectWithMPP {
            kotlin {
                js { nodejs() }

                sourceSets.commonMain {
                    dependencies {
                        npmDev("webpack", "5.0.0")
                    }
                }
                sourceSets.jsMain {
                    dependencies {
                        npmPeer("react", "18.0.0")
                    }
                }
            }
        }.evaluate()

        assertEquals(
            listOf("DEV webpack:5.0.0"),
            project.collectedNpmDependencies("commonMain"),
        )
        assertEquals(
            listOf("PEER react:18.0.0"),
            project.collectedNpmDependencies("jsMain"),
        )
    }

    @Test
    fun `npm dependencies declared with providers are resolved lazily`() {
        val project = buildProjectWithMPP {
            val version = project.objects.property<String>()

            kotlin {
                js { nodejs() }

                sourceSets.commonMain {
                    dependencies {
                        npm("is-even", version)
                        npmDev("webpack", version)
                    }
                }
            }

            version.set("5.0.0")
        }.evaluate()

        assertEquals(
            listOf("DEV webpack:5.0.0", "NORMAL is-even:5.0.0"),
            project.collectedNpmDependencies().sorted(),
        )
    }

    @Test
    fun `only the first declaration of an npm package name is collected`() {
        val project = buildProjectWithMPP {
            kotlin {
                js { nodejs() }

                sourceSets.commonMain {
                    dependencies {
                        implementation(npm("is-even", "1.0.0"))
                        implementation(npm("is-even", "2.0.0"))
                    }
                }
            }
        }.evaluate()

        assertEquals(
            listOf("NORMAL is-even:1.0.0"),
            project.collectedNpmDependencies(),
        )
    }

    @Test
    fun `only the first declaration of an npm package name is collected across the deprecated and the new DSL`() {
        val project = buildProjectWithMPP {
            kotlin {
                js { nodejs() }

                sourceSets.commonMain {
                    dependencies {
                        npmDev("is-even", "2.0.0")
                        implementation(npm("is-even", "1.0.0"))
                        npmDev("is-even", project.provider { "3.0.0" })
                    }
                }
            }
        }.evaluate()

        assertEquals(
            listOf("DEV is-even:2.0.0"),
            project.collectedNpmDependencies(),
        )
    }

    @Test
    @Suppress("DEPRECATION") // the deprecated npm DSL is tested on purpose
    fun `npm dependency on a directory is collected with a file version`(
        @TempDir tempDir: Path,
    ) {
        val npmPackageDir = tempDir.resolve("is-even")
        npmPackageDir.toFile().mkdirs()

        val project = buildProjectWithMPP {
            kotlin {
                js { nodejs() }

                sourceSets.commonMain {
                    dependencies {
                        implementation(npm(npmPackageDir.toFile()))
                    }
                }
            }
        }.evaluate()

        assertEquals(
            listOf("NORMAL is-even:file:${npmPackageDir.toAbsolutePath().normalize().absolutePathString()}"),
            project.collectedNpmDependencies(),
        )
    }

    @Test
    fun `package json task inputs contain the collected npm dependencies`() {
        val project = buildProjectWithNpmDependencies()

        assertEquals(
            listOf(
                "NORMAL is-even:1.0.0",
                "OPTIONAL is-odd:3.0.0",
                "PEER react:18.0.0",
                "DEV webpack:5.0.0",
            ),
            project.packageJsonTaskDeclaredNpmDependencies(),
        )
    }

    @Suppress("DEPRECATION") // the deprecated npm DSL is tested on purpose
    private fun buildProjectWithNpmDependencies(): Project =
        buildProjectWithMPP(preApplyCode = { disableDefaultDependencies() }) {
            kotlin {
                js { nodejs() }

                sourceSets.commonMain {
                    dependencies {
                        implementation(npm("is-even", "1.0.0"))
                        implementation(optionalNpm("is-odd", "3.0.0"))
                        implementation(peerNpm("react", "18.0.0"))
                        implementation(devNpm("webpack", "5.0.0"))
                    }
                }
            }
        }.evaluate()

    private fun Project.collectedNpmDependencies(
        sourceSetName: String = KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME,
    ): List<String> =
        multiplatformExtension.sourceSets
            .getByName(sourceSetName)
            .npmDependenciesCollector
            .npmDependencies.get()
            .map { it.uniqueRepresentation() }

    private fun Project.packageJsonTaskDeclaredNpmDependencies(
        compilationName: String = KotlinCompilation.MAIN_COMPILATION_NAME,
    ): List<String> =
        multiplatformExtension.js()
            .compilations
            .getByName(compilationName)
            .npmProject.packageJsonTask
            .declaredNpmDependencies.get()

    /**
     * The test projects have no repositories,
     * so the default dependencies can't be resolved together with the npm dependencies.
     */
    private fun Project.disableDefaultDependencies() {
        enableDefaultStdlibDependency(false)
        enableDefaultJsDomApiDependency(false)
    }

    private fun KotlinNpmDependency.uniqueRepresentation(): String =
        "$scope $name:$version"
}

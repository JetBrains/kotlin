/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.npm.KotlinNpmDependency
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.plugin.sources.npmDependenciesCollector
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.gradle.utils.property
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
    fun `npm dependencies declared in the kotlin DSL are collected`() {
        val project = buildProjectWithMPP {
            kotlin {
                js { nodejs() }

                sourceSets.commonMain {
                    dependencies {
                        npmDev("webpack", "5.0.0")
                        npmOptional("is-odd", "3.0.0")
                        npmPeer("react", "18.0.0")
                    }
                }
            }
        }.evaluate()

        assertEquals(
            listOf(
                "DEV webpack:5.0.0",
                "OPTIONAL is-odd:3.0.0",
                "PEER react:18.0.0",
            ),
            project.collectedNpmDependencies(),
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
            project.collectedNpmDependencies(KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME),
        )
        assertEquals(
            listOf("PEER react:18.0.0"),
            project.collectedNpmDependencies("jsMain"),
        )
    }

    @Test
    fun `npm dependencies declared in several dependencies blocks of one source set are collected`() {
        val project = buildProjectWithMPP {
            kotlin {
                js { nodejs() }

                sourceSets.commonMain {
                    dependencies {
                        npmDev("webpack", "5.0.0")
                    }
                    dependencies {
                        npmPeer("react", "18.0.0")
                    }
                }
            }
        }.evaluate()

        assertEquals(
            listOf(
                "DEV webpack:5.0.0",
                "PEER react:18.0.0",
            ),
            project.collectedNpmDependencies(),
        )
    }

    @Test
    fun `npm dependencies declared with providers are resolved lazily`() {
        lateinit var version: Property<String>

        val project = buildProjectWithMPP {
            version = project.objects.property<String>()

            kotlin {
                js { nodejs() }

                sourceSets.commonMain {
                    dependencies {
                        npmDev(project.provider { "webpack" }, version)
                    }
                }
            }
        }.evaluate()

        // the version is set only after the project is evaluated,
        // so an eager declaration would have failed or captured no value
        version.set("5.0.0")

        assertEquals(
            listOf("DEV webpack:5.0.0"),
            project.collectedNpmDependencies(),
        )
    }

    @Test
    fun `npm dependency declared with a provider without a value fails`() {
        val project = buildProjectWithMPP {
            kotlin {
                js { nodejs() }

                sourceSets.commonMain {
                    dependencies {
                        npmDev(project.provider { "webpack" }, project.objects.property<String>())
                    }
                }
            }
        }.evaluate()

        assertFailsWith<IllegalStateException> {
            project.collectedNpmDependencies()
        }
    }

    @Test
    fun `npm dependencies declared in the kotlin DSL are not added to a configuration`() {
        val project = buildProjectWithMPP {
            kotlin {
                js { nodejs() }

                sourceSets.commonMain {
                    dependencies {
                        npmDev("webpack", "5.0.0")
                    }
                }
            }
        }.evaluate()

        val commonMain = project.multiplatformExtension.sourceSets
            .getByName(KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME)

        assertEquals(
            emptyList(),
            project.configurations
                .getByName(commonMain.implementationConfigurationName)
                .allDependencies
                .map { it.name }
                .filter { it == "webpack" },
        )
        assertEquals(
            listOf("DEV webpack:5.0.0"),
            project.collectedNpmDependencies(),
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
                        npmDev(project.provider { "is-even" }, project.provider { "3.0.0" })
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
    fun `npm dependencies on directories declared in the kotlin DSL are collected with file versions`(
        @TempDir tempDir: Path,
    ) {
        val isEvenDir = tempDir.resolve("is-even").createDirectories()
        val isOddDir = tempDir.resolve("is-odd").createDirectories()

        val project = buildProjectWithMPP {
            kotlin {
                js { nodejs() }

                sourceSets.commonMain {
                    dependencies {
                        npmDev("is-even", isEvenDir.toFile())
                        npmOptional("is-odd", isOddDir.toFile())
                    }
                }
            }
        }.evaluate()

        assertEquals(
            listOf(
                "DEV is-even:${isEvenDir.fileVersion()}",
                "OPTIONAL is-odd:${isOddDir.fileVersion()}",
            ),
            project.collectedNpmDependencies(),
        )
    }

    @Test
    fun `npm dependencies on directories declared with providers are resolved lazily`(
        @TempDir tempDir: Path,
    ) {
        lateinit var isEvenDir: DirectoryProperty
        lateinit var isOddDir: DirectoryProperty

        val project = buildProjectWithMPP {
            isEvenDir = project.objects.directoryProperty()
            isOddDir = project.objects.directoryProperty()

            kotlin {
                js { nodejs() }

                sourceSets.commonMain {
                    dependencies {
                        npmDev("is-even", isEvenDir)
                        npmOptional("is-odd", isOddDir)
                    }
                }
            }
        }.evaluate()

        // the directories are set only after the project is evaluated,
        // so an eager declaration would have failed or captured no value
        isEvenDir.set(tempDir.resolve("is-even").createDirectories().toFile())
        isOddDir.set(tempDir.resolve("is-odd").createDirectories().toFile())

        assertEquals(
            listOf(
                "DEV is-even:${tempDir.resolve("is-even").fileVersion()}",
                "OPTIONAL is-odd:${tempDir.resolve("is-odd").fileVersion()}",
            ),
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

    @Test
    fun `package json task inputs contain the npm dependencies of all source sets of the compilation`() {
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
            listOf(
                "PEER react:18.0.0",
                "DEV webpack:5.0.0",
            ),
            project.packageJsonTaskDeclaredNpmDependencies(),
        )
    }

    @Test
    fun `package json task inputs do not contain the npm dependencies of other compilations`() {
        val project = buildProjectWithMPP {
            kotlin {
                js { nodejs() }

                sourceSets.jsMain {
                    dependencies {
                        npmDev("webpack", "5.0.0")
                    }
                }
                sourceSets.jsTest {
                    dependencies {
                        npmPeer("react", "18.0.0")
                    }
                }
            }
        }.evaluate()

        assertEquals(
            listOf("DEV webpack:5.0.0"),
            project.packageJsonTaskDeclaredNpmDependencies(KotlinCompilation.MAIN_COMPILATION_NAME),
        )
        assertEquals(
            listOf("PEER react:18.0.0"),
            project.packageJsonTaskDeclaredNpmDependencies(KotlinCompilation.TEST_COMPILATION_NAME),
        )
    }

    @Test
    fun `package json task inputs contain npm dependencies declared with providers`() {
        lateinit var version: Property<String>

        val project = buildProjectWithMPP {
            version = project.objects.property<String>()

            kotlin {
                js { nodejs() }

                sourceSets.commonMain {
                    dependencies {
                        npmDev(project.provider { "webpack" }, version)
                    }
                }
            }
        }.evaluate()

        // the task input must not be resolved before the task runs
        version.set("5.0.0")

        assertEquals(
            listOf("DEV webpack:5.0.0"),
            project.packageJsonTaskDeclaredNpmDependencies(),
        )
    }

    @Suppress("DEPRECATION") // the deprecated npm DSL is tested on purpose
    private fun buildProjectWithNpmDependencies(): Project =
        buildProjectWithMPP {
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
            .internal
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
            .map { it.uniqueRepresentation() }

    private fun KotlinNpmDependency.uniqueRepresentation(): String =
        "$scope $name:$version"

    /** The npm `file:` version notation expected for a dependency on this directory. */
    private fun Path.fileVersion(): String =
        "file:${toAbsolutePath().normalize().absolutePathString()}"
}

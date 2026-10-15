/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.js

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.*
import org.junit.jupiter.api.DisplayName

/**
 * KT-89245: libraries published by KGP must still be imported into the npm module cache.
 * Which names and versions are rejected is covered by `GradleNodeModuleBuilderTest` and `GradleNodeModuleBuilderFunctionalTest`.
 */
@JsGradlePluginTests
@DisplayName("Packages imported into the npm module cache (KT-89245)")
@GradleTestVersions(minVersion = TestVersions.Gradle.MAX_SUPPORTED) // unaffected by Gradle-version, only test the latest version
class NpmImportedPackagesIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899()

    /**
     * Must support 'invalid' npm package names.
     *
     * KGP uses the root project package name as a default package name.
     * This is technically not a valid NPM package - but that's not relevant because the `package.json` bundled into a `.klib`
     * is only used as a format for describing npm dependencies. It doesn't need to be a valid package name.
     */
    @GradleTest
    fun importsPackageNameWithSpace(gradleVersion: GradleVersion) {
        val publishedProject = publishLibraryWithSpaceInNpmName(gradleVersion)

        consumer(gradleVersion, publishedProject) {
            build("jsPackageJson") {
                assertFileExists(projectPath.resolve("build/js/packages_imported/$SPACED_NPM_NAME/$VERSION/package.json"))
            }
        }
    }

    /** A library can set a scoped npm name, `@scope/name`, with `outputModuleName`. It must still be imported. */
    @DisplayName("A published library with a scoped npm name is imported")
    @GradleTest
    fun importsScopedPackageName(gradleVersion: GradleVersion) {
        val publishedProject = publishLibraryWithScopedNpmName(gradleVersion)

        consumer(gradleVersion, publishedProject) {
            build("jsPackageJson") {
                assertFileExists(projectPath.resolve("build/js/packages_imported/$SCOPED_NPM_NAME/$VERSION/package.json"))
            }
        }
    }

    private fun consumer(
        gradleVersion: GradleVersion,
        publishedProject: PublishedProject,
        test: TestProject.() -> Unit,
    ) {
        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            addPublishedProjectToRepositories(publishedProject)

            buildScriptInjection {
                kotlinMultiplatform.apply {
                    js {
                        nodejs()
                    }

                    sourceSets.jsMain.dependencies {
                        implementation(publishedProject.rootCoordinate)
                    }
                }
            }

            test()
        }
    }

    /**
     * Publishes `:lib` of a build whose root project is named `My Project`, so its npm name is `My Project-lib`.
     * The Maven artifactId is `lib`: Gradle rejects a space there.
     */
    private fun publishLibraryWithSpaceInNpmName(gradleVersion: GradleVersion): PublishedProject {
        val publisherConfiguration = PublisherConfiguration(group = "producer", version = VERSION)

        val producer = project("empty", gradleVersion) {
            settingsBuildScriptInjection {
                settings.rootProject.name = SPACED_ROOT_PROJECT_NAME
            }
            addKgpToBuildScriptCompilationClasspath()

            val lib = project("empty", gradleVersion)
            include(lib, LIBRARY_NAME)
            lib.buildScriptInjection {
                project.setupMavenPublication(REPOSITORY_NAME, publisherConfiguration)
                project.applyMultiplatform {
                    js {
                        nodejs()
                    }

                    // A published klib carries its package.json only when it has npm dependencies
                    sourceSets.jsMain.dependencies {
                        implementation(npm("is-odd", "3.0.1"))
                    }
                }
            }

            build(":$LIBRARY_NAME:publishAllPublicationsTo${REPOSITORY_NAME}Repository")
        }

        return PublishedProject(
            producer.projectPath.resolve(LIBRARY_NAME).resolve(publisherConfiguration.repoPath).toFile(),
            publisherConfiguration.group,
            LIBRARY_NAME,
            VERSION,
        )
    }

    /** Publishes a library whose npm name is [SCOPED_NPM_NAME]. */
    private fun publishLibraryWithScopedNpmName(gradleVersion: GradleVersion): PublishedProject =
        project("empty", gradleVersion) {
            settingsBuildScriptInjection {
                settings.rootProject.name = LIBRARY_NAME
            }
            addKgpToBuildScriptCompilationClasspath()
            buildScriptInjection {
                project.applyMultiplatform {
                    js {
                        outputModuleName.set(SCOPED_NPM_NAME)
                        nodejs()
                    }

                    // A published klib carries its package.json only when it has npm dependencies
                    sourceSets.jsMain.dependencies {
                        implementation(npm("is-odd", "3.0.1"))
                    }
                }
            }
        }.publish(publisherConfiguration = PublisherConfiguration(group = "producer", version = VERSION))

    companion object {
        private const val SPACED_ROOT_PROJECT_NAME = "My Project"
        private const val LIBRARY_NAME = "lib"
        private const val SPACED_NPM_NAME = "$SPACED_ROOT_PROJECT_NAME-$LIBRARY_NAME"
        private const val SCOPED_NPM_NAME = "@scope/$LIBRARY_NAME"
        private const val VERSION = "1.0.0"
        private const val REPOSITORY_NAME = "Local"
    }
}

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.mpp

import org.gradle.api.logging.LogLevel.LIFECYCLE
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.test.TestMetadata

@MppGradlePluginTests
class MppIncorrectCompileOnlyDependenciesValidationIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copy(
            logLevel = LIFECYCLE, // less logging, faster tests
            configurationCache = true, // more caching, faster tests
        )

    @GradleTest
    @TestMetadata(value = "mpp-compile-only-dep")
    @GradleTestVersions(minVersion = TypesafeProjectAccessorsMinimumGradleVersion)
    fun `when typesafe project accessor is used as compileOnly dependency, and is correctly exposed as an api dependency, expect no warning`(
        gradleVersion: GradleVersion,
    ) {
        project(
            projectName = "mpp-compile-only-dep",
            gradleVersion = gradleVersion,
        ) {
            enableVersionCatalog()

            subProject("demo-app").apply {
                buildGradleKts.append(
                    """
                    |kotlin {
                    |  sourceSets {
                    |    commonMain.dependencies {
                    |      compileOnly(projects.demoLib)
                    |    }
                    |    jsMain.dependencies {
                    |      api(projects.demoLib)
                    |    }
                    |  }
                    |}
                    """.trimMargin()
                )
            }

            build("help", "--dry-run") {
                output.assertNoDiagnostic(
                    KotlinToolingDiagnostics.IncorrectCompileOnlyDependencyWarning
                )
            }
        }
    }

    @GradleTest
    @TestMetadata(value = "mpp-compile-only-dep")
    @GradleTestVersions(minVersion = VersionCatalogsMinimumGradleVersion)
    fun `when version catalog dependency is used as commonMain compileOnly dependency, and is correctly exposed as an api dependency, expect no warning`(
        gradleVersion: GradleVersion,
    ) {
        project(
            projectName = "mpp-compile-only-dep",
            gradleVersion = gradleVersion,
        ) {
            enableVersionCatalog()

            subProject("demo-app").apply {
                buildGradleKts.append(
                    """
                    |kotlin {
                    |  sourceSets {
                    |    commonMain.dependencies {
                    |      compileOnly(libs.atomicfu)
                    |    }
                    |    jsMain.dependencies {
                    |      api("org.jetbrains.kotlinx:atomicfu:latest.release")
                    |    }
                    |  }
                    |}
                    """.trimMargin()
                )
            }

            build("help", "--dry-run") {
                output.assertNoDiagnostic(
                    KotlinToolingDiagnostics.IncorrectCompileOnlyDependencyWarning
                )
            }
        }
    }

    @GradleTest
    @TestMetadata(value = "mpp-compile-only-dep")
    @GradleTestVersions(minVersion = VersionCatalogsMinimumGradleVersion)
    fun `when commonMain compileOnly dependency is correctly exposed as an api dependency using version catalog dependency, expect no warning`(
        gradleVersion: GradleVersion,
    ) {
        project(
            projectName = "mpp-compile-only-dep",
            gradleVersion = gradleVersion,
        ) {
            enableVersionCatalog()

            subProject("demo-app").apply {
                buildGradleKts.append(
                    """
                    |kotlin {
                    |  sourceSets {
                    |    commonMain.dependencies {
                    |      compileOnly("org.jetbrains.kotlinx:kotlinx-html:latest.release")
                    |    }
                    |    jsMain.dependencies {
                    |      api(libs.kotlinxHtml)
                    |    }
                    |  }
                    |}
                    """.trimMargin()
                )
            }

            build("help", "--dry-run") {
                output.assertNoDiagnostic(
                    KotlinToolingDiagnostics.IncorrectCompileOnlyDependencyWarning
                )
            }
        }
    }

    @GradleTest
    @TestMetadata(value = "mpp-compile-only-dep")
    @GradleTestVersions(minVersion = VersionCatalogsMinimumGradleVersion)
    fun `when compileOnly dependency is external, and api dependency is project with the same coords, expect warning`(
        gradleVersion: GradleVersion,
    ) {
        project(
            projectName = "mpp-compile-only-dep",
            gradleVersion = gradleVersion,
        ) {
            enableVersionCatalog()

            subProject("demo-app").apply {
                buildGradleKts.append(
                    """
                    |kotlin {
                    |  sourceSets {
                    |    commonMain.dependencies {
                    |      api("kgp.it:demo-lib:1.2.3")
                    |    }
                    |    jsMain.dependencies {
                    |      // Add an external dependency with same coords as `projects.demoLib`
                    |      compileOnly(projects.demoLib)
                    |    }
                    |  }
                    |}
                    """.trimMargin()
                )
            }

            build("help", "--dry-run") {
                output.assertHasDiagnostic(
                    KotlinToolingDiagnostics.IncorrectCompileOnlyDependencyWarning
                )
            }
        }
    }

    @Suppress("ConstPropertyName")
    companion object {
        /** The minimum Gradle version required for typesafe project accessors. */
        private const val TypesafeProjectAccessorsMinimumGradleVersion = "7.0"

        /** The minimum Gradle version required for Version Catalogs. */
        private const val VersionCatalogsMinimumGradleVersion = "7.0"

        private fun TestProject.enableVersionCatalog() {
            require(gradleVersion >= VersionCatalogsMinimumGradleVersion) {
                "Version Catalogs can only be enabled in Gradle $VersionCatalogsMinimumGradleVersion or higher," +
                        "but current Gradle version is $gradleVersion."
            }
            if (gradleVersion < "8.0") {
                // VERSION_CATALOGS flag was removed in 8.0, and thereafter enabled by default
                settingsGradleKts.append("""enableFeaturePreview("VERSION_CATALOGS")""")
            }
        }

        private operator fun GradleVersion.compareTo(other: String): Int =
            compareTo(GradleVersion.version(other))
    }
}

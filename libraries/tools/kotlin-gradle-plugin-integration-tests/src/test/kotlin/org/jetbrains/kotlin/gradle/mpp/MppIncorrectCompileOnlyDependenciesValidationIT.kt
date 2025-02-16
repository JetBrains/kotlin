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
            configurationCache = BuildOptions.ConfigurationCacheValue.ENABLED, // more caching, faster tests
        )

    @GradleTest
    @TestMetadata(value = "mpp-compile-only-dep")
    fun `when typesafe project accessor is used as compileOnly dependency, and is correctly exposed as an api dependency, expect no warning`(
        gradleVersion: GradleVersion,
    ) {
        project(
            projectName = "mpp-compile-only-dep",
            gradleVersion = gradleVersion,
        ) {
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
    fun `when version catalog dependency is used as commonMain compileOnly dependency, and is correctly exposed as an api dependency, expect no warning`(
        gradleVersion: GradleVersion,
    ) {
        project(
            projectName = "mpp-compile-only-dep",
            gradleVersion = gradleVersion,
        ) {
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
    fun `when commonMain compileOnly dependency is correctly exposed as an api dependency using version catalog dependency, expect no warning`(
        gradleVersion: GradleVersion,
    ) {
        project(
            projectName = "mpp-compile-only-dep",
            gradleVersion = gradleVersion,
        ) {
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
    fun `when compileOnly dependency is external, and api dependency is project with the same coords, expect warning`(
        gradleVersion: GradleVersion,
    ) {
        project(
            projectName = "mpp-compile-only-dep",
            gradleVersion = gradleVersion,
        ) {
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
}

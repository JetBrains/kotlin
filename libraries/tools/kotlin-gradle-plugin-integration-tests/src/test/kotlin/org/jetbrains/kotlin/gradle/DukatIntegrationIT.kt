/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import com.google.gson.Gson
import org.gradle.api.logging.configuration.WarningMode
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.targets.js.dukat.ExternalsOutputFormat
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJson
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.readText

@DisplayName("Dukat standalone")
@JsGradlePluginTests
class DukatIntegrationStandaloneIT : KGPBaseTest() {

    @DisplayName("Kotlin DSL and root dependencies")
    @GradleTest
    fun testSeparateDukatKotlinDslRootDependencies(gradleVersion: GradleVersion) {
        testSeparateDukat(
            gradleVersion,
            DslType.KOTLIN,
            DependenciesLocation.ROOT
        )
    }

    @DisplayName("Kotlin DSL and external dependencies")
    @GradleTest
    fun testSeparateDukatKotlinDslExtDependencies(gradleVersion: GradleVersion) {
        testSeparateDukat(
            gradleVersion,
            DslType.KOTLIN,
            DependenciesLocation.EXTENSION
        )
    }

    @DisplayName("Groovy DSL and root dependencies")
    @GradleTest
    fun testSeparateDukatGroovyDslRootDependencies(gradleVersion: GradleVersion) {
        testSeparateDukat(
            gradleVersion,
            DslType.GROOVY,
            DependenciesLocation.ROOT
        )
    }

    @DisplayName("Groovy DSL and external dependencies")
    @GradleTest
    fun testSeparateDukatGroovyDslExtDependencies(gradleVersion: GradleVersion) {
        testSeparateDukat(
            gradleVersion,
            DslType.GROOVY,
            DependenciesLocation.EXTENSION
        )
    }

    private fun testSeparateDukat(
        gradleVersion: GradleVersion,
        dslType: DslType,
        dependenciesLocation: DependenciesLocation
    ) {
        project(
            projectName = projectName(dslType, dependenciesLocation, PROJECTS_PREFIX),
            gradleVersion
        ) {
            build("generateExternals")
        }
    }
}

@DisplayName("Dukat integrated")
@JsGradlePluginTests
class DukatIntegratedIntegrationIT : KGPBaseTest() {

    @DisplayName("Kotlin DSL and root dependencies")
    @GradleTest
    fun testIntegratedDukatKotlinDslRootDependencies(gradleVersion: GradleVersion) {
        testIntegratedDukat(
            gradleVersion,
            DslType.KOTLIN,
            DependenciesLocation.ROOT
        )
    }

    @DisplayName("Kotlin DSL and external dependencies")
    @GradleTest
    fun testIntegratedDukatKotlinDslExtDependencies(gradleVersion: GradleVersion) {
        testIntegratedDukat(
            gradleVersion,
            DslType.KOTLIN,
            DependenciesLocation.EXTENSION
        )
    }

    @DisplayName("Groovy DSL and root dependencies")
    @GradleTest
    fun testIntegratedDukatGroovyDslRootDependencies(gradleVersion: GradleVersion) {
        testIntegratedDukat(
            gradleVersion,
            DslType.GROOVY,
            DependenciesLocation.ROOT
        )
    }

    @DisplayName("Groovy DSL and external dependencies")
    @GradleTest
    fun testIntegratedDukatGroovyDslExtDependencies(gradleVersion: GradleVersion) {
        testIntegratedDukat(
            gradleVersion,
            DslType.GROOVY,
            DependenciesLocation.EXTENSION
        )
    }

    private fun testIntegratedDukat(
        gradleVersion: GradleVersion,
        dslType: DslType,
        dependenciesLocation: DependenciesLocation
    ) {
        project(
            projectName = projectName(dslType, dependenciesLocation, PROJECTS_PREFIX),
            gradleVersion
        ) {
            gradleProperties.modify {
                "${ExternalsOutputFormat.externalsOutputFormatProperty}=${ExternalsOutputFormat.SOURCE}"
            }

            build("compileKotlinJs") {
                assertFileInProjectExists("build/externals/${projectName(dslType, dependenciesLocation)}/src/index.module_decamelize.kt")
            }
        }
    }

    @DisplayName("Kotlin DSL: change in root dependencies")
    @GradleTest
    fun testIntegratedDukatWithChangeKotlinDslRootDependencies(gradleVersion: GradleVersion) {
        testIntegratedDukatWithChange(
            gradleVersion,
            DslType.KOTLIN,
            DependenciesLocation.ROOT
        )
    }

    @DisplayName("Kotlin DSL: change in external dependencies")
    @GradleTest
    fun testIntegratedDukatWithChangeKotlinDslExtDependencies(gradleVersion: GradleVersion) {
        testIntegratedDukatWithChange(
            gradleVersion,
            DslType.KOTLIN,
            DependenciesLocation.EXTENSION
        )
    }

    @DisplayName("Groovy DSL: change in root dependencies")
    @GradleTest
    fun testIntegratedDukatWithChangeGroovyDslRootDependencies(gradleVersion: GradleVersion) {
        testIntegratedDukatWithChange(
            gradleVersion,
            DslType.GROOVY,
            DependenciesLocation.ROOT
        )
    }

    @DisplayName("Groovy DSL: change in external dependencies")
    @GradleTest
    fun testIntegratedDukatWithChangeGroovyDslExtDependencies(gradleVersion: GradleVersion) {
        testIntegratedDukatWithChange(
            gradleVersion,
            DslType.GROOVY,
            DependenciesLocation.EXTENSION
        )
    }

    private fun testIntegratedDukatWithChange(
        gradleVersion: GradleVersion,
        dslType: DslType,
        dependenciesLocation: DependenciesLocation
    ) {
        project(
            projectName = projectName(dslType, dependenciesLocation, PROJECTS_PREFIX),
            gradleVersion
        ) {
            gradleProperties.modify {
                "${ExternalsOutputFormat.externalsOutputFormatProperty}=${ExternalsOutputFormat.SOURCE}"
            }

            val externalSrcs = "build/externals/${projectName(dslType, dependenciesLocation)}/src"
            build("compileKotlinJs") {
                assertFileInProjectExists("$externalSrcs/index.module_decamelize.kt")
            }

            buildFileForDslType(dslType).modify { buildScript ->
                buildScript
                    .replace(
                        """implementation(npm("left-pad", "1.3.0"))""",
                        """implementation(npm("left-pad", "1.3.0", true))"""
                    )
                    .replace(
                        """implementation(npm("decamelize", "4.0.0", true))""",
                        """implementation(npm("decamelize", "4.0.0"))"""
                    )
            }

            build("generateExternalsIntegrated") {
                assertFileInProjectExists("$externalSrcs/index.module_left-pad.kt")
            }
        }
    }

    @DisplayName("Kotlin DSL: with false default root dependencies")
    @GradleTest
    fun testIntegratedDukatWithFalseDefaultKotlinDslRootDependencies(gradleVersion: GradleVersion) {
        testIntegratedDukatWithFalseDefault(
            gradleVersion,
            DslType.KOTLIN,
            DependenciesLocation.ROOT
        )
    }

    @DisplayName("Kotlin DSL: with false external dependencies")
    @GradleTest
    fun testIntegratedDukatWithFalseDefaultKotlinDslExtDependencies(gradleVersion: GradleVersion) {
        testIntegratedDukatWithFalseDefault(
            gradleVersion,
            DslType.KOTLIN,
            DependenciesLocation.EXTENSION
        )
    }

    @DisplayName("Groovy DSL: with false root dependencies")
    @GradleTest
    fun testIntegratedDukatWithFalseDefaultGroovyDslRootDependencies(gradleVersion: GradleVersion) {
        testIntegratedDukatWithFalseDefault(
            gradleVersion,
            DslType.GROOVY,
            DependenciesLocation.ROOT
        )
    }

    @DisplayName("Groovy DSL: with false external dependencies")
    @GradleTest
    fun testIntegratedDukatWithFalseDefaultGroovyDslExtDependencies(gradleVersion: GradleVersion) {
        testIntegratedDukatWithFalseDefault(
            gradleVersion,
            DslType.GROOVY,
            DependenciesLocation.EXTENSION
        )
    }

    private fun testIntegratedDukatWithFalseDefault(
        gradleVersion: GradleVersion,
        dslType: DslType,
        dependenciesLocation: DependenciesLocation
    ) {
        project(
            projectName = projectName(dslType, dependenciesLocation, PROJECTS_PREFIX),
            gradleVersion
        ) {
            gradleProperties.modify {
                """
                kotlin.js.generate.externals=true
                ${ExternalsOutputFormat.externalsOutputFormatProperty}=${ExternalsOutputFormat.SOURCE}
            """.trimIndent()
            }

            buildFileForDslType(dslType).modify { buildScript ->
                buildScript
                    .replace(
                        """implementation(npm("decamelize", "4.0.0", true))""",
                        """implementation(npm("decamelize", "4.0.0", false))"""
                    )
            }

            val externalSrcs = "build/externals/${projectName(dslType, dependenciesLocation)}/src"
            build("generateExternalsIntegrated") {
                assertFileInProjectExists("$externalSrcs/index.module_left-pad.kt")
            }
        }
    }
}

@DisplayName("Dukat in js BOTH mode")
@JsGradlePluginTests
class DukatIntegrationBothModeIT : KGPBaseTest() {

    @DisplayName("Dependencies are generated only once")
    @GradleTest
    fun testBothOnlyOnceGenerateDependencies(gradleVersion: GradleVersion) {
        project("dukat-integration/both", gradleVersion) {
            gradleProperties.modify {
                "${ExternalsOutputFormat.externalsOutputFormatProperty}=${ExternalsOutputFormat.SOURCE}"
            }

            val externalSrcs = "build/externals/both-js-ir/src"
            build("compileKotlinJsLegacy") {
                assertTasksExecuted(":irGenerateExternalsIntegrated")
                assertFileInProjectExists("$externalSrcs/index.module_decamelize.kt")

                val legacyExternals = projectPath.resolve("build/externals/both-js-legacy/src")
                assertFileNotExists(legacyExternals)
            }

            build(
                "compileKotlinJsLegacy",
                "--rerun-tasks"
            ) {
                assertTasksExecuted(":irGenerateExternalsIntegrated")
                assertFileInProjectExists("$externalSrcs/index.module_decamelize.kt")
            }
        }
    }

    @DisplayName("Groovy DSL: compilation")
    @GradleTest
    fun testCompilationLegacyBinariesGroovyDsl(gradleVersion: GradleVersion) {
        testCompilationLegacyBinaries(
            gradleVersion,
            DslType.GROOVY
        )
    }

    @DisplayName("Kotlin DSL: compilation")
    @GradleTest
    fun testCompilationLegacyBinariesKotlinDsl(gradleVersion: GradleVersion) {
        testCompilationLegacyBinaries(
            gradleVersion,
            DslType.KOTLIN
        )
    }

    private fun testCompilationLegacyBinaries(
        gradleVersion: GradleVersion,
        dslType: DslType
    ) {
        project(
            projectName(dslType, DependenciesLocation.EXTENSION, PROJECTS_PREFIX),
            gradleVersion
        ) {
            build("compileKotlinJs")
        }
    }

    @DisplayName("Assemble both binaries")
    @GradleTest
    fun testAssembleBothBinaries(gradleVersion: GradleVersion) {
        project("${PROJECTS_PREFIX}/both", gradleVersion) {
            // Warning mode 'Summary' because
            // > Execution optimizations have been disabled for task ':kotlinSourcesJar' to ensure correctness due to the following reasons
            build(
                "assemble",
                buildOptions = defaultBuildOptions.copy(warningMode = WarningMode.Summary)
            )
        }
    }

    @DisplayName("Compile both binaries in legacy mode")
    @GradleTest
    fun testCompileLegacyBothBinaries(gradleVersion: GradleVersion) {
        project("$PROJECTS_PREFIX/both", gradleVersion) {
            gradleProperties.modify {
                """
                ${ExternalsOutputFormat.externalsOutputFormatProperty}=${ExternalsOutputFormat.BINARY}
            """.trimIndent()
            }

            val externalSrcs = "build/externals/both-js-legacy/src"
            build("compileKotlinJsLegacy") {
                assertFileInProjectExists("$externalSrcs/index.d.jar")

                val irExternals = "build/externals/both-js-ir/src"
                assertFileInProjectNotExists(irExternals)
            }
        }
    }

    @DisplayName("Assemble both sources")
    @GradleTest
    fun testAssembleBothSource(gradleVersion: GradleVersion) {
        project("$PROJECTS_PREFIX/both", gradleVersion) {
            gradleProperties.modify {
                """
                ${ExternalsOutputFormat.externalsOutputFormatProperty}=${ExternalsOutputFormat.SOURCE}
            """.trimIndent()
            }

            val externalSrcs = "build/externals/both-js-ir/src"
            build("assemble", buildOptions = defaultBuildOptions.copy(warningMode = WarningMode.Summary)) {
                assertTasksExecuted(":irGenerateExternalsIntegrated")

                assertFileInProjectExists("$externalSrcs/index.module_decamelize.kt")
                val legacyExternals = "build/externals/both-js-legacy/src"
                assertFileInProjectNotExists(legacyExternals)
            }
        }
    }

    @DisplayName("Without generating externals")
    @GradleTest
    fun testWithoutGenerateExternals(gradleVersion: GradleVersion) {
        project("$PROJECTS_PREFIX/without-generate-externals", gradleVersion) {
            build("assemble", buildOptions = defaultBuildOptions.copy(warningMode = WarningMode.Summary)) {
                assertTasksSkipped(":generateExternalsIntegrated")

                assert(
                    !projectPath
                        .resolve("build/js/packages/without-generate-externals")
                        .resolve(NpmProject.PACKAGE_JSON)
                        .let {
                            Gson().fromJson(it.readText(), PackageJson::class.java)
                        }
                        .dependencies
                        .containsKey("dukat")
                ) {
                    "Dukat don't need to be dependency for thus project"
                }
            }
        }
    }
}

private fun projectName(
    dslType: DslType,
    dependenciesLocation: DependenciesLocation,
    directoryPrefix: String = ""
): String =
    "$directoryPrefix/${dslType.value}-${dependenciesLocation.value}"

private enum class DslType(
    val value: String
) {
    KOTLIN("kotlin-dsl"),
    GROOVY("groovy-dsl")
}

private enum class DependenciesLocation(
    val value: String
) {
    ROOT("root"),
    EXTENSION("ext")
}

private fun TestProject.buildFileForDslType(dslType: DslType) =
    when (dslType) {
        DslType.KOTLIN -> buildGradleKts
        DslType.GROOVY -> buildGradle
    }

private const val PROJECTS_PREFIX = "dukat-integration"
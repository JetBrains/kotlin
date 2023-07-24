/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.testbase.*
import java.util.UUID

@MppGradlePluginTests
class TargetPresetDeprecationIT : KGPBaseTest() {

    private enum class ProjectSetup(
        val path: String,
        val projectFile: String,
    ) {
        Groovy("groovy", "build.gradle"),
        Kotlin("kotlin", "build.gradle.kts"),
    }

    private enum class API {
        DeprecatedTargetFromPreset,
        DeprecatedFromPreset,
        DeprecatedCreateTarget,
        Regular,
    }

    private fun apiToUse(
        setup: ProjectSetup,
        api: API,
    ): String {
        return when (setup) {
            ProjectSetup.Groovy -> when (api) {
                API.DeprecatedTargetFromPreset -> """
                    targetFromPreset(presets.getByName("jvm"), "jvm")
                """.trimIndent()
                API.DeprecatedFromPreset -> """
                    targets {
                        fromPreset(presets.getByName("jvm"), "jvm")
                    }
                """.trimIndent()
                API.DeprecatedCreateTarget -> """
                    targets.add(presets.getByName("jvm").createTarget("jvm"))
                """.trimIndent()
                API.Regular -> "jvm()"
            }

            ProjectSetup.Kotlin -> when (api) {
                API.DeprecatedTargetFromPreset -> """
                    targetFromPreset(presets.getByName("jvm"), "jvm")
                """.trimIndent()
                API.DeprecatedFromPreset -> """
                    targets {
                        fromPreset(presets.getByName("jvm"), "jvm")
                    }
                """.trimIndent()
                API.DeprecatedCreateTarget -> """
                    targets.add(presets.getByName("jvm").createTarget("jvm"))
                """.trimIndent()
                API.Regular -> "jvm()"
            }
        }
    }

    private fun BuildResult.assertProjectFileBuildDiagnostics(
        setup: ProjectSetup,
        api: API,
    ) {
        return when (setup) {
            ProjectSetup.Groovy -> {
                // No build diagnostics in Groovy
            }

            ProjectSetup.Kotlin -> when (api) {
                API.DeprecatedFromPreset -> {
                    assertOutputContainsSequence(
                        listOf(
                            "targetPresetsDeprecation/kotlin/build.gradle.kts:10:5: 'fromPreset(KotlinTargetPreset<T>, String): T' is deprecated. The fromPreset() API is deprecated and will be removed in future releases. Learn how to configure targets at: https://kotl.in/target-configuration",
                            "targetPresetsDeprecation/kotlin/build.gradle.kts:10:5: The presets API is deprecated and will be removed in future releases. Learn how to configure targets at: https://kotl.in/target-configuration",
                            "targetPresetsDeprecation/kotlin/build.gradle.kts:10:16: 'presets: NamedDomainObjectCollection<KotlinTargetPreset<*>>' is deprecated. The presets API is deprecated and will be removed in future releases. Learn how to configure targets at: https://kotl.in/target-configuration",
                            "targetPresetsDeprecation/kotlin/build.gradle.kts:10:16: 'presets: NamedDomainObjectCollection<KotlinTargetPreset<*>>' is deprecated. The presets API is deprecated and will be removed in future releases. Learn how to configure targets at: https://kotl.in/target-configuration",
                        )
                    )
                }

                API.DeprecatedTargetFromPreset -> {
                    assertOutputContainsSequence(
                        listOf(
                            "targetPresetsDeprecation/kotlin/build.gradle.kts:9:5: 'targetFromPreset(KotlinTargetPreset<T>, String): T' is deprecated. The targetFromPreset() API is deprecated and will be removed in future releases. Learn how to configure targets at: https://kotl.in/target-configuration",
                            "targetPresetsDeprecation/kotlin/build.gradle.kts:9:5: The presets API is deprecated and will be removed in future releases. Learn how to configure targets at: https://kotl.in/target-configuration",
                            "targetPresetsDeprecation/kotlin/build.gradle.kts:9:22: 'presets: NamedDomainObjectCollection<KotlinTargetPreset<*>>' is deprecated. The presets API is deprecated and will be removed in future releases. Learn how to configure targets at: https://kotl.in/target-configuration",
                            "targetPresetsDeprecation/kotlin/build.gradle.kts:9:22: 'presets: NamedDomainObjectCollection<KotlinTargetPreset<*>>' is deprecated. The presets API is deprecated and will be removed in future releases. Learn how to configure targets at: https://kotl.in/target-configuration",
                        )
                    )
                }

                API.DeprecatedCreateTarget -> {
                    assertOutputContainsSequence(
                        listOf(
                            "targetPresetsDeprecation/kotlin/build.gradle.kts:9:17: 'presets: NamedDomainObjectCollection<KotlinTargetPreset<*>>' is deprecated. The presets API is deprecated and will be removed in future releases. Learn how to configure targets at: https://kotl.in/target-configuration",
                            "targetPresetsDeprecation/kotlin/build.gradle.kts:9:17: 'presets: NamedDomainObjectCollection<KotlinTargetPreset<*>>' is deprecated. The presets API is deprecated and will be removed in future releases. Learn how to configure targets at: https://kotl.in/target-configuration",
                            "targetPresetsDeprecation/kotlin/build.gradle.kts:9:42: 'createTarget(String): T' is deprecated. The presets API is deprecated and will be removed in future releases. Learn how to configure targets at: https://kotl.in/target-configuration",
                            "targetPresetsDeprecation/kotlin/build.gradle.kts:9:42: The presets API is deprecated and will be removed in future releases. Learn how to configure targets at: https://kotl.in/target-configuration",
                        )
                    )
                }

                API.Regular -> {
                    assertNoBuildWarnings()
                }
            }
        }
    }

    private fun expectedVerboseDiagnostics(api: API): List<String> {
        return when (api) {
            API.DeprecatedTargetFromPreset -> listOf(
                KotlinToolingDiagnostics.TargetPresets.TARGET_FROM_PRESET_DEPRECATION_MESSAGE
            )
            API.DeprecatedFromPreset -> listOf(
                KotlinToolingDiagnostics.TargetPresets.FROM_PRESET_DEPRECATION_MESSAGE
            )
            API.DeprecatedCreateTarget -> listOf(
                KotlinToolingDiagnostics.TargetPresets.CREATE_TARGET_DEPRECATION_MESSAGE
            )
            API.Regular -> emptyList()
        }
    }

    @GradleTest
    fun test(gradleVersion: GradleVersion) {
        val setups = ProjectSetup.values()
        val apis = API.values()

        for (setup in setups) {
            for (api in apis) {
                build(
                    gradleVersion,
                    setup,
                    apiToUse(setup, api),
                ) {
                    assertVerboseDiagnosticsEqual(
                        KotlinToolingDiagnostics.TargetPresets,
                        expectedVerboseDiagnostics(api),
                    )
                    assertProjectFileBuildDiagnostics(setup, api)
                }
            }
        }
    }

    private fun build(
        gradleVersion: GradleVersion,
        setup: ProjectSetup,
        apiToUse: String,
        assertions: BuildResult.() -> Unit,
    ) {
        project("targetPresetsDeprecation/${setup.path}", gradleVersion) {
            val projectFile = projectPath.resolve(setup.projectFile)
            projectFile.modify { it.replace("<api>", apiToUse) }
            // Force Gradle to rebuild the project file to produce diagnostics
            projectFile.append("println(\"${UUID.randomUUID()}\")")
            build("assemble") {
                assertions()
            }
        }
    }
}
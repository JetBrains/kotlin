/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnosticFactory
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.main
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import java.io.File
import kotlin.test.Test

class DumpAllDiagnosticsTest {

    @Test
    fun `dump all diagnostics test`() {

        val project = buildProjectWithMPP()
        val kotlin = project.multiplatformExtension

        val target = kotlin.jvm()
        val main = target.compilations.main
        val sourceSet = kotlin.sourceSets.first()

        val factories = getAllDiagnostics()
        val diagnostics: List<ToolingDiagnostic> = factories.mapNotNull { factory ->
            try {

                handleSimpleFactory(factory)?.let { return@mapNotNull it }

                when (factory) {
                    // No parameters
                    is KotlinToolingDiagnostics.AndroidGradlePluginIsMissing -> factory.invoke()
                    is KotlinToolingDiagnostics.KotlinCompilationSourceDeprecation -> factory.invoke(null)

                    // List<String> parameter
                    is KotlinToolingDiagnostics.PreHMPPFlagsError ->
                        factory.invoke(listOf("deprecated-flag-1", "deprecated-flag-2"))
                    is KotlinToolingDiagnostics.DeprecatedKotlinNativeTargetsDiagnostic ->
                        factory.invoke(listOf("target1", "target2"))

                    // String parameter
                    is KotlinToolingDiagnostics.CommonMainOrTestWithDependsOnDiagnostic ->
                        factory.invoke("Main")
                    is KotlinToolingDiagnostics.MissingRuntimeDependencyConfigurationForWasmTarget ->
                        factory.invoke("wasmTarget")
                    is KotlinToolingDiagnostics.MissingResourcesConfigurationForTarget ->
                        factory.invoke("targetName")
                    is KotlinToolingDiagnostics.ResourcePublishedMoreThanOncePerTarget ->
                        factory.invoke("targetName")
                    is KotlinToolingDiagnostics.ResourceMayNotBePublishedForTarget ->
                        factory.invoke("targetName")
                    is KotlinToolingDiagnostics.ResourceMayNotBeResolvedForTarget ->
                        factory.invoke("targetName")
                    is KotlinToolingDiagnostics.UnrecognizedKotlinNativeDistributionType ->
                        factory.invoke("distributionType")
                    is KotlinToolingDiagnostics.KMPJavaPluginsIncompatibilityDiagnostic ->
                        factory.invoke("pluginId")
                    is KotlinToolingDiagnostics.WasmSourceSetsNotFoundError ->
                        factory.invoke("sourceSetName")
                    is KotlinToolingDiagnostics.DeprecatedGradleProperties ->
                        factory.invoke("deprecated.property.use = true")
                    is KotlinToolingDiagnostics.JvmWithJavaIsIncompatibleWithAndroid ->
                        factory.invoke("com.android.plugin", null)
                    is KotlinToolingDiagnostics.NotCompatibleWithGradle9 ->
                        factory.invoke("Fix it")
                    is KotlinToolingDiagnostics.PreHmppDependenciesUsedInBuild ->
                        factory.invoke("DependencyName")
                    is KotlinToolingDiagnostics.UnknownAppleFrameworkBuildType ->
                        factory.invoke("ENV_CONFIG")
                    is KotlinToolingDiagnostics.UnknownValueProvidedForResourcesStrategy ->
                        factory.invoke("unknown_value")

                    // String, String parameters
                    is KotlinToolingDiagnostics.DeprecatedPropertyWithReplacement ->
                        factory.invoke("oldProperty", "newProperty")
                    is KotlinToolingDiagnostics.KotlinJvmMainRunTaskConflict ->
                        factory.invoke("targetName", "taskName")
                    is KotlinToolingDiagnostics.XcodeVersionTooHighWarning ->
                        factory.invoke("15.0", "14.0")
                    is KotlinToolingDiagnostics.BuildToolsApiVersionInconsistency ->
                        factory.invoke("2.0", "1.0")
                    is KotlinToolingDiagnostics.DisabledCinteropsCommonizationInHmppProject ->
                        factory.invoke("sourceSetName", "cinteropName")
                    is KotlinToolingDiagnostics.ExperimentalFeatureWarning ->
                        factory.invoke("feature", "https://google.com")
                    is KotlinToolingDiagnostics.KotlinSourceSetDependsOnDefaultCompilationSourceSet ->
                        factory.invoke("dependeeName", "dependencyName")
                    is KotlinToolingDiagnostics.KotlinSourceSetTreeDependsOnMismatch ->
                        factory.invoke("dependeeName", "dependencyName")
                    is KotlinToolingDiagnostics.NoKotlinTargetsDeclared ->
                        factory.invoke(project.name, project.path)
                    is KotlinToolingDiagnostics.ResourceMayNotBeResolvedWithGradleVersion ->
                        factory.invoke("targetName", "9", "10")
                    is KotlinToolingDiagnostics.SourceSetLayoutV1StyleDirUsageWarning ->
                        factory.invoke("sourceDir1", "layoutName", "sourceDir2")
                    is KotlinToolingDiagnostics.XCFrameworkDifferentInnerFrameworksName ->
                        factory.invoke("xcframework_1", "xcframework_2")

                    // GradleVersion parameters
                    is KotlinToolingDiagnostics.IncompatibleGradleVersionTooLowFatalError ->
                        factory.invoke(GradleVersion.version("7.0"), GradleVersion.version("8.0"))

                    // String parameters for Android
                    is KotlinToolingDiagnostics.AgpRequirementNotMetForAndroidSourceSetLayoutV2 ->
                        factory.invoke("8.0", "7.0")
                    is KotlinToolingDiagnostics.IncompatibleAgpVersionTooLowFatalError ->
                        factory.invoke("7.0", "8.0")
                    is KotlinToolingDiagnostics.FailedToGetAgpVersionWarning ->
                        factory.invoke("com.android.application")
                    is KotlinToolingDiagnostics.AndroidSourceSetLayoutV1SourceSetsNotFoundError ->
                        factory.invoke("androidTest")
                    is KotlinToolingDiagnostics.AndroidTargetIsMissing ->
                        factory.invoke("projectName", "projectPath", "com.android.application")
                    is KotlinToolingDiagnostics.AndroidPublicationNotConfigured ->
                        factory.invoke("componentName", "publicationName")
                    is KotlinToolingDiagnostics.AndroidStyleSourceDirUsageWarning ->
                        factory.invoke("sourceDir1", "sourceDir2")

                    // File or nullable parameters
                    is KotlinToolingDiagnostics.DependencyDoesNotPhysicallyExist ->
                        factory.invoke(File("sample.jar"))
                    is KotlinToolingDiagnostics.NativeStdlibIsMissingDiagnostic ->
                        factory.invoke("kotlin.native.home")
                    is KotlinToolingDiagnostics.BrokenKotlinNativeBundleError ->
                        factory.invoke("/path/to/kotlin/native", "kotlin.native.home")
                    is KotlinToolingDiagnostics.KonanHomeConflictDeclaration ->
                        factory.invoke(File("konan/home"), "kotlin.native.home")

                    // Version parameters
                    is KotlinToolingDiagnostics.OldNativeVersionDiagnostic ->
                        factory.invoke(
                            KotlinToolingVersion(1, 9, 0, "dev"),
                            KotlinToolingVersion(1, 8, 0, "dev")
                        )
                    is KotlinToolingDiagnostics.NewNativeVersionDiagnostic ->
                        factory.invoke(
                            KotlinToolingVersion(1, 9, 0, "dev"),
                            KotlinToolingVersion(1, 8, 0, "dev")
                        )

                    // Boolean parameters
                    is KotlinToolingDiagnostics.XcodeUserScriptSandboxingDiagnostic ->
                        factory.invoke(true)

                    // Collection parameters
                    is KotlinToolingDiagnostics.DisabledKotlinNativeTargets ->
                        factory.invoke(listOf("target1", "target2"))
                    is KotlinToolingDiagnostics.CircularDependsOnEdges ->
                        factory.invoke(listOf("sourceSet1", "sourceSet2"))
                    is KotlinToolingDiagnostics.UnusedSourceSetsWarning ->
                        factory.invoke(listOf("sourceSet1", "sourceSet2"))
                    is KotlinToolingDiagnostics.InternalKotlinGradlePluginPropertiesUsed ->
                        factory.invoke(listOf("some.internal.property1", "some.internal.property2"))

                    // Map parameters
                    is KotlinToolingDiagnostics.DuplicateSourceSetsError ->
                        factory.invoke(mapOf("group1" to listOf("source1", "source2")))

                    // SourceSet parameters
                    is KotlinToolingDiagnostics.AndroidMainSourceSetConventionUsedWithoutAndroidTarget ->
                        factory.invoke(sourceSet)
                    is KotlinToolingDiagnostics.IosSourceSetConventionUsedWithoutIosTarget ->
                        factory.invoke(sourceSet)
                    is KotlinToolingDiagnostics.KotlinDefaultHierarchyFallbackDependsOnUsageDetected ->
                        factory.invoke(project, listOf(sourceSet))
                    is KotlinToolingDiagnostics.PlatformSourceSetConventionUsedWithCustomTargetName ->
                        factory.invoke(sourceSet, target, "customTargetName")
                    is KotlinToolingDiagnostics.PlatformSourceSetConventionUsedWithoutCorrespondingTarget ->
                        factory.invoke(sourceSet, "customTargetName")

                    // Complex object parameters
                    is KotlinToolingDiagnostics.IncorrectCompileOnlyDependencyWarning ->
                        factory.invoke(
                            listOf(
                                KotlinToolingDiagnostics.IncorrectCompileOnlyDependencyWarning.CompilationDependenciesPair(
                                    main,
                                    listOf("com.example.dep1", "com.example.dep2")
                                )
                            )
                        )

                    is KotlinToolingDiagnostics.MultipleSourceSetRootsInCompilation ->
                        factory.invoke(main, "unexpectedSourceRoot", "expectedRoot")

                    is KotlinToolingDiagnostics.InconsistentTargetCompatibilityForKotlinAndJavaTasks ->
                        factory.invoke(
                            "SomeJavaTask",
                            "iosTarget",
                            "SomeKotlinTask",
                            "jvmTarget",
                            ToolingDiagnostic.Severity.WARNING
                        )

                    is KotlinToolingDiagnostics.KotlinDefaultHierarchyFallbackIllegalTargetNames ->
                        factory.invoke(project, listOf("someTargetName1", "someTargetName2"))

                    is KotlinToolingDiagnostics.RedundantDependsOnEdgesFound ->
                        factory.invoke(
                            listOf(
                                KotlinToolingDiagnostics.RedundantDependsOnEdgesFound.RedundantEdge(
                                    "sourceSet1",
                                    "sourceSet2"
                                ),
                                KotlinToolingDiagnostics.RedundantDependsOnEdgesFound.RedundantEdge(
                                    "sourceSet3",
                                    "sourceSet4"
                                )
                            )
                        )

                    else -> {
                        assert(false) {
                            "Unsupported factory type: ${factory::class.simpleName}"
                        }
                        null
                    }
                }
            } catch (e: Exception) {
                assert(false) {
                    "Error creating diagnostic from factory ${factory::class.simpleName}: ${e.message}"
                }
                null
            }
        }

        writeToFile(diagnostics, File("build/diagnostics.txt"))
    }

    private fun getAllDiagnostics(): List<ToolingDiagnosticFactory> {
        return KotlinToolingDiagnostics::class.nestedClasses
            .filter { it.objectInstance != null && it.supertypes.any { type -> type.toString().contains("ToolingDiagnosticFactory") } }
            .mapNotNull { it.objectInstance as? ToolingDiagnosticFactory }
    }

    private fun handleSimpleFactory(factory: ToolingDiagnosticFactory): ToolingDiagnostic? {
        // Get all invoke methods
        val invokeMethods = factory::class.members
            .filter { it.name == "invoke" }

        // Handle no-parameter invoke
        val noParamInvoke = invokeMethods.firstOrNull { it.parameters.size == 1 }
        if (noParamInvoke != null) {
            return noParamInvoke.call(factory) as? ToolingDiagnostic
        }

        return null
    }

    private fun writeToFile(diagnostics: List<ToolingDiagnostic>, file: File, separator: String = "=".repeat(80)) {
        file.bufferedWriter().use { writer ->
            diagnostics.forEach { diagnostic ->
                writer.write(diagnostic.toString())
                writer.newLine()
                writer.write(separator)
                writer.newLine()
                writer.newLine()
            }
        }
    }
}
/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.supportedAppleTargets
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.appleTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.configuration
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks.AssembleSwiftPackageBinary
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks.GenerateSPMPackageFromSwiftExport
import org.jetbrains.kotlin.gradle.plugin.mpp.export.ExportExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.export.SwiftExportConfigurationCompat
import org.jetbrains.kotlin.gradle.plugin.mpp.export.SwiftExportSwiftPackageIntegrationConfiguration
import org.jetbrains.kotlin.gradle.plugin.mpp.export.effectiveDependencyOverrides
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.utils.konanDistribution
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager

/**
 * The SwiftPM platform name of each Apple [Family] that can appear in a generated package.
 */
private val swiftPackagePlatformNames: Map<Family, String> = mapOf(
    Family.IOS to "iOS",
    Family.OSX to "macOS",
    Family.TVOS to "tvOS",
    Family.WATCHOS to "watchOS",
)

/**
 * The `platforms:` entry of the generated `Package.swift` for the given target [families]: the SwiftPM platform
 * name of each family mapped to the minimum version from [SwiftExportConstants.minimumDeploymentTargets].
 *
 * Ordered by platform name so that the generated manifest does not depend on the order of the Kotlin targets.
 */
internal fun swiftPackagePlatforms(families: Iterable<Family>): Map<String, String> = families
    .distinct()
    .mapNotNull { family ->
        // A family without a SwiftPM platform name or a minimum deployment target is skipped silently.
        val name = swiftPackagePlatformNames[family] ?: return@mapNotNull null
        val version = SwiftExportConstants.minimumDeploymentTargets[family] ?: return@mapNotNull null
        name to version
    }
    .sortedBy { it.first }
    .toMap(LinkedHashMap())

/**
 * Registers the Swift package export pipeline for every build type. Called once per project after the DSL
 * is finalised, only when
 * [org.jetbrains.kotlin.gradle.plugin.mpp.export.SwiftExportConfigurationDsl.swiftPackageIntegration] was activated.
 *
 * The per-target Swift Export run and Kotlin static library are shared with the Xcode integration through
 * [registerSwiftExportRunAndBinary]; the package-specific tasks are registered on top of them:
 *
 * ```
 * generate<BuildType>SwiftPackage         Package.swift, Sources/ from the first Apple target
 * assemble<BuildType>SwiftPackageBinary   <Module>Kotlin.xcframework with one slice per Apple platform
 * export<BuildType>SwiftPackage           Sync of both into outputDirectory/<Configuration>
 * ```
 */
internal fun Project.registerSwiftPackageExportPipeline(exportExtension: ExportExtension) {
    val integration = exportExtension.swiftExportConfiguration.activatedSwiftPackageIntegration ?: return
    val appleTargets = multiplatformExtension.supportedAppleTargets().toList()
    if (appleTargets.isEmpty()) return

    // The compat object doesn't depend on the build type, so it is built once per target.
    val configurations = appleTargets.associateWith { target ->
        SwiftExportConfigurationCompat.from(
            configuration = exportExtension.swiftExportConfiguration,
            kotlinNativeCompilation = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME),
            dependencyOptionsOverrides = exportExtension.swiftExportConfiguration.effectiveDependencyOverrides(providers),
            providers = providers,
            objects = objects,
        )
    }

    // `entries` is not available: this module compiles against an older Kotlin API version.
    NativeBuildType.values().forEach { buildType ->
        val outputs = appleTargets.map { target ->
            target to registerSwiftExportRunAndBinary(
                configurations.getValue(target), SwiftExportDSLConstants.TASK_GROUP, buildType, target
            )
        }
        registerSwiftPackageExport(integration, buildType, outputs)
    }
}

private fun Project.registerSwiftPackageExport(
    integration: SwiftExportSwiftPackageIntegrationConfiguration,
    buildType: NativeBuildType,
    outputs: List<Pair<KotlinNativeTarget, SwiftExportBuildOutputs>>,
) {
    val configuration = buildType.configuration
    val primary = outputs.first().second
    val others = outputs.drop(1).map { it.second }
    val swiftApiModuleName = primary.swiftApiModuleName
    val kotlinBinaryTargetName = swiftApiModuleName.map { "${it}Kotlin" }
    val packageDirectory = layout.buildDirectory.dir("SwiftPackage/$configuration")

    val generateTask = locateOrRegisterTask<GenerateSPMPackageFromSwiftExport>(
        lowerCamelCaseName("generate", buildType.getName(), "SwiftPackage")
    ) { task ->
        task.description = "Generates the $configuration Swift package with the exported Swift API"
        task.group = SwiftExportDSLConstants.TASK_GROUP

        task.kotlinRuntime.set(file(Distribution(konanDistribution.root.absolutePath).kotlinRuntimeForSwiftHome))
        task.swiftModulesFile.set(primary.swiftExportTask.map { it.parameters.swiftModulesFile.get() })
        task.swiftApiModuleName.set(swiftApiModuleName)
        task.swiftLibraryName.set(swiftApiModuleName.map { it + "Library" })
        task.kotlinBinaryTargetName.set(kotlinBinaryTargetName)
        task.platforms.set(swiftPackagePlatforms(outputs.map { it.first.konanTarget.family }))
        task.primarySwiftExportOutput.set(primary.swiftExportTask.flatMap { it.parameters.outputPath })
        others.forEach { other ->
            task.otherSwiftExportOutputs.from(other.swiftExportTask.flatMap { it.parameters.outputPath })
        }

        task.packagePath.set(packageDirectory.map { it.dir("package") })
    }

    val assembleTask = locateOrRegisterTask<AssembleSwiftPackageBinary>(
        lowerCamelCaseName("assemble", buildType.getName(), "SwiftPackageBinary")
    ) { task ->
        task.description = "Assembles the $configuration Kotlin binary XCFramework for the Swift package"
        task.group = SwiftExportDSLConstants.TASK_GROUP

        task.libraryName.set(kotlinBinaryTargetName.map { "lib$it.a" })
        task.xcframeworkName.set(kotlinBinaryTargetName.map { "$it.xcframework" })
        task.fatLibrariesDirectory.set(packageDirectory.map { it.dir("fat") })
        task.binaryDirectory.set(packageDirectory.map { it.dir("binary") })
        outputs.forEach { (target, output) ->
            // `map` rather than `flatMap`: KotlinNativeLink.outputFile is a derived provider with no producer,
            // so flatMap would drop the dependency on the link task.
            task.addLibrary(target.konanTarget.appleTarget, output.staticLibrary.linkTaskProvider.map { it.outputFile.get() })
        }
    }

    registerSwiftPackageSync(integration, buildType, generateTask, assembleTask)
}

private fun Project.registerSwiftPackageSync(
    integration: SwiftExportSwiftPackageIntegrationConfiguration,
    buildType: NativeBuildType,
    generateTask: TaskProvider<GenerateSPMPackageFromSwiftExport>,
    assembleTask: TaskProvider<AssembleSwiftPackageBinary>,
) {
    val configuration = buildType.configuration

    // Every build type has its own destination subdirectory: `export<BuildType>SwiftPackage` is a Sync, so two
    // build types sharing one destination would delete each other's package on every build.
    //
    // The destination itself is validated at configuration time by
    // `org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.SwiftPackageOutputDirectoryChecker`, so this
    // stays a plain Provider.
    val outputDirectory: Provider<Directory> = integration.outputDirectory.map { it.dir(configuration) }

    locateOrRegisterTask<Sync>(lowerCamelCaseName("export", buildType.getName(), "SwiftPackage")) { task ->
        task.description = "Exports the $configuration Swift package into swiftPackageIntegration.outputDirectory/$configuration"
        task.group = SwiftExportDSLConstants.TASK_GROUP
        // The package sources and the Kotlin binary are only produced on a Mac host. Without this guard the Sync
        // would run with empty sources on any other host and delete everything in the user's output directory.
        task.onlyIf { HostManager.hostIsMac }

        task.from(generateTask.flatMap { it.packagePath }) { spec ->
            spec.exclude("OtherIncludes/**")
        }
        task.from(assembleTask.flatMap { it.binaryDirectory })
        task.into(outputDirectory)
    }
}

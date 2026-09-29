/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport

import org.gradle.api.Project
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
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks.SwiftExportTargetOutput
import org.jetbrains.kotlin.gradle.plugin.mpp.export.ExportExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.export.SwiftExportSwiftPackageIntegrationConfiguration
import org.jetbrains.kotlin.gradle.plugin.mpp.export.internal.SwiftExportConfigurationCompat
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.utils.konanDistribution
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager

/**
 * The SwiftPM platform names of the Apple families.
 */
internal val swiftPackagePlatformNames: Map<Family, String> = mapOf(
    Family.IOS to "iOS",
    Family.OSX to "macOS",
    Family.TVOS to "tvOS",
    Family.WATCHOS to "watchOS",
)

/**
 * The `platforms:` of the generated manifest: the platform name of each of the Apple [families] and the minimum
 * version Swift Export requires for it. Sorted, so that the manifest doesn't depend on the order of the targets.
 */
internal fun swiftPackagePlatforms(families: Iterable<Family>): Map<String, String> = families
    .associate { swiftPackagePlatformNames.getValue(it) to SwiftExportConstants.minimumDeploymentTargets.getValue(it) }
    .toSortedMap()

/**
 * Registers the tasks of the Swift package integration for every build type:
 *
 * ```
 * generate<BuildType>SwiftPackage         Package.swift and the sources of every Apple target
 * assemble<BuildType>SwiftPackageBinary   <Module>Kotlin.xcframework
 * export<BuildType>SwiftPackage           both, in outputDirectory/<Configuration>
 * ```
 *
 * The Swift Export runs and the Kotlin static libraries are shared with the Xcode integration.
 */
internal fun Project.registerSwiftPackageExportPipeline(exportExtension: ExportExtension) {
    val integration = exportExtension.swiftExportConfiguration.activatedSwiftPackageIntegration ?: return
    val appleTargets = multiplatformExtension.supportedAppleTargets().toList()
    if (appleTargets.isEmpty()) return

    val configurations = appleTargets.associateWith { target ->
        SwiftExportConfigurationCompat.from(
            configuration = exportExtension.swiftExportConfiguration,
            kotlinNativeCompilation = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME),
            providers = providers,
            objects = objects,
        )
    }

    // TODO(KT-84114): switch to `entries` once the plugins compile with Kotlin 2.1,
    //  see https://github.com/JetBrains/kotlin/pull/8212.
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
    val swiftApiModuleName = outputs.first().second.swiftApiModuleName
    val kotlinBinaryTargetName = swiftApiModuleName.map { "${it}Kotlin" }
    val packageDirectory = layout.buildDirectory.dir("SwiftPackage/$configuration")

    val generateTask = locateOrRegisterTask<GenerateSPMPackageFromSwiftExport>(
        lowerCamelCaseName("generate", buildType.getName(), "SwiftPackage")
    ) { task ->
        task.description = "Generates the $configuration Swift package with the exported Swift API"
        task.group = SwiftExportDSLConstants.TASK_GROUP

        task.kotlinRuntime.set(file(Distribution(konanDistribution.root.absolutePath).kotlinRuntimeForSwiftHome))
        task.swiftApiModuleName.set(swiftApiModuleName)
        task.swiftLibraryName.set(swiftApiModuleName.map { it + "Library" })
        task.kotlinBinaryTargetName.set(kotlinBinaryTargetName)
        task.platforms.set(swiftPackagePlatforms(outputs.map { it.first.konanTarget.family }))
        outputs.forEach { (target, output) ->
            task.targetOutputs.add(objects.SwiftExportTargetOutput(target, output.swiftExportTask))
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
            // `map` rather than `flatMap`: `outputFile` is derived with a `flatMap` of its own and carries no producer,
            // so only a `map` on the task provider makes the link task a dependency.
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

    locateOrRegisterTask<Sync>(lowerCamelCaseName("export", buildType.getName(), "SwiftPackage")) { task ->
        task.description = "Exports the $configuration Swift package into swiftPackageIntegration.outputDirectory/$configuration"
        task.group = SwiftExportDSLConstants.TASK_GROUP
        // On other hosts there is nothing to copy, and a Sync of nothing empties its destination.
        task.onlyIf { HostManager.hostIsMac }

        task.from(generateTask.flatMap { it.packagePath }) { spec ->
            spec.exclude("OtherIncludes/**")
        }
        task.from(assembleTask.flatMap { it.binaryDirectory })
        // A directory per build type: two Sync tasks can't share a destination.
        task.into(integration.outputDirectory.map { it.dir(configuration) })
        // Xcode and SwiftPM keep their state in the package directory.
        task.preserve { it.include(".swiftpm/**", ".build/**") }
    }
}

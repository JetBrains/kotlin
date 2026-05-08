/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.apple

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.appleArchitecture
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.applePlatform
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.appleTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.sdk
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.DumpXcodeBuildArgs
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.GenerateSyntheticLinkageImportProject
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.PrepareXcodeBuildArgsDumpFingerprint
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMImportExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMXcodeDumpBuildService
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.TransitiveSwiftPMDependencies
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.toNormalizedDumpTaskFingerprintInput
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

internal fun Project.registerSwiftPMImportDumpArgsTestTasks(
    extension: SwiftPMImportExtension,
    packageGeneration: TaskProvider<GenerateSyntheticLinkageImportProject>,
    stubTrackedFiles: File,
    target: KonanTarget = KonanTarget.IOS_SIMULATOR_ARM64,
    dumpTaskName: String = "packageDumpArgs",
    fingerprintTaskName: String = "${dumpTaskName}Fingerprint",
): TaskProvider<DumpXcodeBuildArgs> {
    val dumpTask = tasks.register<DumpXcodeBuildArgs>(dumpTaskName) {
        dependsOn(packageGeneration)
        xcodebuildPlatform.set(target.applePlatform)
        xcodebuildSdk.set(target.appleTarget.sdk)
        architectures.add(target.appleArchitecture)
        hasSwiftPMDependencies.set(true)
        coordinationService.set(SwiftPMXcodeDumpBuildService.registerIfAbsent(project))
        filesToTrackFromLocalPackages.set(stubTrackedFiles)
        syntheticImportProjectRoot.set(packageGeneration.map { it.syntheticImportProjectRoot.get() })
        swiftPMDependenciesCheckout.set(layout.buildDirectory.dir("checkout"))
        xcodebuildExecutionHashFile.set(layout.buildDirectory.file("kotlin/customSwiftImportDump/xcodebuildExecutionHash.txt"))
        xcodeDumpLocationFile.set(layout.buildDirectory.file("kotlin/customSwiftImportDump/location.json"))
    }

    val emptyTransitiveSwiftPMDependencies = TransitiveSwiftPMDependencies(emptyMap())
    val fingerprintTask = tasks.register<PrepareXcodeBuildArgsDumpFingerprint>(fingerprintTaskName) {
        xcodebuildPlatform.set(target.applePlatform)
        xcodebuildSdk.set(target.appleTarget.sdk)
        architectures.add(target.appleArchitecture)
        packageResolvedSynchronization.set("identifier:default")
        buildSettingsFingerprint.set("")
        filesToTrackFromLocalPackages.set(stubTrackedFiles)
        directSwiftPMDependencies.set(extension.swiftPMDependencies)
        normalizedTransitiveSwiftPMDependenciesInput.set(emptyTransitiveSwiftPMDependencies.toNormalizedDumpTaskFingerprintInput())
        xcodebuildExecutionHashFile.set(dumpTask.flatMap { it.xcodebuildExecutionHashFile })
    }

    dumpTask.configure {
        it.dependsOn(fingerprintTask)
    }
    return dumpTask
}

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.gradle.tasks.locateTask
import java.io.File

internal class SwiftPMImportProducts(
    /**
     * Whether the project has direct or transitive SwiftPM dependencies, i.e. whether [fetchTask] resolves a synthetic
     * package at all. Backed by the dependencies' SwiftPM metadata, so read it only at execution time and `dependsOn` it.
     */
    val hasDependencies: Provider<Boolean>,
    /**
     * The in-project root of the synthetic SPM package. With fingerprint coordination enabled this holds a
     * *synced copy* of the shared owner package — content-identical (useful as a tracked input) but not
     * consumable as a `.package(path:)` dependency, because its relative references are baked against the
     * owner's location; resolve the consumable root with [sharedPackageRootFor] in that case.
     */
    val syntheticPackageLocalRoot: Provider<Directory>,
    /**
     * The synthetic package's fingerprint file; present only when fingerprint coordination is enabled. Its
     * hash keys the shared owner locations ([sharedPackageRootFor], [sharedCheckoutFor]) — read it only at
     * execution time, from tasks that depend on [fetchTask].
     */
    val syntheticPackageFingerprint: Provider<RegularFile>,
    /** The service that maps a fingerprint hash to the shared owner locations. */
    val coordinationService: Provider<SwiftImportFingerprintedCoordinationService>,
    /** The umbrella library product vended by the synthetic package. */
    val umbrellaProductName: String,
    /**
     * The in-project SwiftPM checkout. Like [syntheticPackageLocalRoot], with fingerprint coordination the
     * populated checkout is the shared one ([sharedCheckoutFor]). A consumer that resolves the synthetic
     * package itself should pass the effective checkout to xcodebuild (`-clonedSourcePackagesDirPath`) to
     * reuse the fetched remote packages.
     */
    val swiftPMLocalCheckout: Provider<Directory>,
    /**
     * The task running `swift package resolve` on the synthetic package. Its output locations are not
     * task-managed output properties, so consumers must `dependsOn` this task explicitly.
     */
    val fetchTask: TaskProvider<*>,
)

/** The own SwiftPM-import cinterop klib of a target, bundling the imported packages' Objective-C modules. */
internal class SwiftPMImportCinterop(
    /** Module name of the reexported cinterop klib. */
    val moduleName: Provider<String>,
    /** The cinterop klib (klib manifest `interop=true`). */
    val klib: Provider<File>,
)

private fun packageHash(fingerprintFile: File): String = fingerprintFile.readSwiftImportFingerprint().incrementalFingerprint

internal fun SwiftImportFingerprintedCoordinationService.sharedPackageRootFor(fingerprintFile: File): File =
    sharedPackageGenerationRoot(packageHash(fingerprintFile))

internal fun SwiftImportFingerprintedCoordinationService.sharedCheckoutFor(fingerprintFile: File): File =
    sharedCheckoutDir(packageHash(fingerprintFile))

/**
 * The SwiftPM-import synthetic package of this project, or `null` when SwiftPM import is disabled. The package also
 * holds the transitive SwiftPM dependencies, so it exists even without direct ones; see [SwiftPMImportProducts.hasDependencies].
 */
internal fun Project.swiftPMImportProductsOrNull(): SwiftPMImportProducts? {
    if (kotlinPropertiesProvider.disableSwiftPMImport) return null
    val fetchTask = locateOrRegisterSwiftPMImportFetchTask()
    return SwiftPMImportProducts(
        hasDependencies = hasDirectOrTransitiveSwiftPMDependencies(),
        syntheticPackageLocalRoot = fetchTask.flatMap { it.syntheticImportProjectRoot },
        syntheticPackageFingerprint = fetchTask.flatMap { it.syntheticPackageFingerprint.fingerprintFile },
        coordinationService = locateOrRegisterCoordinationService(),
        umbrellaProductName = GenerateSyntheticLinkageImportProject.SYNTHETIC_IMPORT_TARGET_MAGIC_NAME,
        swiftPMLocalCheckout = fetchTask.flatMap { it.swiftPMDependenciesCheckout },
        fetchTask = fetchTask,
    )
}

/** Calls [onAvailable] once the target has its own SwiftPM-import cinterop, which only direct SwiftPM dependencies create. */
internal fun KotlinNativeTarget.whenSwiftPMImportCinteropAvailable(onAvailable: (SwiftPMImportCinterop) -> Unit) {
    val mainCompilation = compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
    mainCompilation.cinterops
        .matching { it.name == GenerateSyntheticLinkageImportProject.SWIFT_PM_IMPORT_CINTEROP_NAME }
        .all { cinterop ->
            val cinteropTask = project.locateTask<CInteropProcess>(cinterop.interopProcessingTaskName) ?: return@all
            onAvailable(
                SwiftPMImportCinterop(
                    moduleName = cinteropTask.map { it.moduleName },
                    klib = cinteropTask.flatMap { it.klibOutput },
                )
            )
        }
}

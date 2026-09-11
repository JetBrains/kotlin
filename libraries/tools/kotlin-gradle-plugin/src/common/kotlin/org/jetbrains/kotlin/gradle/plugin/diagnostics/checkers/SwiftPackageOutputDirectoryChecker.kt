/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.AfterFinaliseDsl
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.findExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.configuration
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.SwiftExportConstants
import org.jetbrains.kotlin.gradle.plugin.mpp.export.EXPORT_EXTENSION_NAME
import org.jetbrains.kotlin.gradle.plugin.mpp.export.ExportExtension
import java.io.File

/**
 * Validates the destination of `export<BuildType>SwiftPackage` as soon as the DSL is finalised.
 *
 * The export is a `Sync`: everything in its destination that is not part of the generated Swift package is
 * deleted. A typo in `swiftPackageIntegration.outputDirectory` that points at a source tree would wipe it, and
 * without this checker the user would only find out after the whole graph — the Swift Export runs, the native
 * links and `xcodebuild` — has already been built.
 *
 * Each build type has its own destination, `outputDirectory/<Configuration>`, so every one of them is
 * validated. The safety rules are also applied to `outputDirectory` itself: with the build type appended, a
 * destination is never literally the project directory, but pointing the DSL property at it is exactly the
 * mistake worth refusing.
 *
 * Awaits [AfterFinaliseDsl] so that the order of the DSL calls in the build script doesn't matter.
 */
internal object SwiftPackageOutputDirectoryChecker : KotlinGradleProjectChecker {

    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        AfterFinaliseDsl.await()

        val mppExtension = multiplatformExtension ?: return
        val exportExtension = mppExtension.findExtension<ExportExtension>(EXPORT_EXTENSION_NAME) ?: return
        if (!exportExtension.isSwiftExportConfigured) return
        val integration = exportExtension.swiftExportConfiguration.activatedSwiftPackageIntegration ?: return

        // No Apple targets means no export tasks are registered at all, so there is nothing to validate.
        val hasAppleTargets = mppExtension.awaitTargets()
            .withType(KotlinNativeTarget::class.java)
            .any { it.konanTarget.family.isAppleFamily }
        if (!hasAppleTargets) return

        val declaredOutputDirectory = integration.outputDirectory.orNull
        if (declaredOutputDirectory == null) {
            collector.report(diagnosticsContext, KotlinToolingDiagnostics.SwiftExportPackageOutputDirectoryNotSet(projectPath))
            return
        }
        val outputDirectory = declaredOutputDirectory.asFile.absoluteFile.normalize()

        val projectDirectory = project.projectDir.absoluteFile.normalize()
        // project.rootDir rather than rootProject.projectDir: the same directory without touching another Project.
        val rootProjectDirectory = project.rootDir.absoluteFile.normalize()

        unsafeReason(outputDirectory, projectDirectory, rootProjectDirectory)?.let { reason ->
            collector.report(
                diagnosticsContext,
                KotlinToolingDiagnostics.SwiftExportPackageOutputDirectoryUnsafe(projectPath, outputDirectory.path, reason)
            )
            return
        }

        // `entries` is not available: this module compiles against an older Kotlin API version.
        for (buildType in NativeBuildType.values()) {
            val destination = outputDirectory.resolve(buildType.configuration)

            val reason = unsafeReason(destination, projectDirectory, rootProjectDirectory)
            if (reason != null) {
                collector.report(
                    diagnosticsContext,
                    KotlinToolingDiagnostics.SwiftExportPackageOutputDirectoryUnsafe(projectPath, destination.path, reason)
                )
                continue
            }

            if (isNonEmptyForeignDirectory(destination)) {
                collector.report(
                    diagnosticsContext,
                    KotlinToolingDiagnostics.SwiftExportPackageOutputDirectoryNotEmpty(
                        projectPath,
                        destination.path,
                        SwiftExportConstants.SWIFT_PACKAGE_MARKER_FILE_NAME,
                    )
                )
            }
        }
    }

    /**
     * Why [directory] must not be synchronised into, or `null` if there is no reason to refuse it. The text is
     * rendered after "which", so it reads as "'...', which is the project directory".
     */
    private fun unsafeReason(directory: File, projectDirectory: File, rootProjectDirectory: File): String? {
        for (protected in listOf(projectDirectory, rootProjectDirectory)) {
            if (protected.startsWith(directory)) {
                return "is the project directory '$protected' or one of its ancestors"
            }
        }

        val child = directory.listFiles().orEmpty().firstOrNull { it.name == ".git" || it.extension in XCODE_EXTENSIONS }
        if (child != null) {
            return "contains '${child.name}'"
        }

        return null
    }

    /**
     * Whether [directory] holds something that this export did not produce. A previous export leaves the
     * marker file behind, and the export replaces its own output wholesale, so only a non-empty directory
     * without the marker is a directory whose content would be lost.
     */
    private fun isNonEmptyForeignDirectory(directory: File): Boolean {
        val children = directory.listFiles() ?: return false
        return children.isNotEmpty() && children.none { it.name == SwiftExportConstants.SWIFT_PACKAGE_MARKER_FILE_NAME }
    }

    private val XCODE_EXTENSIONS = setOf("xcodeproj", "xcworkspace")
}

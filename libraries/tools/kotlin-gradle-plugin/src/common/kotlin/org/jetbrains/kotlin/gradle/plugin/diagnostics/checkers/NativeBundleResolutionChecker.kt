/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.artifacts.ivyservice.TypedResolveException
import org.gradle.api.internal.artifacts.transform.TransformException
import org.gradle.internal.resolve.ArtifactNotFoundException
import org.jetbrains.kotlin.gradle.plugin.KOTLIN_NATIVE_BUNDLE_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.utils.withType
import org.jetbrains.kotlin.konan.target.HostManager

internal object NativeBundleResolutionChecker : KotlinGradleProjectChecker {

    override suspend fun KotlinGradleProjectCheckerContext.runChecks(
        collector: KotlinToolingDiagnosticsCollector
    ) {
        // Early returns for non-applicable projects
        val extension = multiplatformExtension ?: return
        if (extension.awaitTargets().withType<KotlinNativeTarget>().isEmpty()) return

        KotlinPluginLifecycle.Stage.ReadyForExecution.await()

        val nativeBundleConfig = project.configurations
            .findByName(KOTLIN_NATIVE_BUNDLE_CONFIGURATION_NAME)
            ?: return

        project.resolveNativeBundleConfiguration(nativeBundleConfig, collector)
    }

    private fun Project.resolveNativeBundleConfiguration(
        configuration: Configuration,
        collector: KotlinToolingDiagnosticsCollector
    ) {
        try {
            configuration.resolve()
        } catch (e: TypedResolveException) {
            handleResolveException(e, collector)
        } catch (e: Exception) {
            // Log unexpected exceptions for debugging
            logger.debug(
                "Unexpected exception while resolving native bundle configuration",
                e
            )
        }
    }

    private fun Project.handleResolveException(
        exception: TypedResolveException,
        collector: KotlinToolingDiagnosticsCollector
    ) {
        val transformException = exception.cause as? TransformException
        val artifactNotFoundException = transformException?.cause as? ArtifactNotFoundException

        when {
            artifactNotFoundException != null -> {
                reportArtifactNotFound(artifactNotFoundException, collector)
            }
            transformException != null -> {
                // Handle other transform exceptions if needed
                logger.warn(
                    "Transform exception during native bundle resolution: ${transformException.message}"
                )
            }
            else -> {
                // Generic resolution error
                logger.warn(
                    "Failed to resolve native bundle configuration: ${exception.message}"
                )
            }
        }
    }

    private fun Project.reportArtifactNotFound(
        exception: ArtifactNotFoundException,
        collector: KotlinToolingDiagnosticsCollector
    ) {
        // Report diagnostic instead of println
        collector.report(
            project,
            KotlinToolingDiagnostics.NativeBundleResolution(
                exception.message ?: "Kotlin/Native bundle not found",
                HostManager.hostName
            )
        )

        // Keep debug logging for development
        logger.debug("Artifact not found details", exception)
    }
}
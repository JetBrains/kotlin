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
import org.gradle.internal.resolve.ModuleVersionNotFoundException
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
            logger.error("Unexpected exception while resolving native bundle configuration", e)
        }
    }

    private fun Project.handleResolveException(
        exception: TypedResolveException,
        collector: KotlinToolingDiagnosticsCollector
    ) {
        val transformException = exception.cause as? TransformException
        val moduleVersionException = exception.cause as? ModuleVersionNotFoundException
        val artifactNotFoundException = transformException?.cause as? ArtifactNotFoundException

        when {
            artifactNotFoundException != null -> {
                reportArtifactNotFound(artifactNotFoundException, collector)
            }
            moduleVersionException != null -> {
                reportVersionNotFound(moduleVersionException, collector)
            }
            transformException != null -> {
                // Handle other transform exceptions
                logger.error("Transform exception during native bundle resolution", transformException)
            }
            else -> {
                // Generic resolution error
                logger.error("Failed to resolve native bundle configuration", exception)
            }
        }
    }

    private fun Project.reportVersionNotFound(
        exception: Throwable,
        collector: KotlinToolingDiagnosticsCollector
    ) {
        collector.report(
            project,
            KotlinToolingDiagnostics.NativeBundleVersionNotFoundError(
                HostManager.platformName(),
                exception
            )
        )

        logger.debug("Version not found details", exception)
    }

    private fun Project.reportArtifactNotFound(
        exception: Throwable,
        collector: KotlinToolingDiagnosticsCollector
    ) {
        collector.report(
            project,
            KotlinToolingDiagnostics.NativeBundleArtifactNotFoundError(
                HostManager.platformName(),
                exception
            )
        )

        logger.debug("Artifact not found details", exception)
    }
}
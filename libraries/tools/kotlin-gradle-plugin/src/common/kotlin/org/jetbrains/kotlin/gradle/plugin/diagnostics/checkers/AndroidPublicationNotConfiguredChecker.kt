/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.internal.getComponentOrNull
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinTargetSoftwareComponent
import org.jetbrains.kotlin.gradle.utils.isPluginApplied

internal object AndroidPublicationNotConfiguredChecker : KotlinGradleProjectChecker {
    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        // kotlin("android") is applied
        if (project.kotlinExtension !is KotlinAndroidProjectExtension) return
        project.isPluginApplied("com.android.library") || return
        project.isPluginApplied("maven-publish") || return

        // After this stage, all publications are created and configured
        Stage.AfterFinaliseDsl.await()

        val publishing = project.extensions.getByName("publishing") as PublishingExtension
        publishing.publications.withType(MavenPublication::class.java).configureEach { publication ->
            val component = publication.getComponentOrNull(project) ?: return@configureEach

            // This is heuristic, but still reliable check.
            // When all three plugins are applied AND user configured AGP's publications correctly,
            // then neither of the components should be `KotlinTargetComponent`
            if (component !is KotlinTargetSoftwareComponent) return@configureEach

            collector.report(
                project,
                KotlinToolingDiagnostics.AndroidPublicationNotConfigured(
                    // cast is needed to avoid ambiguity on getName()
                    componentName = component.name,
                    publicationName = publication.name,
                )
            )
        }
    }
}
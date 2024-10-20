/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics.AndroidReleasePublicationIsNotConsumable
import org.jetbrains.kotlin.gradle.plugin.diagnostics.kotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinVariant
import org.jetbrains.kotlin.gradle.plugin.mpp.publishing.awaitMavenPublications
import org.jetbrains.kotlin.gradle.plugin.mpp.publishing.kotlinMultiplatformRootPublication
import org.jetbrains.kotlin.tooling.core.extrasReadWriteProperty

private var KotlinAndroidTarget.publishLibraryVariantsCallTrace: Throwable?
        by extrasReadWriteProperty("androidReleasePublicationRequestTrace")

/**
 * Remembers the call site where user configures [KotlinAndroidTarget.publishLibraryVariants].
 * So when [AndroidReleasePublicationIsNotConsumable] is reported, users can find via stacktrace where
 * publication configuration happens.
 *
 * [KotlinAndroidTarget.publishLibraryVariants] can be called in multiple places.
 * So technically, there are many callsites where misconfiguration could have happened.
 * However, for practical reasons, it is enough to "remember" just last place, so when navigating it can be easily fixed
 * by setting values explicitly.
 */
internal fun KotlinAndroidTarget.rememberPublishLibraryVariantsCallTrace(trace: Throwable) {
    publishLibraryVariantsCallTrace = trace
}

internal fun KotlinAndroidTarget.reportAndroidReleaseComponentPublicationIsBroken(
    component: KotlinVariant,
    publishableAndroidVariantNames: List<String>,
) {
    project.launch {
        val rootPublication = project.kotlinMultiplatformRootPublication.await() ?: return@launch
        val publication = awaitMavenPublications()[component.name] ?: return@launch

        val coordinates = "${publication.groupId}:${publication.artifactId}:${publication.version}"
        val rootCoordinates = "${rootPublication.groupId}:${rootPublication.artifactId}:${rootPublication.version}"

        project.kotlinToolingDiagnosticsCollector.report(
            project,
            AndroidReleasePublicationIsNotConsumable(
                coordinates = coordinates,
                rootCoordinates = rootCoordinates,
                publishableAndroidVariants = publishableAndroidVariantNames,
                trace = publishLibraryVariantsCallTrace
            )
        )
    }
}
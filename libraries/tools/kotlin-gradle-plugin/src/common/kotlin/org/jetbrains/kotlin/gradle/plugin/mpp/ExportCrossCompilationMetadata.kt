/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Input
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupCoroutine
import org.jetbrains.kotlin.gradle.plugin.internal.KotlinProjectSharedDataProvider
import org.jetbrains.kotlin.gradle.plugin.internal.KotlinSecondaryVariantsDataSharing
import org.jetbrains.kotlin.gradle.plugin.internal.KotlinShareableDataAsSecondaryVariant
import org.jetbrains.kotlin.gradle.plugin.internal.kotlinSecondaryVariantsDataSharing

private const val PROJECT_CROSS_COMPILATION_SHARING_KEY = "crossCompilationMetadata"

internal val ExportCrossCompilationMetadata = KotlinProjectSetupCoroutine {
    val nativeTargets = project.multiplatformExtension.awaitTargets().filterIsInstance<KotlinNativeTarget>()
    val sharingService = project.kotlinSecondaryVariantsDataSharing

    nativeTargets.forEach { target ->
        val crossCompilationSupported = target.crossCompilationOnCurrentHostSupported
        val crossCompilationData = provider {
            CrossCompilationData(
                crossCompilationSupported.getOrThrow(),
            )
        }
        val configuration = configurations.getByName(target.apiElementsConfigurationName)
        sharingService.shareDataFromProvider(
            PROJECT_CROSS_COMPILATION_SHARING_KEY,
            configuration,
            crossCompilationData
        )
    }
}

internal class CrossCompilationData(
    @get:Input
    val crossCompilationSupported: Boolean,
) : KotlinShareableDataAsSecondaryVariant

internal fun KotlinSecondaryVariantsDataSharing.consumeCrossCompilationMetadata(
    from: Configuration
): KotlinProjectSharedDataProvider<CrossCompilationData> = consume(
    PROJECT_CROSS_COMPILATION_SHARING_KEY,
    from,
    CrossCompilationData::class.java
)
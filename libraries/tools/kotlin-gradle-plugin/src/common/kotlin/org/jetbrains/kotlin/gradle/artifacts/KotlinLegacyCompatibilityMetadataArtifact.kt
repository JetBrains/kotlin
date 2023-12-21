/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.artifacts

import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.tasks.bundling.Jar
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.categoryByName
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope
import org.jetbrains.kotlin.gradle.plugin.sources.sourceSetDependencyConfigurationByScope
import org.jetbrains.kotlin.gradle.plugin.usesPlatformOf
import org.jetbrains.kotlin.gradle.targets.metadata.COMMON_MAIN_ELEMENTS_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.targets.metadata.isCompatibilityMetadataVariantEnabled
import org.jetbrains.kotlin.gradle.targets.metadata.isKotlinGranularMetadataEnabled
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.createConsumable
import org.jetbrains.kotlin.gradle.utils.setAttribute

internal val KotlinLegacyCompatibilityMetadataArtifact = KotlinTargetArtifact { target, _, _ ->
    if (target !is KotlinMetadataTarget) return@KotlinTargetArtifact
    if (!target.project.isKotlinGranularMetadataEnabled) return@KotlinTargetArtifact
    if (!target.project.isCompatibilityMetadataVariantEnabled) return@KotlinTargetArtifact

    val legacyJar = target.project.registerTask<Jar>(target.legacyArtifactsTaskName)
    legacyJar.configure {
        // Capture it here to use in onlyIf spec. Direct usage causes serialization of target attempt when configuration cache is enabled
        it.description = "Assembles an archive containing the Kotlin metadata of the commonMain source set."
        it.from(target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME).output.allOutputs)
    }

    /* Create actual Gradle artifact */
    target.project.configurations.createConsumable(COMMON_MAIN_ELEMENTS_CONFIGURATION_NAME).apply {
        usesPlatformOf(target)

        attributes.setAttribute(Usage.USAGE_ATTRIBUTE, KotlinUsages.producerApiUsage(target))
        attributes.setAttribute(Category.CATEGORY_ATTRIBUTE, target.project.categoryByName(Category.LIBRARY))

        val commonMainApiConfiguration = target.project.configurations.sourceSetDependencyConfigurationByScope(
            target.project.kotlinExtension.sourceSets.getByName(KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME),
            KotlinDependencyScope.API_SCOPE
        )
        extendsFrom(commonMainApiConfiguration)

        target.project.artifacts.add(name, legacyJar)
    }
}
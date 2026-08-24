/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.importmodel

import org.gradle.api.attributes.Attribute
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.internal.compatAccessor
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.KotlinTargetSideEffect
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.utils.currentBuildId

// Carries the exact producer compilation ID through Gradle's selected local project variant
internal val kotlinImportModelCompilationIdAttribute: Attribute<String> =
    Attribute.of("org.jetbrains.kotlin.import-models.compilation-unit-id", String::class.java)

internal val KotlinImportModelVariantIdentitySideEffect = KotlinTargetSideEffect { target ->
    val (targetKey, compilationName) = when (target) {
        is KotlinMetadataTarget -> target.name to KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME
        is KotlinJvmTarget, is KotlinNativeTarget -> target.targetName to KotlinCompilation.MAIN_COMPILATION_NAME
        else -> return@KotlinTargetSideEffect
    }
    val project = target.project
    val compilationId = compilationUnitIdValue(
        project.currentBuildId().compatAccessor(project).buildPath,
        project.path,
        targetKey,
        compilationName,
    )
    listOfNotNull(
        project.configurations.findByName(target.apiElementsConfigurationName),
        project.configurations.findByName(target.runtimeElementsConfigurationName),
    ).forEach { configuration ->
        configuration.attributes.attribute(kotlinImportModelCompilationIdAttribute, compilationId)
    }
}

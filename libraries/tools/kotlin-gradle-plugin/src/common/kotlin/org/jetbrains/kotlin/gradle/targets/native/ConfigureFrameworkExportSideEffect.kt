/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native

import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.gradle.plugin.categoryByName
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractNativeLibrary
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.usesPlatformOf
import org.jetbrains.kotlin.gradle.targets.KotlinTargetSideEffect
import org.jetbrains.kotlin.gradle.utils.maybeCreateResolvable
import org.jetbrains.kotlin.gradle.utils.setAttribute

internal val ConfigureFrameworkExportSideEffect = KotlinTargetSideEffect<KotlinNativeTarget> { target ->
    val project = target.project

    target.binaries.withType(AbstractNativeLibrary::class.java).all { framework ->
        project.configurations.maybeCreateResolvable(framework.exportConfigurationName).apply {
            isVisible = false
            isTransitive = false
            usesPlatformOf(target)
            attributes.setAttribute(Usage.USAGE_ATTRIBUTE, KotlinUsages.consumerApiUsage(target))
            attributes.setAttribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
            description = "Dependencies to be exported in framework ${framework.name} for target ${target.targetName}"
        }
    }
}

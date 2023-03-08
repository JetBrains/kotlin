/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.android

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginLifecycle.Stage
import org.jetbrains.kotlin.gradle.plugin.newLifecycleAwareProperty

sealed class KotlinAndroidTargetVariantTypeDsl {
    abstract val kotlinModuleName: Property<String>
}

internal val KotlinAndroidTargetVariantTypeDsl.internal: InternalKotlinAndroidTargetVariantTypeDsl
    get() = when (this) {
        is InternalKotlinAndroidTargetVariantTypeDsl -> this
    }

internal class InternalKotlinAndroidTargetVariantTypeDsl(
    val project: Project,
) : KotlinAndroidTargetVariantTypeDsl() {

    override val kotlinModuleName: Property<String> by project.newLifecycleAwareProperty<String>(
        finaliseIn = Stage.BeforeFinaliseRefinesEdges
    )
}
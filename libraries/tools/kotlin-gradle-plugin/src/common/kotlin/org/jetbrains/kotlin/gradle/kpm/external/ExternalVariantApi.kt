/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.external

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinTopLevelExtension
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmProjectModelBuilder
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*

@RequiresOptIn("API is intended to build external Kotlin Targets.")
annotation class ExternalVariantApi

@ExternalVariantApi
val KotlinTopLevelExtension.project: Project
    get() = this.project

@ExternalVariantApi
val KotlinPm20ProjectExtension.ideaKpmProjectModelBuilder: IdeaKpmProjectModelBuilder
    get() = this.ideaKpmProjectModelBuilder

@ExternalVariantApi
fun GradleKpmModule.createExternalJvmVariant(
    name: String,
    config: GradleKpmJvmVariantConfig
): GradleKpmJvmVariant {
    val variant = GradleKpmJvmVariantFactory(this, config).create(name)
    fragments.add(variant)
    return variant
}

@ExternalVariantApi
val GradleKpmVariantInternal.compilationData
    get() = this.compilationData

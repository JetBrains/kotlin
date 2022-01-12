/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.external

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinTopLevelExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*

@RequiresOptIn("API is intended to build external Kotlin Targets.")
annotation class ExternalVariantApi

@ExternalVariantApi
val KotlinTopLevelExtension.project: Project
    get() = this.project

@ExternalVariantApi
fun KotlinGradleModule.createExternalJvmVariant(
    name: String,
    instantiator: KotlinJvmVariantInstantiator,
    configurator: KotlinJvmVariantConfigurator
): KotlinJvmVariant {
    val variant = KotlinJvmVariantFactory(instantiator, configurator).create(name)
    fragments.add(variant)
    return variant
}

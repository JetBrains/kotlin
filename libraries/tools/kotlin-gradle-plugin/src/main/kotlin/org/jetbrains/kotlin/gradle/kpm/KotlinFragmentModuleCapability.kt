/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm

import org.gradle.api.artifacts.Configuration
import org.gradle.api.capabilities.Capability
import org.jetbrains.kotlin.gradle.kpm.util.ComputedCapability
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleFragment
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleModule

val KotlinFragmentModuleCapability = FragmentCapabilities<KotlinGradleFragment> {
    capability(fragment.containingModule.moduleCapability ?: return@FragmentCapabilities)
}

internal fun setModuleCapability(configuration: Configuration, module: KotlinGradleModule) {
    configuration.outgoing.capability(module.moduleCapability ?: return)
}

internal val KotlinGradleModule.moduleCapability: Capability?
    get() = if (moduleClassifier != null) ComputedCapability.fromModule(this) else null

/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.artifacts.Configuration
import org.gradle.api.capabilities.Capability
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.ComputedCapability

val KotlinFragmentModuleCapability = FragmentCapabilities<KpmGradleFragment> {
    capability(fragment.containingModule.moduleCapability ?: return@FragmentCapabilities)
}

internal fun setModuleCapability(configuration: Configuration, module: KpmGradleModule) {
    configuration.outgoing.capability(module.moduleCapability ?: return)
}

internal val KpmGradleModule.moduleCapability: Capability?
    get() = if (moduleClassifier != null) ComputedCapability.fromModule(this) else null

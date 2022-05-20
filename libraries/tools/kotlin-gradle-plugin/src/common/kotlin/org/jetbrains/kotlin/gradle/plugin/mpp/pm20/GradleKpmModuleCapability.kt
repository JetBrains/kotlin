/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.artifacts.Configuration
import org.gradle.api.capabilities.Capability
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.ComputedCapability

val GradleKpmModuleCapability = GradleKpmConfigurationCapabilitiesSetup<GradleKpmFragment> {
    capability(fragment.containingModule.moduleCapability ?: return@GradleKpmConfigurationCapabilitiesSetup)
}

internal fun setModuleCapability(configuration: Configuration, module: GradleKpmModule) {
    configuration.outgoing.capability(module.moduleCapability ?: return)
}

internal val GradleKpmModule.moduleCapability: Capability?
    get() = if (moduleClassifier != null) ComputedCapability.fromModule(this) else null

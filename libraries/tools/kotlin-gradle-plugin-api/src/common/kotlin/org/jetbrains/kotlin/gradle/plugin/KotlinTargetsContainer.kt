/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.NamedDomainObjectContainer
import org.jetbrains.kotlin.gradle.PRESETS_API_IS_DEPRECATED_MESSAGE
import org.jetbrains.kotlin.gradle.DeprecatedTargetPresetApi
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi

@KotlinTargetsDsl
interface KotlinTargetsContainer {
    val targets: NamedDomainObjectCollection<KotlinTarget>
}

@OptIn(InternalKotlinGradlePluginApi::class)
@DeprecatedTargetPresetApi
interface KotlinTargetsContainerWithPresets : KotlinTargetsContainer {
    @Deprecated(
        PRESETS_API_IS_DEPRECATED_MESSAGE,
        level = DeprecationLevel.WARNING,
    )
    val presets: NamedDomainObjectCollection<KotlinTargetPreset<*>>
}

interface KotlinSourceSetContainer {
    val sourceSets: NamedDomainObjectContainer<KotlinSourceSet>
}

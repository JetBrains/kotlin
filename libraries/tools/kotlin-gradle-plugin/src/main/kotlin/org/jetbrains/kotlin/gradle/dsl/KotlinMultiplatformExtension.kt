/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.gradle.api.NamedDomainObjectCollection
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetPreset

open class KotlinMultiplatformExtension : KotlinProjectExtension() {
    lateinit var presets: NamedDomainObjectCollection<KotlinTargetPreset<*>>
        internal set

    lateinit var targets: NamedDomainObjectCollection<KotlinTarget>
        internal set

    internal var isGradleMetadataAvailable: Boolean = false
    internal var isGradleMetadataExperimental: Boolean = false
}
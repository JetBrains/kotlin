/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.ecosystem.util

import org.gradle.api.initialization.Settings
import org.jetbrains.kotlin.gradle.ecosystem.KotlinEcosystemExtension
import org.jetbrains.kotlin.gradle.ecosystem.KotlinEcosystemPlugin


/**
 * Applies [KotlinEcosystemPlugin] to the given [Settings] instance.
 */
fun Settings.applyKotlinEcosystemPlugin() {
    gradle.rootProject.objects.newInstance(KotlinEcosystemPlugin::class.java).apply(this)
}

/**
 * Returns the [KotlinEcosystemExtension].
 *
 * First use [applyKotlinEcosystemPlugin] before accessing this extension.
 */
val Settings.kotlin: KotlinEcosystemExtension
    get() = extensions.getByType(KotlinEcosystemExtension::class.java)

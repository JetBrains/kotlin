/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ecosystem

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.api.logging.Logging

class KotlinEcosystemPlugin : Plugin<Settings> {

    private val logger = Logging.getLogger("KotlinEcosystemPlugin")

    override fun apply(settings: Settings) {
        logger.warn("Kotlin Ecosystem plugin is experimental!")
    }
}
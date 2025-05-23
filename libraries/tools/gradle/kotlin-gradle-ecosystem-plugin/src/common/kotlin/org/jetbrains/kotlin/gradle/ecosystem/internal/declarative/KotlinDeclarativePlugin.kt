/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.ecosystem.internal.declarative

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.api.logging.Logging
import org.gradle.util.GradleVersion

internal abstract class KotlinDeclarativePlugin : Plugin<Settings> {

    private val logger = Logging.getLogger("KotlinDeclarativePlugin")

    override fun apply(settings: Settings) {
        logger.info(DCL_ENABLED_MESSAGE)
    }

    internal companion object {
        internal val SUPPORTED_GRADLE_VERSION = GradleVersion.version("8.14")
        internal const val DCL_ENABLED_MESSAGE = "Applying declarative Gradle support"
    }
}
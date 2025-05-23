/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.ecosystem

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.api.logging.Logging
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.ecosystem.internal.declarative.KotlinDclStatus
import org.jetbrains.kotlin.ecosystem.internal.declarative.KotlinDeclarativePlugin
import org.jetbrains.kotlin.ecosystem.internal.declarative.dclStatus

class KotlinEcosystemPlugin : Plugin<Settings> {

    private val logger = Logging.getLogger("KotlinEcosystemPlugin")

    override fun apply(settings: Settings) {
        logger.warn("Kotlin Ecosystem plugin is experimental!")

        settings.applyDeclarativeGradleIfEnabled()
    }

    private fun Settings.applyDeclarativeGradleIfEnabled() {
        val dclStatus = providers.dclStatus.get()
        if (dclStatus == KotlinDclStatus.ENABLED) {
            if (GradleVersion.current() == KotlinDeclarativePlugin.SUPPORTED_GRADLE_VERSION) {
                settings.plugins.apply(KotlinDeclarativePlugin::class.java)
            } else {
                logger.warn(
                    "Kotlin Declarative Gradle support is disabled due to unsupported Gradle version: ${GradleVersion.current()}. " +
                            "It only works with ${KotlinDeclarativePlugin.SUPPORTED_GRADLE_VERSION} Gradle release."
                )
            }
        } else {
            logger.info("Kotlin Declarative Gradle is disabled")
        }
    }
}
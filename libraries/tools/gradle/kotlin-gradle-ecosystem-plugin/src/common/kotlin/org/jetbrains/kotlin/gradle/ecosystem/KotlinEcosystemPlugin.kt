/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.ecosystem

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.api.logging.Logging
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ecosystem.internal.declarative.DCL_STATUS_GRADLE_PROPERTY_NAME
import org.jetbrains.kotlin.gradle.ecosystem.internal.declarative.KotlinDclStatus
import org.jetbrains.kotlin.gradle.ecosystem.internal.declarative.KotlinDeclarativePlugin
import org.jetbrains.kotlin.gradle.ecosystem.internal.declarative.dclStatus

class KotlinEcosystemPlugin : Plugin<Settings> {

    private val logger = Logging.getLogger("KotlinEcosystemPlugin")

    override fun apply(settings: Settings) {
        logger.warn(EXPERIMENTAL_WARNING_MESSAGE)

        settings.applyDeclarativeGradleIfEnabled()
    }

    private fun Settings.applyDeclarativeGradleIfEnabled() {
        val dclStatus = providers.dclStatus.get()
        if (dclStatus == KotlinDclStatus.ENABLED) {
            if (GradleVersion.current() in KotlinDeclarativePlugin.MIN_SUPPORTED_GRADLE_VERSION..KotlinDeclarativePlugin.MAX_SUPPORTED_GRADLE_VERSION) {
                settings.plugins.apply(KotlinDeclarativePlugin::class.java)
            } else {
                logger.warn(buildDclUnsupportedMessage(GradleVersion.current()))
            }
        } else {
            logger.info(DCL_DISABLED_MESSAGE)
        }
    }

    companion object {
        internal const val EXPERIMENTAL_WARNING_MESSAGE = "The Kotlin Ecosystem plugin ('org.jetbrains.kotlin.ecosystem') is experimental" +
                " and may change in future releases!"
        internal const val DCL_DISABLED_MESSAGE =
            "Kotlin Declarative Gradle is disabled by the property '$DCL_STATUS_GRADLE_PROPERTY_NAME'."

        internal fun buildDclUnsupportedMessage(
            currentGradleVersion: GradleVersion
        ) = "Support for Kotlin Declarative Gradle is only available with Gradle version " +
                "'${KotlinDeclarativePlugin.MIN_SUPPORTED_GRADLE_VERSION}.*'. The current Gradle version is $currentGradleVersion."
    }
}
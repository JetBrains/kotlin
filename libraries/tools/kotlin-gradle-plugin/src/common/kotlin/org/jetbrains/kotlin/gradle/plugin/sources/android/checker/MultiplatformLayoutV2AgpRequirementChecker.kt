/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources.android.checker

import com.android.Version
import org.jetbrains.kotlin.gradle.plugin.AndroidGradlePluginVersion
import org.jetbrains.kotlin.gradle.plugin.isAtLeast
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.sources.android.KotlinAndroidSourceSetLayout

internal object MultiplatformLayoutV2AgpRequirementChecker : KotlinAndroidSourceSetLayoutChecker {

    internal val minimumRequiredAgpVersion = AndroidGradlePluginVersion(7, 0, 0)

    override fun checkBeforeLayoutApplied(
        diagnosticReporter: KotlinAndroidSourceSetLayoutChecker.DiagnosticReporter,
        target: KotlinAndroidTarget,
        layout: KotlinAndroidSourceSetLayout
    ) {
        if (!isAgpRequirementMet()) {
            diagnosticReporter.error(
                AgpRequirementNotMetDiagnostic(minimumRequiredAgpVersion.toString(), Version.ANDROID_GRADLE_PLUGIN_VERSION)
            )
        }
    }

    internal fun isAgpRequirementMet(): Boolean {
        return AndroidGradlePluginVersion.currentOrNull.isAtLeast(minimumRequiredAgpVersion)
    }

    internal data class AgpRequirementNotMetDiagnostic(
        val minimumRequiredAgpVersion: String,
        val currentAgpVersion: String
    ) : KotlinAndroidSourceSetLayoutChecker.Diagnostic {
        override val message: String
            get() = """
                requires Android Gradle Plugin Version >= $minimumRequiredAgpVersion.
                Found $currentAgpVersion
            """.trimIndent()
    }
}

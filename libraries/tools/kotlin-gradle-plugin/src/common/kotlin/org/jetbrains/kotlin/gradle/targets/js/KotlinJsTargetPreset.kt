/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch")

// Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.PublicationRegistrationMode
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.hasKpmModel
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.mapTargetCompilationsToKpmVariants
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTargetConfigurator
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTargetPreset
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.gradle.utils.runProjectConfigurationHealthCheckWhenEvaluated
import org.jetbrains.kotlin.statistics.metrics.StringMetrics

open class KotlinJsTargetPreset(
    project: Project
) : KotlinOnlyTargetPreset<KotlinJsTarget, KotlinJsCompilation>(
    project
) {
    var mixedMode: Boolean? = null

    open val isMpp: Boolean
        get() = true

    override val platformType: KotlinPlatformType
        get() = KotlinPlatformType.js

    override fun instantiateTarget(name: String): KotlinJsTarget {
        return project.objects.newInstance(
            KotlinJsTarget::class.java,
            project,
            platformType,
            mixedMode
        ).apply {
            this.isMpp = this@KotlinJsTargetPreset.isMpp

            if (!mixedMode) {
                project.runProjectConfigurationHealthCheckWhenEvaluated {
                    if (!isBrowserConfigured && !isNodejsConfigured) {
                        project.logger.warn(
                            """
                            Please choose a JavaScript environment to build distributions and run tests.
                            Not choosing any of them will be an error in the future releases.
                            kotlin {
                                js {
                                    // To build distributions for and run tests on browser or Node.js use one or both of:
                                    browser()
                                    nodejs()
                                }
                            }
                        """.trimIndent()
                        )
                    }
                    val buildStatsService = KotlinBuildStatsService.getInstance()
                    when {
                        isBrowserConfigured && isNodejsConfigured -> buildStatsService?.report(StringMetrics.JS_TARGET_MODE, "both")
                        isBrowserConfigured -> buildStatsService?.report(StringMetrics.JS_TARGET_MODE, "browser")
                        isNodejsConfigured -> buildStatsService?.report(StringMetrics.JS_TARGET_MODE, "nodejs")
                        !isBrowserConfigured && !isNodejsConfigured -> buildStatsService?.report(StringMetrics.JS_TARGET_MODE, "none")
                    }
                    Unit
                }
            }
        }
    }

    override fun createKotlinTargetConfigurator() = KotlinJsTargetConfigurator()

    override fun createTarget(name: String): KotlinJsTarget {
        val result = super.createTarget(name)
        if (project.hasKpmModel) {
            mapTargetCompilationsToKpmVariants(result, PublicationRegistrationMode.IMMEDIATE)
        }
        return result
    }

    override fun getName(): String = JS_PRESET_NAME

    public override fun createCompilationFactory(forTarget: KotlinJsTarget): KotlinJsCompilationFactory {
        return KotlinJsCompilationFactory(project, forTarget)
    }

    companion object {
        val JS_PRESET_NAME = lowerCamelCaseName(
            "js",
            KotlinJsCompilerType.LEGACY.lowerName
        )
    }
}

class KotlinJsSingleTargetPreset(
    project: Project
) : KotlinJsTargetPreset(
    project
) {
    override val isMpp: Boolean
        get() = false

    // In a Kotlin/JS single-platform project, we don't need any disambiguation suffixes or prefixes in the names:
    override fun provideTargetDisambiguationClassifier(target: KotlinOnlyTarget<KotlinJsCompilation>): String? =
        if (mixedMode!!) {
            super.provideTargetDisambiguationClassifier(target)
                ?.removePrefix(target.name.removeJsCompilerSuffix(KotlinJsCompilerType.LEGACY))
                ?.decapitalize()
        } else {
            null
        }

    override fun createKotlinTargetConfigurator() = KotlinJsTargetConfigurator()
}

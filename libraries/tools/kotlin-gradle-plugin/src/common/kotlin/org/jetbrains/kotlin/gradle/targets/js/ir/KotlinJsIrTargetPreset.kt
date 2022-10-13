/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.PublicationRegistrationMode
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.hasKpmModel
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.mapTargetCompilationsToKpmVariants
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.gradle.utils.runProjectConfigurationHealthCheckWhenEvaluated
import org.jetbrains.kotlin.statistics.metrics.StringMetrics

open class KotlinJsIrTargetPreset(
    project: Project
) : KotlinOnlyTargetPreset<KotlinJsIrTarget, KotlinJsIrCompilation>(
    project
) {
    internal var legacyPreset: KotlinJsTargetPreset? = null
        internal set

    open val isMpp: Boolean
        get() = true

    override val platformType: KotlinPlatformType = KotlinPlatformType.js

    override fun useDisambiguationClassifierAsSourceSetNamePrefix() = legacyPreset == null

    override fun overrideDisambiguationClassifierOnIdeImport(name: String): String? =
        legacyPreset?.let {
            name.removeJsCompilerSuffix(KotlinJsCompilerType.IR)
        }

    override fun instantiateTarget(name: String): KotlinJsIrTarget {
        return project.objects.newInstance(KotlinJsIrTarget::class.java, project, platformType).apply {
            this.isMpp = this@KotlinJsIrTargetPreset.isMpp
            this.legacyTarget = legacyPreset?.createTarget(
                lowerCamelCaseName(
                    name.removeJsCompilerSuffix(KotlinJsCompilerType.IR),
                    KotlinJsCompilerType.LEGACY.lowerName
                )
            )
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

    override fun createKotlinTargetConfigurator(): AbstractKotlinTargetConfigurator<KotlinJsIrTarget> =
        KotlinJsIrTargetConfigurator()

    override fun createTarget(name: String): KotlinJsIrTarget {
        val result = super.createTarget(name)
        if (project.hasKpmModel) {
            mapTargetCompilationsToKpmVariants(result, PublicationRegistrationMode.IMMEDIATE)
        }
        return result
    }

    override fun getName(): String = lowerCamelCaseName(
        PRESET_NAME,
        legacyPreset?.let { KotlinJsCompilerType.BOTH.lowerName }
    )

    override fun createCompilationFactory(
        forTarget: KotlinJsIrTarget
    ): KotlinCompilationFactory<KotlinJsIrCompilation> {
        return KotlinJsIrCompilationFactory(forTarget)
    }

    companion object {
        const val PRESET_NAME = "js"
    }
}

class KotlinJsIrSingleTargetPreset(
    project: Project
) : KotlinJsIrTargetPreset(
    project
) {
    override val isMpp: Boolean
        get() = false

    override fun overrideDisambiguationClassifierOnIdeImport(name: String): String? =
        null

    // In a Kotlin/JS single-platform project, we don't need any disambiguation suffixes or prefixes in the names:
    override fun provideTargetDisambiguationClassifier(target: KotlinOnlyTarget<KotlinJsIrCompilation>): String? =
        legacyPreset?.let {
            super.provideTargetDisambiguationClassifier(target)
                ?.removePrefix(target.name.removeJsCompilerSuffix(KotlinJsCompilerType.IR))
                ?.decapitalize()
        }

    override fun createKotlinTargetConfigurator(): KotlinOnlyTargetConfigurator<KotlinJsIrCompilation, KotlinJsIrTarget> =
        KotlinJsIrTargetConfigurator()
}

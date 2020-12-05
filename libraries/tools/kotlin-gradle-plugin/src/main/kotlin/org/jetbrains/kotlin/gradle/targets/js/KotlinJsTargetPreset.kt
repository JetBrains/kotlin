/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch")

// Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTargetConfigurator
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTargetPreset
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.statistics.metrics.StringMetrics

open class KotlinJsTargetPreset(
    project: Project,
    kotlinPluginVersion: String
) : KotlinOnlyTargetPreset<KotlinJsTarget, KotlinJsCompilation>(
    project,
    kotlinPluginVersion
) {
    var irPreset: KotlinJsIrTargetPreset? = null
        internal set

    open val isMpp: Boolean
        get() = true

    override val platformType: KotlinPlatformType
        get() = KotlinPlatformType.js

    override fun useDisambiguationClassifierAsSourceSetNamePrefix() = irPreset == null

    override fun overrideDisambiguationClassifierOnIdeImport(name: String): String? =
        irPreset?.let {
            name.removeJsCompilerSuffix(KotlinJsCompilerType.LEGACY)
        }

    override fun instantiateTarget(name: String): KotlinJsTarget {
        return project.objects.newInstance(
            KotlinJsTarget::class.java,
            project,
            platformType
        ).apply {
            this.irTarget = irPreset?.createTarget(
                lowerCamelCaseName(
                    name.removeJsCompilerSuffix(KotlinJsCompilerType.LEGACY),
                    KotlinJsCompilerType.IR.lowerName
                )
            )?.also {
                it.legacyTarget = this
            }
            this.isMpp = this@KotlinJsTargetPreset.isMpp

            project.whenEvaluated {
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

    override fun createKotlinTargetConfigurator() = KotlinJsTargetConfigurator(
        kotlinPluginVersion
    )

    override fun getName(): String {
        return lowerCamelCaseName(
            PRESET_NAME,
            irPreset?.let { KotlinJsCompilerType.BOTH.lowerName }
        )
    }

    override fun createCompilationFactory(forTarget: KotlinJsTarget): KotlinJsCompilationFactory {
        return KotlinJsCompilationFactory(project, forTarget, irPreset?.let { (forTarget as KotlinJsTarget).irTarget })
    }

    companion object {
        const val PRESET_NAME = "js"
    }
}

class KotlinJsSingleTargetPreset(
    project: Project,
    kotlinPluginVersion: String
) : KotlinJsTargetPreset(
    project,
    kotlinPluginVersion
) {
    override val isMpp: Boolean
        get() = false

    override fun overrideDisambiguationClassifierOnIdeImport(name: String): String? =
        null

    // In a Kotlin/JS single-platform project, we don't need any disambiguation suffixes or prefixes in the names:
    override fun provideTargetDisambiguationClassifier(target: KotlinOnlyTarget<KotlinJsCompilation>): String? =
        irPreset?.let {
            super.provideTargetDisambiguationClassifier(target)
                ?.removePrefix(target.name.removeJsCompilerSuffix(KotlinJsCompilerType.LEGACY))
                ?.decapitalize()
        }

    override fun createKotlinTargetConfigurator() = KotlinJsTargetConfigurator(
        kotlinPluginVersion
    )
}

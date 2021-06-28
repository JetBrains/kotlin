/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinCompilationFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinOnlyTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinOnlyTargetPreset
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.statistics.metrics.StringMetrics

open class KotlinJsIrTargetPreset(
    project: Project
) : KotlinOnlyTargetPreset<KotlinJsIrTarget, KotlinJsIrCompilation>(
    project
) {
    internal var mixedMode: Boolean? = null

    open val isMpp: Boolean
        get() = true

    override val platformType: KotlinPlatformType
        get() = KotlinPlatformType.js

    override fun instantiateTarget(name: String): KotlinJsIrTarget {
        return project.objects.newInstance(KotlinJsIrTarget::class.java, project, platformType, mixedMode).apply {
            this.isMpp = this@KotlinJsIrTargetPreset.isMpp
            if (!mixedMode) {
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
    }

    override fun createKotlinTargetConfigurator(): KotlinOnlyTargetConfigurator<KotlinJsIrCompilation, KotlinJsIrTarget> =
        KotlinJsIrTargetConfigurator()

    override fun getName(): String = PRESET_NAME

    //TODO[Ilya Goncharov] remove public morozov
    public override fun createCompilationFactory(
        forTarget: KotlinJsIrTarget
    ): KotlinCompilationFactory<KotlinJsIrCompilation> =
        KotlinJsIrCompilationFactory(project, forTarget)

    companion object {
        val PRESET_NAME = lowerCamelCaseName(
            "js",
            KotlinJsCompilerType.IR.lowerName
        )
    }
}

class KotlinJsIrSingleTargetPreset(
    project: Project
) : KotlinJsIrTargetPreset(
    project
) {
    override val isMpp: Boolean
        get() = false

    // In a Kotlin/JS single-platform project, we don't need any disambiguation suffixes or prefixes in the names:
    override fun provideTargetDisambiguationClassifier(target: KotlinOnlyTarget<KotlinJsIrCompilation>): String? {
        return if (mixedMode!!) {
            super.provideTargetDisambiguationClassifier(target)
                ?.removePrefix(target.name.removeJsCompilerSuffix(KotlinJsCompilerType.IR))
                ?.decapitalize()
        } else {
            null
        }
    }

    override fun createKotlinTargetConfigurator(): KotlinOnlyTargetConfigurator<KotlinJsIrCompilation, KotlinJsIrTarget> =
        KotlinJsIrTargetConfigurator()
}

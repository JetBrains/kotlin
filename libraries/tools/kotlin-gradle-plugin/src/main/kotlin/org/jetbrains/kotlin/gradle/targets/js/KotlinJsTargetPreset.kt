/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch")

// Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.removeJsCompilerSuffix
import org.jetbrains.kotlin.gradle.plugin.whenEvaluated
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTargetConfigurator
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTargetPreset
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

open class KotlinJsTargetPreset(
    project: Project,
    kotlinPluginVersion: String
) : KotlinOnlyTargetPreset<KotlinJsTarget, KotlinJsCompilation>(
    project,
    kotlinPluginVersion
) {
    var irPreset: KotlinJsIrTargetPreset? = null
        internal set

    override val platformType: KotlinPlatformType
        get() = KotlinPlatformType.js

    override fun instantiateTarget(name: String): KotlinJsTarget {
        return project.objects.newInstance(
            KotlinJsTarget::class.java,
            project,
            platformType
        ).apply {
            this.irTarget = irPreset?.createTarget(
                lowerCamelCaseName(name.removeJsCompilerSuffix(KotlinJsCompilerType.legacy), KotlinJsCompilerType.ir.name)
            )

            project.whenEvaluated {
                if (!isBrowserConfigured && !isNodejsConfigured) {
                    project.logger.warn(
                        """
                            Choose sub target (or both), for which js is necessary
                            In next releases it will be error
                            Use
                            kotlin {
                                js {
                                    // Affect in which tests are executed and final dist (in browser is only one bundle file)
                                    browser()
                                    nodejs()
                                }
                            }
                        """.trimIndent()
                    )
                }
            }
        }
    }

    override fun createKotlinTargetConfigurator() = KotlinJsTargetConfigurator(
        kotlinPluginVersion
    )

    override fun getName(): String {
        return lowerCamelCaseName(
            PRESET_NAME,
            irPreset?.let { KotlinJsCompilerType.both.name }
        )
    }

    override fun createCompilationFactory(forTarget: KotlinOnlyTarget<KotlinJsCompilation>): KotlinJsCompilationFactory {
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
    // In a Kotlin/JS single-platform project, we don't need any disambiguation suffixes or prefixes in the names:
    override fun provideTargetDisambiguationClassifier(target: KotlinOnlyTarget<KotlinJsCompilation>): String? =
        irPreset?.let {
            super.provideTargetDisambiguationClassifier(target)
                ?.removePrefix(target.name.removeJsCompilerSuffix(KotlinJsCompilerType.legacy))
                ?.decapitalize()
        }

    override fun createKotlinTargetConfigurator() = KotlinJsTargetConfigurator(
        kotlinPluginVersion
    )
}
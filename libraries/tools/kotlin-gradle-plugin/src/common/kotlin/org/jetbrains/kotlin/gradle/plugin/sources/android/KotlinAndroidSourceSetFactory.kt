/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources.android

import com.android.build.gradle.api.AndroidSourceSet
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.sources.android.checker.KotlinAndroidSourceSetLayoutChecker
import org.jetbrains.kotlin.gradle.utils.getOrCreate
import org.jetbrains.kotlin.gradle.utils.runProjectConfigurationHealthCheck

internal class KotlinAndroidSourceSetFactory(
    private val target: KotlinAndroidTarget,
    private val kotlin: KotlinProjectExtension,
    private val layout: KotlinAndroidSourceSetLayout,
    private val diagnosticReporter: KotlinAndroidSourceSetLayoutChecker.DiagnosticReporter,
) {
    private val configuredKotlinSourceSets = mutableSetOf<KotlinSourceSet>()

    fun getOrCreateConfiguredKotlinSourceSet(kotlinSourceSetName: String, androidSourceSet: AndroidSourceSet): KotlinSourceSet {
        val kotlinSourceSet = getOrCreateKotlinSourceSet(kotlinSourceSetName, androidSourceSet)
        if (configuredKotlinSourceSets.add(kotlinSourceSet)) {
            layout.sourceSetConfigurator.configure(target, kotlinSourceSet, androidSourceSet)
            target.project.runProjectConfigurationHealthCheck {
                val layout = this@KotlinAndroidSourceSetFactory.layout
                layout.checker.checkCreatedSourceSet(diagnosticReporter, target, layout, kotlinSourceSet, androidSourceSet)
            }
        }
        return kotlinSourceSet
    }


    private fun getOrCreateKotlinSourceSet(
        name: String, androidSourceSet: AndroidSourceSet
    ): KotlinSourceSet {
        return kotlin.sourceSets.getOrCreate(name) { kotlinSourceSet ->
            kotlinSourceSet.androidSourceSetInfoOrNull?.let { info ->
                /* Check if kotlinSourceSetName is correct */
                check(info.kotlinSourceSetName == name) {
                    "Bad 'androidSourceSetInfo.kotlinSourceSetName' on KotlinSourceSet:$name: " +
                            "Expected: ${kotlinSourceSet.name} " +
                            "Found: ${info.kotlinSourceSetName}"
                }

                /* Check if androidSourceSetName is correct */
                check(info.androidSourceSetName == androidSourceSet.name) {
                    "Bad 'androidSourceSetInfo.androidSourceSetName' on KotlinSourceSet:$name: " +
                            "Expected: ${androidSourceSet.name} " +
                            "Found: ${info.androidSourceSetName}"
                }
                return@getOrCreate
            }

            kotlinSourceSet.androidSourceSetInfo = KotlinAndroidSourceSetInfo.Mutable(
                kotlinSourceSetName = name,
                androidSourceSetName = androidSourceSet.name,
                androidVariantType = AndroidBaseSourceSetName.byName(androidSourceSet.name)?.variantType ?: AndroidVariantType.Unknown
            )

            logger.debug("Created KotlinSourceSet: ${kotlinSourceSet.name} for AndroidSourceSet: ${androidSourceSet.name}")
        }
    }

    private companion object {
        val logger: Logger = Logging.getLogger(KotlinAndroidSourceSetFactory::class.java)
    }
}

/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources.android

import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.forEachVariant
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.utils.androidExtension
import org.jetbrains.kotlin.gradle.utils.runProjectConfigurationHealthCheck

internal object KotlinAndroidSourceSets {

    private val logger = Logging.getLogger(this::class.java)

    fun applyKotlinAndroidSourceSetLayout(
        target: KotlinAndroidTarget,
        layout: KotlinAndroidSourceSetLayout = target.project.kotlinAndroidSourceSetLayout
    ) {
        logger.debug("Applying ${KotlinAndroidSourceSetLayout::class.java.simpleName}: ${layout.name}")

        val android = target.project.androidExtension
        val factory = KotlinAndroidSourceSetFactory(target, target.project.kotlinExtension, layout)

        target.project.runProjectConfigurationHealthCheck {
            layout.checker.checkBeforeLayoutApplied(target, layout)
        }

        /*
        Hook eagerly into AndroidSourceSet creation
        Some 'base' source sets shall be created eagerly, as soon as available, too also
        make them available in the buildscript dsl immediately.
         */
        android.sourceSets.all { androidSourceSet ->
            val kotlinSourceSetName = layout.naming.kotlinSourceSetName(target.disambiguationClassifier, androidSourceSet.name)
            factory.getOrCreateConfiguredKotlinSourceSet(kotlinSourceSetName ?: return@all, androidSourceSet)
        }

        /* Hook into Android's variant creation: This is invoked in 'afterEvaluate' */
        forEachVariant(target.project) { variant ->
            variant.sourceSets.forEach { sourceProvider ->
                val androidSourceSet = android.sourceSets.findByName(sourceProvider.name) ?: return@forEach

                val kotlinSourceSetName = layout.naming.kotlinSourceSetName(target.disambiguationClassifier, sourceProvider.name)
                    ?: layout.naming.kotlinSourceSetName(target.disambiguationClassifier, sourceProvider.name, variant.type)

                val kotlinSourceSet = factory.getOrCreateConfiguredKotlinSourceSet(kotlinSourceSetName, androidSourceSet)
                layout.sourceSetConfigurator.configureWithVariant(target, kotlinSourceSet, variant)
            }
        }
    }
}



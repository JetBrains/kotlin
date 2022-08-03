/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources.android

import com.android.build.gradle.api.AndroidSourceSet
import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.forEachVariant
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.utils.androidExtension
import org.jetbrains.kotlin.gradle.utils.getOrCreate
import org.jetbrains.kotlin.gradle.utils.runProjectConfigurationHealthCheck

internal object KotlinAndroidSourceSets {

    private val logger = Logging.getLogger(this::class.java)

    fun applyKotlinAndroidSourceSetLayout(
        target: KotlinAndroidTarget,
        layout: KotlinAndroidSourceSetLayout = target.project.kotlinAndroidSourceSetLayout
    ) {
        logger.debug("Applying ${KotlinAndroidSourceSetLayout::class.java.simpleName}: ${layout.name}")

        val project = target.project
        val kotlin = project.kotlinExtension
        val android = project.androidExtension
        val naming = layout.naming
        val configurator = layout.sourceSetConfigurator
        val checker = layout.checker

        project.runProjectConfigurationHealthCheck {
            checker.checkBeforeLayoutApplied(target, layout)
        }

        /* Ensures that each KotlinSourceSet only invokes the 'configurator' once */
        val configuredKotlinSourceSets = mutableSetOf<KotlinSourceSet>()

        /*
        Hook eagerly into AndroidSourceSet creation
        Some 'base' source sets shall be created eagerly, as soon as available, too also
        make them available in the buildscript dsl immediately.
         */
        android.sourceSets.all { androidSourceSet ->
            val kotlinSourceSetName = naming.kotlinSourceSetName(target.disambiguationClassifier, androidSourceSet.name) ?: return@all
            val kotlinSourceSet = kotlin.getOrCreateKotlinSourceSet(kotlinSourceSetName, androidSourceSet)

            if (configuredKotlinSourceSets.add(kotlinSourceSet)) {
                configurator.configure(target, kotlinSourceSet, androidSourceSet)
                project.runProjectConfigurationHealthCheck {
                    checker.checkCreatedSourceSet(target, layout, kotlinSourceSet, androidSourceSet)
                }
            }
        }

        /* Hook into Android's variant creation: This is invoked in 'afterEvaluate' */
        forEachVariant(project) { variant ->
            variant.sourceSets.forEach { sourceProvider ->
                val androidSourceSet = android.sourceSets.findByName(sourceProvider.name) ?: return@forEach
                val kotlinSourceSetName = naming.kotlinSourceSetName(target.disambiguationClassifier, sourceProvider.name)
                    ?: naming.kotlinSourceSetName(target.disambiguationClassifier, sourceProvider.name, variant.type)
                val kotlinSourceSet = kotlin.getOrCreateKotlinSourceSet(kotlinSourceSetName, androidSourceSet)

                if (configuredKotlinSourceSets.add(kotlinSourceSet)) {
                    configurator.configure(target, kotlinSourceSet, androidSourceSet)
                    project.runProjectConfigurationHealthCheck {
                        checker.checkCreatedSourceSet(target, layout, kotlinSourceSet, androidSourceSet)
                    }
                }
                configurator.configureWithVariant(target, kotlinSourceSet, variant)
            }
        }
    }


    private fun KotlinProjectExtension.getOrCreateKotlinSourceSet(
        name: String, androidSourceSet: AndroidSourceSet
    ): KotlinSourceSet {
        return sourceSets.getOrCreate(name) { kotlinSourceSet ->
            kotlinSourceSet.androidSourceSetInfoOrNull?.let { info ->
                check(info.kotlinSourceSetName == name) { "Bad 'androidSourceSetInfo' on $name: Bad 'kotlinSourceSetName'" }
                check(info.androidSourceSetName == androidSourceSet.name) { "Bad 'androidSourceSetInfo' on $name: Bad 'androidSourceSetName'" }
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
}
